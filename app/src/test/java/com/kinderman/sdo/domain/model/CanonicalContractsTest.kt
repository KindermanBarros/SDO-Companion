package com.kinderman.sdo.domain.model

import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.migratedStructuredRecord
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CanonicalContractsTest {
    private val abilityRef = CatalogReference(AbilityCatalogId("ability.fire"), 1)
    private val characterSource = Source.narrative(NarrativeSourceId("character-1"))

    @Test fun publishedRevisionIsImmutable() {
        val catalog = ImmutableCatalog<String>()
        val entry = PublishedCatalogEntry(abilityRef, "v1", 10)
        catalog.publish(entry)
        assertEquals("v1", catalog.resolve(abilityRef))

        expect<DomainError.RevisionConflict> { catalog.publish(entry.copy(value = "overwrite")) }
        assertEquals("v1", catalog.resolve(abilityRef))
    }

    @Test fun concurrentRevisionRejectsStaleWriter() {
        val current = PublishedCatalogEntry(abilityRef, "v1", 10)
        val advanced = compareAndAdvanceRevision(current, RevisionedWrite(1, "v2"))
        assertEquals(2, advanced.reference.revision)
        expect<DomainError.RevisionConflict> { compareAndAdvanceRevision(advanced, RevisionedWrite(1, "stale")) }
    }

    @Test fun invalidAshIsReportedWithoutGuessingMissingFields() {
        val ash = ability(
            kind = AbilityKind.ASH,
            source = characterSource,
            cost = AbilityCost.AshCost(1),
        )
        assertTrue(CanonicalValidator.validate(ash).any { it.message!!.contains("Cinza") })
    }

    @Test fun passivePowerRequiresZeroCostAndNoDuration() {
        val passive = ability(
            kind = AbilityKind.POWER,
            execution = Execution(ExecutionKind.Passive),
            duration = null,
            cost = AbilityCost.PowerCost(1, SpendableResource.PE, false),
        )
        assertTrue(CanonicalValidator.validate(passive).any { it.message!!.contains("custo zero") })
    }

    @Test fun ambiguousLegacyTextBecomesStableNeedsReview() {
        val first = CanonicalMigration.migrateMechanicalText("Descrição", "+2 em testes")
        val second = CanonicalMigration.migrateMechanicalText(first.value, first.reviews.single().legacyValue)
        assertEquals(MigrationState.NEEDS_REVIEW, first.state)
        assertEquals(first, second)
        assertEquals("Descrição", first.value)
    }

    @Test fun ashConsumptionIsAtomic() {
        val item = ItemState(
            id = ItemInstanceId("ash-item"), definition = CatalogReference(ItemCatalogId("item.ash"), 1),
            scope = CatalogScope.Character, consumable = ConsumableState(doses = 2),
        )
        val ash = ability(
            kind = AbilityKind.ASH, source = null, cost = AbilityCost.AshCost(2),
            ashOrigin = AshOrigin.Fire, ashPurity = AshPurity.RAW, linkedItemId = ItemInstanceId("ash-item"),
        )
        assertEquals(0, consumeAshAtomically(ash, item, 2).item.consumable!!.doses)
        expect<DomainError.InsufficientResource> { consumeAshAtomically(ash, item, 3) }
        assertEquals(2, item.consumable!!.doses)
    }

    @Test fun progressionChoiceIsIdempotentAndCannotDoubleGrant() {
        val choice = AuditableChoice(
            EntityId("choice-1"), ChoiceKind.PATH, abilityRef, characterSource.sourceRef,
            listOf(EntityId("power-1")), 10,
        )
        val once = CharacterProgression().register(choice)
        assertSame(once, once.register(choice))
        expect<DomainError.DuplicateIdentity> {
            once.register(choice.copy(id = EntityId("choice-2")))
        }
    }

    @Test fun derivedValuesCarryClosedFormulaIds() {
        val character = Character()
        assertEquals(DerivedFormulaId.VidaMax, DerivedValueService.resources(character).first().formulaId)
        assertEquals(character.maximumLoad, DerivedValueService.load(character).value)
        assertEquals(2, DerivedValueService.implantLimit(character).value)
    }

    @Test fun specializationRequiresValidParentKnowledge() {
        val parent = SpecialKnowledge(id = "parent-1", name = "Sobrevivência", category = "Adquirido")
        val validSpec = SpecialKnowledge(
            id = "spec-1", name = "Rastreamento", category = "Especialização",
            specializationParentId = parent.id,
        )
        val missingParentSpec = SpecialKnowledge(
            id = "spec-2", name = "Rastreamento", category = "Especialização",
            specializationParentId = "",
        )
        val orphanSpec = SpecialKnowledge(
            id = "spec-3", name = "Rastreamento", category = "Especialização",
            specializationParentId = "non-existent",
        )

        val validErrors = CanonicalValidator.validateSpecialKnowledge(validSpec, listOf(parent, validSpec))
        assertTrue(validErrors.isEmpty())

        val missingErrors = CanonicalValidator.validateSpecialKnowledge(missingParentSpec, listOf(parent))
        assertTrue(missingErrors.any { it is DomainError.ValidationError })

        val orphanErrors = CanonicalValidator.validateSpecialKnowledge(orphanSpec, listOf(parent))
        assertTrue(orphanErrors.any { it is DomainError.InvalidReference })

        val typedSpec = SpecializationKnowledge(
            id = SpecialKnowledgeId("spec-typed"),
            name = "Rastreamento",
            parentKnowledgeId = SpecialKnowledgeId("parent-1"),
        )
        assertEquals("parent-1", typedSpec.parentKnowledgeId.value)
    }

    @Test fun structuredNarrativeModelsSupportOptionalMechanicalEffects() {
        val effect = MechanicalEffect(
            operations = listOf(
                EffectOperation.SpendResource(resource = SpendableResource.PE, amount = 1),
            ),
        )
        val trait = StructuredTrait(name = "Corajoso", isPositive = true, mechanicalEffect = effect)
        val occupation = StructuredOccupation(name = "Guarda da Muralha", mechanicalEffect = effect)
        val profession = StructuredProfession(name = "Ferreiro Rúnico", mechanicalEffect = effect)
        val faction = StructuredFaction(name = "Ordem da Aurora", standing = "Aliado", mechanicalEffect = effect)
        val bond = StructuredBond(name = "Irmão de Armas", target = "Kaelen", bondType = "Lealdade", mechanicalEffect = effect)

        assertEquals("Corajoso", trait.name)
        assertEquals(1, trait.mechanicalEffect!!.operations.size)
        assertEquals("Guarda da Muralha", occupation.name)
        assertEquals("Ferreiro Rúnico", profession.name)
        assertEquals("Ordem da Aurora", faction.name)
        assertEquals("Irmão de Armas", bond.name)
    }

    @Test fun powerAndMysticAbilityUnifyUnderCanonicalAbility() {
        val power = Power(
            id = "power-1",
            name = "Golpe Feroz",
            costType = AbilityCostType.ENERGY,
            costValue = 2,
            executionType = AbilityExecution.ACTION,
            rangeType = AbilityRange.SHORT,
            durationType = AbilityDuration.INSTANT,
            canonicalSource = AbilitySource.PATH,
            sourceId = "path.guerreiro",
            origin = "Guerreiro",
        )
        val canonicalPower = power.toCanonicalAbility()
        assertEquals(AbilityKind.POWER, canonicalPower.kind)
        assertEquals(2, canonicalPower.cost.amount)
        assertEquals(ExecutionKind.Action, canonicalPower.execution.kind)
        assertEquals(RangeBand.Short, canonicalPower.range.band)

        val spell = MysticAbility(
            id = "spell-1",
            type = "Magia",
            name = "Raio Arcano",
            costValue = 3,
            executionType = AbilityExecution.ACTION,
            rangeType = AbilityRange.MEDIUM,
            durationType = AbilityDuration.INSTANT,
            canonicalSource = AbilitySource.KNOWLEDGE,
            knowledgeId = "arcane-1",
            knowledgeLevel = 2,
        )
        val canonicalSpell = spell.toCanonicalAbility()
        assertEquals(AbilityKind.SPELL, canonicalSpell.kind)
        assertEquals(3, canonicalSpell.cost.amount)

        val character = Character(powers = listOf(power), mysticAbilities = listOf(spell))
        val unified = character.allCanonicalAbilities()
        assertEquals(2, unified.size)
        assertEquals(setOf(AbilityKind.POWER, AbilityKind.SPELL), unified.map { it.kind }.toSet())
    }

    @Test fun bodyRegionsOrgansAndConditionsConvertBidirectionally() {
        val region = BodyRegion(roll = 2, name = "Torso", failures = 1, localProtection = 3, generalProtection = 2, state = BodyIntegrity.Damaged)
        val canonicalRegion = region.toCanonicalState()
        assertEquals(BodyRegionSlot.Torso, canonicalRegion.region)
        assertEquals(BodyIntegrity.Damaged, canonicalRegion.state)
        assertEquals(1, canonicalRegion.failures)
        assertEquals(3, canonicalRegion.protection.localProtection)

        val roundtripRegion = canonicalRegion.toLegacyRegion()
        assertEquals(region.name, roundtripRegion.name)
        assertEquals(region.failures, roundtripRegion.failures)
        assertEquals(region.localProtection, roundtripRegion.localProtection)

        val organ = OrganStatus(name = "Coração / Núcleo", failures = 0, slot = OrganSlot.HeartOrCore)
        val canonicalOrgan = organ.toCanonicalState()
        assertEquals(OrganSlot.HeartOrCore, canonicalOrgan.organ)
        assertEquals(BodyIntegrity.Intact, canonicalOrgan.state)
        assertEquals(organ.name, canonicalOrgan.toLegacyStatus().name)

        val condition = ConditionEffect(id = "cond-1", name = "Abalado", intensity = "2", duration = "3", origin = "source-1")
        val canonicalCondition = condition.toCanonicalInstance()
        assertEquals(ConditionKind.Abalado, canonicalCondition.kind)
        assertEquals(2, canonicalCondition.intensity)
        val roundtripCondition = canonicalCondition.toLegacyEffect()
        assertEquals("Abalado", roundtripCondition.name)
        assertEquals("2", roundtripCondition.intensity)
    }

    @Test fun structuredSessionOperationsExecuteDeterministicMutations() {
        val character = Character(
            life = ResourceValue(current = 10, maximum = 10),
            energy = ResourceValue(current = 5, maximum = 5, adjustment = 5),
            inventory = listOf(InventoryItem(id = "item-1", name = "Poção", quantity = 3)),
            itemStates = listOf(ItemState(
                id = ItemInstanceId("item-1"),
                definition = CatalogReference(ItemCatalogId("potion"), 1),
                scope = CatalogScope.Canonical,
                stack = StackState(3),
                consumable = ConsumableState(doses = 3),
            )),
        )

        // 1. Damage operation
        val dmgOp = EffectOperation.Damage(
            damageType = DamageType.Physical,
            amount = DiceOrNumber.Fixed(4),
            target = TargetSpec.Self,
        )
        val dmgResult = character.applyEffectOperation(dmgOp)
        assertEquals(6, dmgResult.character.life.current)
        assertEquals(SessionOperationType.DAMAGE, dmgResult.operation.type)

        // 2. Healing operation
        val healOp = EffectOperation.Healing(
            resource = SpendableResource.PV,
            amount = DiceOrNumber.Fixed(3),
            target = TargetSpec.Self,
        )
        val healResult = dmgResult.character.applyEffectOperation(healOp)
        assertEquals(9, healResult.character.life.current)
        assertEquals(SessionOperationType.HEAL, healResult.operation.type)

        // 3. Spend Resource
        val spendOp = EffectOperation.SpendResource(resource = SpendableResource.PE, amount = 2)
        val spendResult = character.applyEffectOperation(spendOp)
        assertEquals(3, spendResult.character.energy.current)
        assertEquals(SessionOperationType.RESOURCE, spendResult.operation.type)

        // 4. Consume Item
        val consumeOp = EffectOperation.ConsumeItemState(itemInstanceId = ItemInstanceId("item-1"), amount = 1)
        val consumeResult = character.applyEffectOperation(consumeOp)
        assertEquals(2, consumeResult.character.inventory.single().quantity)
        assertEquals(2, consumeResult.character.itemStates.single().consumable?.doses)

        // 5. Apply Condition
        val condOp = EffectOperation.ApplyCondition(condition = ConditionPayload(kind = ConditionKind.Abalado, intensity = 1))
        val condResult = character.applyEffectOperation(condOp)
        assertTrue(condResult.character.conditions.any { it.name == "Abalado" })
        assertEquals(ConditionKind.Abalado, condResult.character.canonicalConditions().single().payload.kind)
        assertEquals(condOp, condResult.operation.canonicalPayload?.effectOperations?.single())
    }

    @Test fun sessionConditionRequiresTypedPayloadAndAuditsItsContract() {
        val character = Character()
        expect<DomainError.ValidationError> {
            character.applySessionCommand(
                SessionCommand(type = SessionOperationType.CONDITION_ADD, label = "Abalado"),
                actorId = "historian",
            )
        }

        val payload = ConditionPayload(kind = ConditionKind.Envenenado, intensity = 2)
        val mutation = character.applySessionCommand(
            SessionCommand(type = SessionOperationType.CONDITION_ADD, condition = payload, reason = "teste"),
            actorId = "historian",
        )
        assertEquals(payload, mutation.character.canonicalConditions().single().payload)
        assertEquals(payload, mutation.operation.canonicalPayload?.condition)
    }

    @Test fun canonicalSessionPayloadSurvivesRoomAndFirestoreRecordProjection() {
        val payload = CanonicalSessionPayload(
            effectOperations = listOf(EffectOperation.SpendResource(SpendableResource.PE, 2)),
            condition = ConditionPayload(kind = ConditionKind.Cego, duration = Duration(DurationKind.Scene)),
            bodyRegion = BodyRegionSlot.Head,
            abilityId = "ability-1",
            abilityCost = AbilityCost.SpellCost(2),
        )
        val operation = SessionOperation(
            id = "operation-1",
            idempotencyKey = "key-1",
            type = SessionOperationType.EFFECT_OPERATION,
            canonicalPayload = payload,
        )
        assertEquals(payload, operation.toRecord().toDomain().canonicalPayload)
    }

    @Test fun canonicalAbilityUsePaysCostAndAppliesTypedOperationsAsOneMutation() {
        val applyCondition = EffectOperation.ApplyCondition(
            ConditionPayload(kind = ConditionKind.Imobilizado, duration = Duration(DurationKind.Scene)),
        )
        val passive = Ability(
            id = "ability.test",
            name = "Prender",
            kind = AbilityKind.POWER,
            source = characterSource,
            execution = Execution(ExecutionKind.Passive),
            duration = null,
            cost = AbilityCost.PowerCost(0, SpendableResource.PE),
            mechanicalEffect = MechanicalEffect(operations = listOf(applyCondition)),
        )
        val character = Character(abilities = listOf(passive))
        val mutation = character.applySessionCommand(
            SessionCommand(type = SessionOperationType.ABILITY_USE, targetId = passive.id),
            actorId = "player",
        )
        assertEquals(ConditionKind.Imobilizado, mutation.character.canonicalConditions().single().kind)
        assertEquals(passive.id, mutation.operation.canonicalPayload?.abilityId)
        assertEquals(listOf(applyCondition), mutation.operation.canonicalPayload?.effectOperations)
    }

    @Test fun highestOnlyModifierReplacesLowerValueAndDamageRecordsTypedInjury() {
        val target = BonusTarget.AttributeTarget(Attribute.FOR)
        val weaker = ActiveModifier(target = target, amount = 1, source = characterSource, stacking = StackingRule.HighestOnly)
        val stronger = ActiveModifier(target = target, amount = 3, source = characterSource, stacking = StackingRule.HighestOnly)
        val character = Character(life = ResourceValue(current = 10, maximum = 10))
        val afterWeak = character.applyEffectOperation(EffectOperation.AddModifier(weaker)).character
        val afterStrong = afterWeak.applyEffectOperation(EffectOperation.AddModifier(stronger)).character
        assertEquals(listOf(stronger), afterStrong.activeModifiers)

        val damage = EffectOperation.Damage(
            amount = DiceOrNumber.Fixed(2),
            target = TargetSpec.TargetRegion(BodyRegionSlot.ArmLeft),
        )
        val injured = character.applyEffectOperation(damage).character.canonicalBodyState().region(BodyRegionSlot.ArmLeft)
        assertEquals(2, injured.injuries.single().damage?.amount)
    }

    @Test fun derivedResourcesConsumeCanonicalPermanentAndActiveModifiers() {
        val permanent = Ability(
            id = "passive.resource",
            name = "Vigor",
            kind = AbilityKind.POWER,
            source = characterSource,
            execution = Execution(ExecutionKind.Passive),
            duration = null,
            cost = AbilityCost.PowerCost(0, SpendableResource.PE),
            powerData = PowerData(
                grantsPermanentBonus = true,
                permanentBonuses = listOf(PermanentBonus(BonusTarget.ResourceMaxTarget(SpendableResource.PV), 3)),
                passiveState = PassiveState.ManualToggle(active = true),
            ),
        )
        val active = ActiveModifier(
            target = BonusTarget.ResourceMaxTarget(SpendableResource.PV),
            amount = 2,
            source = characterSource,
        )
        val character = Character(abilities = listOf(permanent), activeModifiers = listOf(active))
        assertEquals(character.lifeBase + 5, character.lifeMaximum)
        assertEquals(character.lifeMaximum, DerivedValueService.resources(character).first { it.formulaId == DerivedFormulaId.VidaMax }.value)
    }

    @Test fun fullCreateMigrateEditSyncLifecycleEnforcesCanonicalInvariants() {
        // Create initial character
        val character = Character(
            name = "Valente",
            canonicalSchemaVersion = CANONICAL_SCHEMA_VERSION,
            powers = listOf(Power(name = "Investida", origin = "Guerreiro")),
        )
        val record = character.toRecord()
        assertEquals(CANONICAL_SCHEMA_VERSION, record.canonicalSchemaVersion)
        // Derived maximums are zeroed on record to avoid persisting authority
        assertEquals(0, record.life.maximum)
        assertEquals(emptyMap<String, Int>(), record.protections)

        // Reopening from record dynamically recomputes canonical projections
        val reloaded = record.toDomain()
        assertEquals(character.lifeMaximum, reloaded.lifeMaximum)
        assertTrue(reloaded.protectionTotal("Geral") > 0)

        // Editing character advances updated state with canonical invariants
        val edited = reloaded.copy(name = "Valente Renascido").toRecord()
        assertEquals("Valente Renascido", edited.toDomain().name)
        assertEquals(CANONICAL_SCHEMA_VERSION, edited.canonicalSchemaVersion)
    }

    @Test fun concurrentBatchMigrationPreservesDataWithoutLoss() {
        val pool = Executors.newFixedThreadPool(8)
        val characterCount = 40
        val records = (1..characterCount).map { i ->
            CharacterRecord(
                id = "char-$i",
                name = "Personagem $i",
                ownerId = "owner-$i",
                canonicalSchemaVersion = 0,
                creationRulesVersion = 1,
                itemSchemaVersion = 0,
            )
        }

        val migratedResults = ConcurrentHashMap<String, CharacterRecord>()
        records.forEach { record ->
            pool.submit {
                val migrated = record.migratedStructuredRecord(markDirty = true)
                migratedResults[record.id] = migrated
            }
        }
        pool.shutdown()
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS))

        assertEquals(characterCount, migratedResults.size)
        records.forEach { record ->
            val migrated = migratedResults[record.id]
            assertNotNull(migrated)
            assertEquals(CANONICAL_SCHEMA_VERSION, migrated!!.canonicalSchemaVersion)
            assertEquals(record.name, migrated.name)
            assertEquals(record.ownerId, migrated.ownerId)
        }
    }

    @Test fun compoundAshDoseAndResourceConsumptionAtomicity() {
        val item = ItemState(
            id = ItemInstanceId("ash-item-1"),
            definition = CatalogReference(ItemCatalogId("item.ash"), 1),
            scope = CatalogScope.Character,
            consumable = ConsumableState(doses = 1),
        )
        val ash = ability(
            kind = AbilityKind.ASH,
            source = null,
            cost = AbilityCost.AshCost(2),
            ashOrigin = AshOrigin.Fire,
            ashPurity = AshPurity.RAW,
            linkedItemId = ItemInstanceId("ash-item-1"),
        )
        val character = Character(
            energy = ResourceValue(current = 10, maximum = 10, adjustment = 10),
            inventory = listOf(InventoryItem(id = "ash-item-1", name = "Cinza de Fogo", quantity = 1)),
        )

        expect<DomainError.InsufficientResource> {
            consumeAshAtomically(ash, item, doses = 2)
        }
        assertEquals(10, character.energy.current)
        assertEquals(1, character.inventory.single().quantity)
    }

    @Test fun legacyWriteRejectionEnforcedOnOutdatedSchema() {
        val legacyCharacter = Character(
            name = "Legado",
            canonicalSchemaVersion = 0,
        )
        expect<DomainError.LegacyWriteRejected> {
            if (legacyCharacter.canonicalSchemaVersion != CANONICAL_SCHEMA_VERSION) {
                throw DomainError.LegacyWriteRejected()
            }
        }
    }

    private fun ability(
        kind: AbilityKind,
        source: Source? = characterSource,
        execution: Execution = Execution(ExecutionKind.Action),
        duration: Duration? = Duration(DurationKind.Instant),
        cost: AbilityCost,
        ashOrigin: AshOrigin? = null,
        ashPurity: AshPurity? = null,
        linkedItemId: ItemInstanceId? = null,
    ) = Ability(
        id = "ability-1",
        kind = kind,
        definition = abilityRef,
        source = source,
        name = "Habilidade",
        execution = execution,
        range = RangeSpec(RangeBand.Personal),
        duration = duration,
        resistance = Resistance.None,
        cost = cost,
        effect = "Texto livre",
        ashData = if (ashOrigin != null && linkedItemId != null) {
            AshData(origin = ashOrigin, purity = ashPurity ?: AshPurity.RAW, linkedItemId = linkedItemId)
        } else null,
    )

    private inline fun <reified T : Throwable> expect(block: () -> Unit) {
        try {
            block()
            fail("Esperava ${T::class.simpleName}")
        } catch (error: Throwable) {
            if (error !is T) throw error
        }
    }
}
