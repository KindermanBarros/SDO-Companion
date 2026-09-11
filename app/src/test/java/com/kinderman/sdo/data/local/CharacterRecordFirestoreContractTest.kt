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
import com.kinderman.sdo.domain.model.CURRENT_ITEM_DATA_VERSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterRecordFirestoreContractTest {
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
        assertEquals(draft, CharacterRecord(itemCreationDraft = draft, creationRulesVersion = 2).toDomain().toRecord().itemCreationDraft)
    }

    @Test fun knowledgeMilestoneRewardsSurviveRoomConversion() {
        val reward = KnowledgeMilestoneReward(3, KnowledgeMilestoneRewardType.POWER, "power.rhythm", "instance-1")
        val knowledge = SpecialKnowledge(name = "Música", milestoneLevels = listOf(3), milestoneRewards = listOf(reward))
        val converters = CharacterConverters()

        assertEquals(knowledge, converters.stringToKnowledges(converters.knowledgesToString(listOf(knowledge))).single())
    }

    @Test fun legacyEmbeddedRacialBonusMovesToDerivedModifierOnce() {
        val attributes = defaultAttributes().map { if (it.acronym == "CAR") it.copy(value = 3) else it }
        val migrated = CharacterRecord(race = "Humanos", raceAttribute = "CAR", attributes = attributes, creationRulesVersion = 1).toDomain()

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

        val migrated = CharacterRecord(inventory = listOf(legacy)).toDomain().toRecord().inventory

        assertTrue(migrated.isEmpty())
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
        assertEquals(456L, CharacterRecord(lastSyncedAt = 456L).toDomain().toRecord().lastSyncedAt)
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
        val character = CharacterRecord(id = "character-1", notes = "Pista antiga").toDomain()

        assertEquals(1, character.personalNotes.size)
        assertEquals("Registro Pessoal 1", character.personalNotes.single().title)
        assertEquals("Pista antiga", character.personalNotes.single().text)
        assertEquals("", character.toRecord().notes)
    }

    @Test
    fun deliveryApplicationMarkersSurviveLocalAndFirestoreMapping() {
        val markers = listOf("delivery-a", "delivery-b")

        assertEquals(markers, CharacterRecord(appliedDeliveryIds = markers).toDomain().toRecord().appliedDeliveryIds)
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
        ).toDomain()

        assertEquals(9, character.protectionAdjustments.getValue("Geral"))
        assertEquals(0, character.protectionAdjustments.getValue("Esquiva"))
        assertEquals(23, character.protectionTotal("Esquiva"))
        assertEquals(character.calculatedProtections(), character.toRecord().protections)
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
        assertEquals("AGI", record.toDomain().toRecord().raceAttribute)
    }
}
