package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CanonicalContractsTest {
    private val abilityRef = CatalogReference(AbilityCatalogId("ability.fire"), 1)
    private val characterSource = Source.narrative(NarrativeSourceId("character-1"))

    @Test fun canonicalRulesCommitIsRecorded() {
        assertEquals("86102242603058895e49129ee0f88d66a9a317bb", CANONICAL_RULES_COMMIT)
    }

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
            id = EntityId("ash-item"), definition = CatalogReference(CatalogEntryId("item.ash"), 1),
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
