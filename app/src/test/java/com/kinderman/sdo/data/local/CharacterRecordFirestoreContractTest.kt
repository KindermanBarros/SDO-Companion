package com.kinderman.sdo.data.local

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.kinderman.sdo.domain.model.defaultAttributes
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.ItemCreationDraft
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.KnowledgeMilestoneReward
import com.kinderman.sdo.domain.model.KnowledgeMilestoneRewardType
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.*
import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterRecordFirestoreContractTest {
    @Test fun canonicalPayloadsSurviveRoomAndFirestoreRecordRoundTrip() {
        val canonical = Character(
            abilities = listOf(Ability(
                id = "typed-power", name = "Pulso", kind = AbilityKind.POWER,
                source = Source.narrative(NarrativeSourceId("choice-1")),
                cost = AbilityCost.PowerCost(1, SpendableResource.PE),
                mechanicalEffect = MechanicalEffect(listOf(EffectOperation.Damage(amount = DiceOrNumber.Roll(Dice(2, 6))))),
            )),
            bodyState = BodyState().withInjury(
                BodyRegionSlot.Torso,
                InjuryEvent(failuresAdded = 1, source = Source.narrative(NarrativeSourceId("combat-1"))),
            ),
            conditionInstances = listOf(ConditionInstance(
                instanceId = ConditionInstanceId("condition-1"),
                payload = ConditionPayload(kind = ConditionKind.Abalado, intensity = 2),
                source = Source.narrative(NarrativeSourceId("power-1")),
            )),
            activeModifiers = listOf(ActiveModifier(
                modifierId = "modifier-1", target = BonusTarget.AttributeTarget(Attribute.AGI), amount = 1,
                source = Source.narrative(NarrativeSourceId("condition-1")), duration = Duration(DurationKind.Scene),
            )),
        )

        val record = canonical.toRecord()
        val decoded = record.toDomain()

        assertEquals(canonical.abilities, decoded.abilities)
        assertEquals(canonical.bodyState, decoded.bodyState)
        assertEquals(canonical.conditionInstances, decoded.conditionInstances)
        assertEquals(canonical.activeModifiers, decoded.activeModifiers)
        assertEquals(record.canonicalAbilitiesPayload, decoded.toRecord().canonicalAbilitiesPayload)
    }

    @Test fun legacyBodyEditorChangesPreserveCanonicalInjuryHistory() {
        val injured = Character().let { character ->
            character.copy(bodyState = BodyState().withInjury(
                BodyRegionSlot.Torso,
                InjuryEvent(failuresAdded = 1, source = Source.narrative(NarrativeSourceId("combat-1"))),
            ))
        }
        val legacyEdited = injured.copy(
            bodyRegions = injured.bodyRegions.map { region ->
                if (region.name == BodyRegionSlot.Torso.label) region.copy(failures = 2, state = BodyIntegrity.Damaged) else region
            },
        )

        val persisted = legacyEdited.toRecord().toDomain()

        assertEquals(2, persisted.bodyState?.region(BodyRegionSlot.Torso)?.failures)
        assertEquals(1, persisted.bodyState?.region(BodyRegionSlot.Torso)?.injuries?.size)
    }

    @Test fun itemCreationDraftSurvivesRoomAndRecordMapping() {
        val draft = ItemCreationDraft(
            step = 3, category = "Armadura", baseId = "elmo", materialId = "ligas_comuns",
            quality = ItemQuality.IMPROVED, modificationIds = listOf("robusta"), gemIds = listOf("gema_atributo_for"),
            gemSlots = 1, technologySlots = 2, customName = "Elmo da Aurora",
            manualPrice = "120", commonName = "Kit", commonEffect = "Ferramentas",
            commonLoad = 2, commonQuantity = 3,
        )
        val converters = CharacterConverters()

        assertEquals(draft, converters.stringToItemCreationDraft(converters.itemCreationDraftToString(draft)))
        assertEquals(draft, CharacterRecord(itemCreationDraft = draft, creationRulesVersion = 2)
            .migratedStructuredRecord(markDirty = false).toDomain().toRecord().itemCreationDraft)
    }

    @Test fun knowledgeMilestoneRewardsSurviveRoomConversion() {
        val reward = KnowledgeMilestoneReward(3, KnowledgeMilestoneRewardType.POWER, "power.rhythm", "instance-1")
        val knowledge = SpecialKnowledge(name = "Música", milestoneLevels = listOf(3), milestoneRewards = listOf(reward))
        val converters = CharacterConverters()

        assertEquals(knowledge, converters.stringToKnowledges(converters.knowledgesToString(listOf(knowledge))).single())
    }

    @Test fun legacyEmbeddedRacialBonusMovesToDerivedModifierOnce() {
        val attributes = defaultAttributes().map { if (it.acronym == "CAR") it.copy(value = 3) else it }
        val migrated = CharacterRecord(race = "Humanos", raceAttribute = "CAR", attributes = attributes, creationRulesVersion = 1)
            .migratedStructuredRecord(markDirty = false).toDomain()

        assertEquals(2, migrated.attributes.first { it.acronym == "CAR" }.value)
        assertEquals(3, migrated.attributeTotal("CAR"))
        assertEquals(2, migrated.creationRulesVersion)
        val reloaded = migrated.toRecord().toDomain()
        assertEquals(2, reloaded.attributes.first { it.acronym == "CAR" }.value)
        assertEquals(3, reloaded.attributeTotal("CAR"))
    }

    @Test fun normalizedItemFieldsSurviveRoomConversion() {
        val item = InventoryItem(
            id = "weapon-1",
            baseId = "faca",
            materialId = "madeira",
            modificationIds = listOf("afiada"),
            gemIds = listOf("gema_menor_aleatoria"),
            mechanicalEffects = listOf(ItemEffect("gema_menor_aleatoria", ItemEffectType.KNOWLEDGE, 1, "*")),
            dataVersion = CURRENT_ITEM_DATA_VERSION,
        )
        val converters = CharacterConverters()
        assertEquals(item, converters.stringToInventory(converters.inventoryToString(listOf(item))).single())
    }

    @Test fun legacyInventoryIsPersistedWithTheCurrentTypedContract() {
        val legacy = InventoryItem(
            id = "legacy-item",
            state = "EQUIPPED",
            name = "Objeto personalizado",
            category = "Acessório",
        )

        val record = CharacterRecord(inventory = listOf(legacy)).migratedStructuredRecord(markDirty = false)
        val migrated = record.toDomain().toRecord()

        assertEquals("Objeto personalizado", migrated.inventory.single().name)
        assertEquals(1, migrated.toDomain().itemStates.size)
        assertEquals(1, migrated.toDomain().customItemCatalog.size)
    }

    @Test
    fun lastSyncedRevisionStaysOnlyInTheLocalDatabase() {
        val getterAnnotation = CharacterRecord::class.java
            .getDeclaredMethod("getLastSyncedAt")
            .getAnnotation(Exclude::class.java)
        val fieldAnnotation = CharacterRecord::class.java
            .getDeclaredField("lastSyncedAt")
            .getAnnotation(Exclude::class.java)

        assertNotNull(getterAnnotation)
        assertNotNull(fieldAnnotation)
        assertEquals(456L, CharacterRecord(lastSyncedAt = 456L).toDomain().lastSyncedAt)
        assertEquals(456L, CharacterRecord(lastSyncedAt = 456L)
            .migratedStructuredRecord(markDirty = false).toDomain().toRecord().lastSyncedAt)
    }

    @Test
    fun isLockedKeepsTheSecurityRulesFieldName() {
        val getterAnnotation = CharacterRecord::class.java
            .getDeclaredMethod("isLocked")
            .getAnnotation(PropertyName::class.java)
        val fieldAnnotation = CharacterRecord::class.java
            .getDeclaredField("isLocked")
            .getAnnotation(PropertyName::class.java)

        assertEquals("isLocked", requireNotNull(getterAnnotation).value)
        assertEquals("isLocked", requireNotNull(fieldAnnotation).value)
    }

    @Test
    fun legacyNotesAreMigratedToTheFirstPersonalRecord() {
        val character = CharacterRecord(id = "character-1", notes = "Pista antiga")
            .migratedStructuredRecord(markDirty = false).toDomain()

        assertEquals(1, character.personalNotes.size)
        assertEquals("Registro Pessoal 1", character.personalNotes.single().title)
        assertEquals("Pista antiga", character.personalNotes.single().text)
        assertEquals("", character.toRecord().notes)
    }

    @Test
    fun deliveryApplicationMarkersSurviveLocalAndFirestoreMapping() {
        val markers = listOf("delivery-a", "delivery-b")

        assertEquals(markers, CharacterRecord(appliedDeliveryIds = markers)
            .migratedStructuredRecord(markDirty = false).toDomain().toRecord().appliedDeliveryIds)
    }

    @Test
    fun resourceConverterReadsTheLegacyFormatWithoutAnAdjustment() {
        val resource = CharacterConverters().stringToResource("4|12")

        assertEquals(4, resource.current)
        assertEquals(12, resource.maximum)
        assertEquals(0, resource.adjustment)
        assertTrue(CharacterConverters().resourceToString(resource).endsWith("|0"))
    }

    @Test
    fun defaultLegacyProtectionsAdoptAutomaticCalculations() {
        val attributes = defaultAttributes().map { attribute ->
            if (attribute.acronym == "AGI") attribute.copy(value = 2) else attribute
        }
        val character = CharacterRecord(attributes = attributes).toDomain()

        assertEquals(0, character.protectionAdjustments.getValue("Esquiva"))
        assertEquals(12, character.protectionTotal("Esquiva"))
    }

    @Test
    fun customizedLegacyProtectionTotalsArePreservedAsAdjustments() {
        val attributes = defaultAttributes().map { attribute ->
            if (attribute.acronym == "AGI") {
                attribute.copy(
                    value = 2,
                    skills = attribute.skills.map { if (it.name == "Reflexos") it.copy(value = 2) else it },
                )
            } else {
                attribute
            }
        }
        val character = CharacterRecord(
            attributes = attributes,
            protections = linkedMapOf(
                "Geral" to 19,
                "Esquiva" to 23,
                "Postura" to 10,
                "Mental" to 10,
                "Arcana" to 10,
            ),
        ).migratedStructuredRecord(markDirty = false).toDomain()

        assertEquals(9, character.protectionAdjustments.getValue("Geral"))
        assertEquals(0, character.protectionAdjustments.getValue("Esquiva"))
        assertEquals(23, character.protectionTotal("Esquiva"))
        assertTrue(character.toRecord().protections.isEmpty())
        assertEquals(character.calculatedProtections(), character.toRecord().toDomain().calculatedProtections())
    }

    @Test
    fun convertersPreserveEquipmentProtectionAndBodyAssignments() {
        val converters = CharacterConverters()
        val item = com.kinderman.sdo.domain.model.InventoryItem(
            id = "armor-1", pg = 4, pl = 3, category = "Armadura", agilityLimit = 3,
            quality = ItemQuality.ICONIC,
        )
        val region = com.kinderman.sdo.domain.model.BodyRegion(name = "Braço", equippedItemIds = listOf(item.id))

        assertEquals(item, converters.stringToInventory(converters.inventoryToString(listOf(item))).single())
        assertEquals(region, converters.stringToBody(converters.bodyToString(listOf(region))).single())
    }

    @Test
    fun raceSelectionMetadataSurvivesLocalAndFirestoreMapping() {
        val record = CharacterRecord(race = "Humanos", subRace = "Oráculo", raceAttribute = "AGI")

        assertEquals("AGI", record.toDomain().raceAttribute)
        assertEquals("AGI", record.migratedStructuredRecord(markDirty = false).toDomain().toRecord().raceAttribute)
    }
}
