package com.kinderman.sdo.data.sync

import com.kinderman.sdo.data.local.CharacterConverters
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.AmmoState
import com.kinderman.sdo.domain.model.AmmoTypeId
import com.kinderman.sdo.domain.model.CatalogReference
import com.kinderman.sdo.domain.model.CatalogScope
import com.kinderman.sdo.domain.model.ComponentInstallation
import com.kinderman.sdo.domain.model.ContainerState
import com.kinderman.sdo.domain.model.InstallationId
import com.kinderman.sdo.domain.model.ItemCatalogId
import com.kinderman.sdo.domain.model.ItemInstanceId
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.ItemState
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.normalizedForSparkV2
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.MAX_SPARK_ITEM_FIELD_TARGETS_PER_OPERATION
import com.kinderman.sdo.domain.model.MAX_SPARK_TARGETS_PER_OPERATION
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.ProgressionRecord
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.TargetKind
import com.kinderman.sdo.domain.model.TargetOp
import com.kinderman.sdo.domain.model.defaultAttributes
import com.kinderman.sdo.domain.model.isWithinSparkTransactionBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SparkWireCodecTest {

    @Test
    fun normalizedInventoryHydratesCanonicalItemStateWithStableIdentity() {
        val normalized = Character(id = "sheet", ownerId = "owner",
            inventory = listOf(InventoryItem(id = "potion", name = "Potion", quantity = 2)))
            .toRecord().normalizedForSparkV2()
        val record = CharacterRecord(id = "sheet", ownerId = "owner", lastEditedBy = "owner",
            dirty = false, inventory = normalized.inventory,
            canonicalItemsPayload = normalized.canonicalItemsPayload,
            itemSchemaVersion = normalized.itemSchemaVersion,
            canonicalSchemaVersion = normalized.canonicalSchemaVersion)
        val decoded = SparkWireCodec.fromSparkDocuments(SparkWireCodec.toSparkDocuments(record))
        assertEquals(record.copy(fieldUpdatedAt = decoded.fieldUpdatedAt), decoded)
        assertEquals(CharacterConverters().stringToCanonicalItems(record.canonicalItemsPayload),
            CharacterConverters().stringToCanonicalItems(decoded.canonicalItemsPayload))
    }

    @Test
    fun canonicalItemRoundTripPreservesNestedIdsAndAdminMigrationShapes() {
        assertItemStateRoundTrip(wrappedIds = false)
    }

    @Test
    fun legacyWrappedItemIdsHydrateWithoutChangingCanonicalValues() {
        assertItemStateRoundTrip(wrappedIds = true)
    }

    private fun assertItemStateRoundTrip(wrappedIds: Boolean) {
        val installation = ComponentInstallation(InstallationId("install-1"),
            CatalogReference(ItemCatalogId("component-1"), 3), 123L)
        val state = ItemState(ItemInstanceId("item-1"),
            CatalogReference(ItemCatalogId("definition-1"), 2), CatalogScope.Character,
            material = installation, installations = listOf(installation.copy(id = InstallationId("install-2"))),
            ammo = AmmoState(AmmoTypeId("ammo-1"), 2, 5),
            container = ContainerState(listOf(ItemInstanceId("contained-1"))))
        val converters = CharacterConverters()
        val record = CharacterRecord(id = "sheet", ownerId = "owner", lastEditedBy = "owner", dirty = false,
            canonicalItemsPayload = converters.canonicalItemsToString(listOf(state)))
        val docs = SparkWireCodec.toSparkDocuments(record)
        val canonical = kotlinx.serialization.json.Json { encodeDefaults = true }
            .encodeToString(ItemState.serializer(), state)
        @Suppress("UNCHECKED_CAST")
        val values = com.google.gson.GsonBuilder().setObjectToNumberStrategy(
            com.google.gson.ToNumberPolicy.LONG_OR_DOUBLE).create().fromJson(canonical, Map::class.java) as Map<String, Any?>
        fun wrap(value: Any?, key: String = ""): Any? = when {
            wrappedIds && key in setOf("id", "ammoTypeId") && value is String -> mapOf("value" to value)
            value is Map<*, *> -> value.entries.associate { it.key.toString() to wrap(it.value, it.key.toString()) }
            value is List<*> -> value.map { if (wrappedIds && key == "containedItemIds") mapOf("value" to it) else wrap(it) }
            else -> value
        }
        val bundle = docs.items.getValue("istate_item-1")
        val adminOrLegacy = bundle.copy(fields = bundle.fields.mapValues { (key, field) ->
            field + ("value" to wrap(values[key], key))
        })
        val decoded = SparkWireCodec.fromSparkDocuments(docs.copy(items =
            docs.items + ("istate_item-1" to adminOrLegacy)))
        assertEquals(listOf(state), converters.stringToCanonicalItems(decoded.canonicalItemsPayload))
        assertEquals(record.copy(fieldUpdatedAt = decoded.fieldUpdatedAt), decoded)
        val reencoded = SparkWireCodec.toSparkDocuments(decoded)
        assertEquals(docs.items.keys, reencoded.items.keys)
    }

    @Test
    fun fullRecordRoundTripPreservesAllDomainAndCanonicalFields() {
        val original = CharacterRecord(
            id = "sheet-1",
            ownerId = "owner-1",
            campaignId = "camp-1",
            name = "Árvore Dourada",
            race = "Humano",
            subRace = "Nortenho",
            raceAttribute = "VIG",
            occupation = "Guardião",
            age = "28",
            height = "1,80m",
            sex = "M",
            size = "Médio",
            level = 4,
            canonicalSchemaVersion = 2,
            itemSchemaVersion = 4,
            migrationReviewPayload = "review-payload",
            canonicalBodyPayload = "{\"regions\":[]}",
            activeModifiersPayload = "[]",
            scopedItemCatalogPayload = "[]",
            canonicalProgressionPayload = "[]",
            progressionLifeBonus = 2,
            progressionHistory = listOf(ProgressionRecord(id = "prog-1", previousLevel = 1, newLevel = 2, appliedAt = 100L)),
            money = 120,
            life = ResourceValue(14, 18, 1),
            sanity = ResourceValue(10, 12, 0),
            arcane = ResourceValue(6, 8, 2),
            energy = ResourceValue(5, 7, 0),
            destiny = ResourceValue(3, 5, 0),
            exhaustion = ResourceValue(1, 10, 0),
            corruption = ResourceValue(12, 100, 0),
            attributes = defaultAttributes(),
            protectionAdjustments = mapOf("Geral" to 2, "Esquiva" to 1),
            positiveTraits = listOf("Corajoso"),
            negativeTraits = listOf("Teimoso"),
            learnedKnowledges = listOf(SpecialKnowledge(id = "know-1", name = "Ocultismo", attribute = "INT", value = 2)),
            pathName = "Lâmina",
            pathMotto = "Sem recuar",
            pathKeywords = listOf("Ferro", "Juramento", "Vigia"),
            pathPillars = listOf("Honra", "Disciplina", "Fogo"),
            powers = listOf(Power(id = "pow-1", name = "Golpe Solar", active = true, favorite = true)),
            inventory = listOf(
                InventoryItem(id = "inv-1", name = "Espada Longa", quantity = 1, load = 2, acquiredAt = 500L),
                InventoryItem(id = "inv/special:id", name = "Amuleto", quantity = 3, acquiredAt = 600L),
            ),
            mysticAbilities = listOf(MysticAbility(id = "mys-1", name = "Marca Cinzenta", available = true)),
            conditions = listOf(ConditionEffect(id = "lcond-1", name = "Sangramento", intensity = "2")),
            story = "História longa",
            personalNotes = listOf(
                PersonalNote(id = "note-1", title = "Diário", text = "Entrada 1"),
                PersonalNote(id = "note/with space", title = "Segredo", text = "Código 42"),
            ),
            photoUri = "preset:guardian",
            lockType = "PLAYER",
            isLocked = true,
            lockedBy = "owner-1",
            lockedAt = 777L,
            updatedAt = 900L,
            lastEditedBy = "owner-1",
            appliedDeliveryIds = listOf("deliv-1"),
        ).normalizedForSparkV2()

        val sparkDocs = SparkWireCodec.toSparkDocuments(
            original,
            revision = 5L,
            authorizedCampaignId = "camp-consent",
        )

        assertEquals(2, sparkDocs.root["protocolVersion"])
        assertEquals(2, sparkDocs.root["schemaVersion"])
        assertEquals("camp-consent", sparkDocs.root["authorizedCampaignId"])
        assertEquals("preset:guardian", sparkDocs.fields["avatarPreset"]?.get("value"))
        assertNull(sparkDocs.mediaAvatar)
        assertTrue(sparkDocs.items.keys.all {
            it.matches(Regex("^(inv|istate|abil|pow|mys|cond|lcond|note|attr)_[A-Za-z0-9_.:=-]{1,128}$"))
        })
        // Granular item property keys must match Spark v2 independent field contract.
        val invFields = sparkDocs.items["inv_inv-1"]!!.fields.keys
        assertTrue(setOf("name", "state", "quantity", "durabilityCurrent", "durabilityMax", "itemCondition", "meta").all(invFields::contains))
        val powFields = sparkDocs.items["pow_pow-1"]!!.fields.keys
        assertTrue(setOf("name", "cost", "costType", "costValue", "active", "favorite", "available", "meta").all(powFields::contains))
        // Skill keys must obey Spark v2 granular regex even for Portuguese accented names like "Lábia".
        val carSkills = sparkDocs.items["attr_CAR"]!!.fields.keys.filter { it.startsWith("skill_") }
        assertTrue(carSkills.isNotEmpty())
        assertTrue(carSkills.all { it.matches(Regex("^skill_[A-Za-z0-9_.-]{1,64}_(value|modifier|name)$")) })

        val decoded = SparkWireCodec.fromSparkDocuments(sparkDocs)
        assertEquals(original.copy(fieldUpdatedAt = decoded.fieldUpdatedAt), decoded)
    }

    @Test
    fun avatarHandlingEnforces64KiBThumbnailLimitAndLocalOnlyOriginals() {
        val smallThumb = "data:image/jpeg;base64," + "A".repeat(1_024)
        val smallDocs = SparkWireCodec.toSparkDocuments(CharacterRecord(id = "s1", photoUri = smallThumb))
        assertNotNull(smallDocs.mediaAvatar)
        assertEquals("", smallDocs.fields["avatarPreset"]?.get("value"))
        assertEquals(smallThumb, smallDocs.localPhotoUri)
        // Remote reader without localPhotoUri still receives the <=64KiB thumbnail.
        assertEquals(smallThumb, SparkWireCodec.fromSparkDocuments(smallDocs.copy(localPhotoUri = "")).photoUri)

        val oversized = "data:image/jpeg;base64," + "A".repeat(70_000)
        val oversizedDocs = SparkWireCodec.toSparkDocuments(CharacterRecord(id = "s2", photoUri = oversized))
        assertNull(oversizedDocs.mediaAvatar)
        assertEquals(oversized, oversizedDocs.localPhotoUri)
        assertEquals(oversized, SparkWireCodec.fromSparkDocuments(oversizedDocs).photoUri)

        val contentUri = "content://media/external/images/media/42"
        val contentDocs = SparkWireCodec.toSparkDocuments(CharacterRecord(id = "s3", photoUri = contentUri))
        assertNull(contentDocs.mediaAvatar)
        assertEquals(contentUri, contentDocs.localPhotoUri)
        assertEquals(contentUri, SparkWireCodec.fromSparkDocuments(contentDocs).photoUri)
    }

    @Test
    fun diffBatchesChunkToAtMostThreeTargetsAndEmitTombstonesForRemovedItems() {
        val before = CharacterRecord(
            id = "sheet-1",
            ownerId = "owner-1",
            campaignId = "default",
            name = "Ayla",
            race = "Humano",
            occupation = "Batedora",
            money = 10,
            inventory = listOf(
                InventoryItem(id = "keep-1", name = "Arco", quantity = 1),
                InventoryItem(id = "drop-1", name = "Tocha", quantity = 2),
            ),
        )
        val after = before.copy(
            campaignId = "",
            name = "Ayla Vael",
            race = "Elfa",
            occupation = "Arqueira",
            money = 40,
            story = "Nova história",
            inventory = listOf(
                InventoryItem(id = "keep-1", name = "Arco", quantity = 1),
                InventoryItem(id = "new-1", name = "Flecha", quantity = 10),
            ),
        )

        val batches = SparkWireCodec.buildOperationBatches(
            before = before,
            after = after,
            operationIdPrefix = "op-diff",
            actorUid = "owner-1",
            existingRevisions = mapOf(
                "field:name" to 1L,
                "field:race" to 1L,
                "field:occupation" to 1L,
                "field:money" to 1L,
                "field:story" to 1L,
                "item:inv_drop-1" to 3L,
            ),
            sheetRevision = 4L,
        )

        assertTrue(batches.size >= 2)
        assertTrue(batches.all { isWithinSparkTransactionBudget(it.targets) })
        assertTrue(batches.filter { b -> b.targets.any { it.kind == TargetKind.ITEM_FIELD } }.all { it.targets.size <= MAX_SPARK_ITEM_FIELD_TARGETS_PER_OPERATION })
        assertTrue(batches.all { it.targets.size in 1..MAX_SPARK_TARGETS_PER_OPERATION })

        val allTargets = batches.flatMap { it.targets }
        val nameTarget = allTargets.first { it.kind == TargetKind.FIELD && it.key == "name" }
        assertEquals("\"Ayla\"", nameTarget.baseValueJson)
        assertEquals("\"Ayla Vael\"", nameTarget.valueJson)
        assertTrue(allTargets.any { it.kind == TargetKind.ITEM && it.itemId == "inv_drop-1" && it.op == TargetOp.TOMBSTONE && it.expectedRevision == 3L })
        val createIdx = allTargets.indexOfFirst { it.kind == TargetKind.ITEM && it.itemId == "inv_new-1" && it.op == TargetOp.CREATE }
        val firstFieldIdx = allTargets.indexOfFirst { it.kind == TargetKind.ITEM_FIELD && it.itemId == "inv_new-1" }
        assertTrue(createIdx >= 0 && firstFieldIdx > createIdx)

        assertFalse(batches.map { it.operationId }.distinct().size != batches.size)
        // Root campaignId "default" normalizes to "" and authorizedCampaignId defaults to "".
        val docs = SparkWireCodec.toSparkDocuments(before)
        assertEquals("", docs.root["campaignId"])
        assertEquals("", docs.root["authorizedCampaignId"])
    }

    @Test
    fun inventoryItemQualityDecodesPortuguesePropertyNamesAndEnumNames() {
        val cases = listOf(
            "Mundana" to ItemQuality.MUNDANE,
            "Comum" to ItemQuality.COMMON,
            "Aprimorada" to ItemQuality.IMPROVED,
            "Icônica" to ItemQuality.ICONIC,
            "Obra-Prima" to ItemQuality.MASTERPIECE,
            "Artefato" to ItemQuality.ARTIFACT,
            "Anciã" to ItemQuality.ANCIENT,
            "IMPROVED" to ItemQuality.IMPROVED,
            "unknown" to ItemQuality.COMMON,
        )
        cases.forEachIndexed { idx, (rawQuality, expectedQuality) ->
            val itemId = "item-$idx"
            val wireItemId = "inv_$itemId"
            val base = Character(
                id = "sheet-quality",
                ownerId = "owner-1",
                inventory = listOf(InventoryItem(id = itemId, name = "Item $idx")),
            ).toRecord().normalizedForSparkV2()
            val docs = SparkWireCodec.toSparkDocuments(base)
            val bundle = docs.items.getValue(wireItemId)
            val updatedFields = bundle.fields.toMutableMap()
            updatedFields["quality"] = mapOf("key" to "quality", "value" to rawQuality, "revision" to 1L)
            val patchedDocs = docs.copy(items = docs.items + (wireItemId to bundle.copy(fields = updatedFields)))

            val decoded = SparkWireCodec.fromSparkDocuments(patchedDocs)
            assertEquals("Failed for quality=$rawQuality", expectedQuality, decoded.inventory.single().quality)
        }
    }
}
