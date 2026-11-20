package com.kinderman.sdo.data.sync

import com.kinderman.sdo.data.local.CharacterConverters
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.data.local.toDomain
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

    @Test
    fun existingSheetWithConditionsStoryNotesAndNewPreEraCatalogEntriesRoundTripsLosslessly() {
        val original = Character(
            id = "sheet-pre-era",
            ownerId = "owner-1",
            name = "Chronista",
            pathName = "Intérprete do Antes-Véu",
            story = "História gravada antes da Fratura.",
            personalNotes = listOf(com.kinderman.sdo.domain.model.PersonalNote(id = "note-1", title = "Sinal", text = "Essalhen")),
            learnedKnowledges = listOf(
                com.kinderman.sdo.domain.model.SpecialKnowledge(
                    id = "k-essalhen",
                    catalogEntryId = "knowledge.acquired_knowledge.essalhen",
                    name = "Essalhen",
                    attribute = "INT",
                    value = 2,
                ),
            ),
            arcaneKnowledges = listOf(
                com.kinderman.sdo.domain.model.SpecialKnowledge(
                    id = "k-primeiros",
                    catalogEntryId = "knowledge.arcane_knowledge.magia_dos_primeiros",
                    name = "Magia dos Primeiros",
                    attribute = "POD",
                    value = 2,
                ),
            ),
            conditionInstances = listOf(
                com.kinderman.sdo.domain.model.ConditionInstance(
                    payload = com.kinderman.sdo.domain.model.ConditionPayload(
                        kind = com.kinderman.sdo.domain.model.ConditionKind.Abalado,
                        intensity = 2,
                        duration = com.kinderman.sdo.domain.model.Duration(
                            com.kinderman.sdo.domain.model.DurationKind.Turns,
                            turns = 3,
                        ),
                    ),
                    source = com.kinderman.sdo.domain.model.Source.narrative(
                        com.kinderman.sdo.domain.model.NarrativeSourceId("manual-condition"),
                    ),
                ),
            ),
            inventory = listOf(
                InventoryItem(
                    id = "item-astrolabio",
                    catalogEntryId = "item.astrolabio_de_lo",
                    name = "Astrolábio de Céu Morto",
                    quantity = 1,
                ),
            ),
        ).toRecord().normalizedForSparkV2()

        val docs = SparkWireCodec.toSparkDocuments(original)
        val decoded = SparkWireCodec.fromSparkDocuments(docs)
        val batches = SparkWireCodec.buildOperationBatches(
            before = null,
            after = Character(id = "sheet-new", ownerId = "owner-1").toRecord(),
            operationIdPrefix = "op-fast-create",
            actorUid = "owner-1",
            skipDefaultFieldsOnCreate = true,
        )

        assertEquals(original.pathName, decoded.pathName)
        assertEquals(original.story, decoded.story)
        assertEquals(original.personalNotes.single().title, decoded.personalNotes.single().title)
        assertEquals(original.learnedKnowledges.single().catalogEntryId, decoded.learnedKnowledges.single().catalogEntryId)
        assertEquals(original.arcaneKnowledges.single().catalogEntryId, decoded.arcaneKnowledges.single().catalogEntryId)
        assertEquals(original.toDomain().conditionInstances.single().intensity, decoded.toDomain().conditionInstances.single().intensity)
        assertEquals(original.inventory.single().name, decoded.inventory.single().name)
        val fullBatches = SparkWireCodec.buildOperationBatches(
            before = null,
            after = Character(id = "sheet-new", ownerId = "owner-1").toRecord(),
            operationIdPrefix = "op-full-create",
            actorUid = "owner-1",
        )
        val fastTargets = batches.sumOf { it.targets.size }
        val fullTargets = fullBatches.sumOf { it.targets.size }
        assertEquals("New default sheet must fit in a single Spark transaction", 1, batches.size)
        assertTrue("fast=$fastTargets full=$fullTargets", fastTargets in 1..7 && fastTargets < fullTargets / 5)
    }

    @Test
    fun `condition with null intensity encodes valid int32 mirror field and preserves null on decode`() {
        val original = Character(
            id = "sheet-cond-null-intensity",
            ownerId = "owner-1",
            name = "Cond Test",
            conditionInstances = listOf(
                com.kinderman.sdo.domain.model.ConditionInstance(
                    payload = com.kinderman.sdo.domain.model.ConditionPayload(
                        kind = com.kinderman.sdo.domain.model.ConditionKind.Abalado,
                        intensity = null,
                        duration = com.kinderman.sdo.domain.model.Duration(
                            com.kinderman.sdo.domain.model.DurationKind.Instant,
                        ),
                    ),
                    source = com.kinderman.sdo.domain.model.Source.narrative(
                        com.kinderman.sdo.domain.model.NarrativeSourceId("manual-condition"),
                    ),
                ),
            ),
        ).toRecord().normalizedForSparkV2()

        val docs = SparkWireCodec.toSparkDocuments(original)
        val condBundle = docs.items.values.first { it.document["kind"] == "CONDITION" }
        assertEquals(0, condBundle.fields["intensity"]?.get("value"))

        val decoded = SparkWireCodec.fromSparkDocuments(docs).toDomain()
        assertEquals(null, decoded.conditionInstances.single().intensity)
        assertEquals(com.kinderman.sdo.domain.model.ConditionKind.Abalado, decoded.conditionInstances.single().kind)
    }

    @Test
    fun `editing attribute omitted on fast-create materializes parent item and preserves all default attributes on decode`() {
        val initial = Character(id = "sheet-attr-sparse", ownerId = "owner-1", name = "Sparse").toRecord()
        val edited = initial.toDomain().let { ch ->
            ch.copy(attributes = ch.attributes.map { if (it.acronym == "FOR") it.copy(value = 3) else it })
        }.toRecord()

        val batches = SparkWireCodec.buildOperationBatches(
            before = initial,
            after = edited,
            operationIdPrefix = "op-edit-for",
            actorUid = "owner-1",
            existingRevisions = emptyMap(),
            sheetRevision = 1L,
        )

        val allTargets = batches.flatMap { it.targets }
        assertTrue(
            "Expected ITEM CREATE for attr_FOR",
            allTargets.any { it.kind == com.kinderman.sdo.domain.model.TargetKind.ITEM && it.itemId == "attr_FOR" && it.op == com.kinderman.sdo.domain.model.TargetOp.CREATE },
        )
        assertTrue(
            "Expected ITEM_FIELD CREATE for attr_FOR/value",
            allTargets.any { it.kind == com.kinderman.sdo.domain.model.TargetKind.ITEM_FIELD && it.itemId == "attr_FOR" && it.key == "value" },
        )

        // Decode a sparse snapshot containing ONLY attr_FOR with value=3 (no AGI..CAR docs and no skill docs).
        val fullDocs = SparkWireCodec.toSparkDocuments(edited)
        val sparseForBundle = fullDocs.items.getValue("attr_FOR").let { b ->
            b.copy(fields = b.fields.filterKeys { it in setOf("name", "acronym", "value") })
        }
        val sparseDocs = fullDocs.copy(items = mapOf("attr_FOR" to sparseForBundle))
        val decoded = SparkWireCodec.fromSparkDocuments(sparseDocs).toDomain()

        assertEquals(6, decoded.attributes.size)
        assertEquals(3, decoded.attributes.first { it.acronym == "FOR" }.value)
        assertTrue(decoded.attributes.first { it.acronym == "FOR" }.skills.isNotEmpty())
        assertEquals(0, decoded.attributes.first { it.acronym == "AGI" }.value)
    }

    @Test
    fun `simultaneous field edit and item removal separates tombstones into DELETE_ITEM batch with matching operationId`() {
        val oldPower = com.kinderman.sdo.domain.model.Power(id = "pow-old", name = "Antigo", costValue = -2)
        val newPower = com.kinderman.sdo.domain.model.Power(id = "pow-new", name = "Novo", costValue = -1)
        val before = Character(
            id = "sheet-path-switch",
            ownerId = "owner-1",
            pathName = "Caminho A",
            powers = listOf(oldPower),
        ).toRecord()
        val after = before.toDomain().copy(
            pathName = "Caminho B",
            powers = listOf(newPower),
        ).toRecord()

        val batches = SparkWireCodec.buildOperationBatches(
            before = before,
            after = after,
            operationIdPrefix = "op-switch-path",
            actorUid = "owner-1",
            existingRevisions = mapOf("field:pathName" to 1L, "item:pow_pow-old" to 1L),
            sheetRevision = 1L,
        )

        val tombstoneBatches = batches.filter { b -> b.targets.any { it.op == com.kinderman.sdo.domain.model.TargetOp.TOMBSTONE } }
        assertTrue("Expected at least one tombstone batch", tombstoneBatches.isNotEmpty())
        tombstoneBatches.forEach { batch ->
            assertEquals(com.kinderman.sdo.domain.model.OperationType.DELETE_ITEM, batch.type)
            assertTrue(batch.targets.all { it.op == com.kinderman.sdo.domain.model.TargetOp.TOMBSTONE })
        }
        batches.forEach { batch ->
            batch.writes.forEach { write ->
                if (!write.isMedia && "lastOperationId" in write.documentData) {
                    assertEquals(batch.operationId, write.documentData["lastOperationId"])
                }
            }
        }
        val docs = SparkWireCodec.toSparkDocuments(
            after.copy(
                inventory = listOf(
                    InventoryItem(id = "inv-neg", name = "Negativo", durabilityCurrent = -3, durabilityMax = -1, pg = -2, pl = -4, load = -5),
                ),
            ),
        )
        assertTrue((docs.items.getValue("pow_pow-new").fields.getValue("costValue")["value"] as Int) >= 0)
        val invFields = docs.items.getValue("inv_inv-neg").fields
        assertEquals(0, invFields.getValue("durabilityCurrent")["value"])
        assertEquals(0, invFields.getValue("durabilityMax")["value"])
        assertEquals(0, invFields.getValue("pg")["value"])
        assertEquals(0, invFields.getValue("pl")["value"])
        assertEquals(0, invFields.getValue("load")["value"])
    }

    @Test
    fun `replaying inventory and power meta targets preserves heritageCost acquisitionSource and power sourceType`() {
        val before = Character(
            id = "sheet-replay-meta",
            ownerId = "owner-1",
            creationStatus = com.kinderman.sdo.domain.model.CharacterCreationStatus.DRAFT,
            creationStep = 7,
        )
        val heritageItem = InventoryItem(
            id = "inv-heritage-1",
            name = "Espada Longa",
            category = "Arma média",
            acquisitionSource = com.kinderman.sdo.domain.model.ItemAcquisitionSource.HERITAGE,
            heritageCost = 5,
            catalogEntryId = "item.regular_6",
            baseId = "espada_longa",
            materialId = "ligas_comuns",
            canonical = true,
        )
        val pathPower = com.kinderman.sdo.domain.model.Power(
            id = "pow-path-1",
            name = "Poder do Caminho",
            sourceType = com.kinderman.sdo.domain.model.PowerSourceType.PATH,
        )
        val after = before.copy(
            inventory = listOf(heritageItem),
            powers = listOf(pathPower),
        )
        val batches = SparkWireCodec.buildOperationBatches(
            before = before.toRecord(),
            after = after.toRecord(),
            operationIdPrefix = "op-replay-meta",
            actorUid = "owner-1",
            existingRevisions = emptyMap(),
            sheetRevision = 1L,
        )
        val intents = batches.map { batch ->
            com.kinderman.sdo.domain.model.SheetIntent(
                operationId = batch.operationId,
                accountId = "owner-1",
                sheetId = before.id,
                type = batch.type,
                targetKey = batch.targets.first().canonicalTargetKey(),
                targets = batch.targets,
                baseRevision = 1L,
            )
        }
        val replay = com.kinderman.sdo.domain.model.applyPendingIntentsOnConfirmed(
            confirmed = com.kinderman.sdo.domain.model.AccountSheetSnapshot(
                accountId = "owner-1",
                sheetId = before.id,
                sheetRevision = 1L,
                confirmedCharacter = before,
            ),
            intents = intents,
        )
        val replayed = replay.projectedCharacter

        assertEquals(5, com.kinderman.sdo.domain.creation.CharacterCreation.heritageSpent(replayed))
        assertEquals(
            com.kinderman.sdo.domain.model.ItemAcquisitionSource.HERITAGE,
            replayed.inventory.single().acquisitionSource,
        )
        assertEquals(5, replayed.inventory.single().heritageCost)
        assertEquals(
            com.kinderman.sdo.domain.model.PowerSourceType.PATH,
            replayed.powers.single().sourceType,
        )
    }

    @Test
    fun addingInventoryItemsPacksIntoSingleAtomicMetaBatchAndFollowUpEditPatchesSingleField() {
        val before = Character(
            id = "sheet-fast-inv",
            ownerId = "owner-1",
            name = "Ayla",
        )
        val sword = InventoryItem(
            id = "inv-fast-1",
            name = "Espada Longa",
            category = "Arma média",
            load = 2,
            durabilityCurrent = 5,
            durabilityMax = 5,
            effect = "Corte preciso",
            quantity = 1,
            acquisitionSource = com.kinderman.sdo.domain.model.ItemAcquisitionSource.HERITAGE,
            heritageCost = 5,
            catalogEntryId = "item.regular_6",
            canonical = true,
        )
        val ammo = InventoryItem(
            id = "inv-fast-2",
            name = "Flechas",
            category = "Munição",
            load = 1,
            quantity = 10,
            acquisitionSource = com.kinderman.sdo.domain.model.ItemAcquisitionSource.HERITAGE,
            heritageCost = 0,
        )
        val afterSingleAdd = before.copy(inventory = listOf(sword))
        val singleAddBatches = SparkWireCodec.buildOperationBatches(
            before = before.toRecord(),
            after = afterSingleAdd.toRecord(),
            operationIdPrefix = "op-fast-single",
            actorUid = "owner-1",
            existingRevisions = emptyMap(),
            sheetRevision = 1L,
        )
        // A brand-new inventory item packs `inv_` into 2 targets (item + meta) and omits default `istate_` fields,
        // fitting in <= 2 parallel batches instead of 5-7 chained transactions.
        assertTrue("Expected <= 2 batches for 1 new item, got ${singleAddBatches.size}", singleAddBatches.size <= 2)
        val singleInvTargets = singleAddBatches.flatMap { it.targets }.filter { it.itemId == "inv_inv-fast-1" }
        assertEquals(2, singleInvTargets.size)
        assertEquals("meta", singleInvTargets.first { it.kind == TargetKind.ITEM_FIELD }.key)

        val afterAdd = before.copy(inventory = listOf(sword, ammo))
        val addBatches = SparkWireCodec.buildOperationBatches(
            before = before.toRecord(),
            after = afterAdd.toRecord(),
            operationIdPrefix = "op-fast-add",
            actorUid = "owner-1",
            existingRevisions = emptyMap(),
            sheetRevision = 1L,
        )
        // Two brand-new inventory items (inv_ + istate_ each) fit in <= 3 batches (was 10+!).
        assertTrue("Expected <= 3 batches for 2 new items, got ${addBatches.size}", addBatches.size <= 3)
        val metaWrite = addBatches.flatMap { it.writes }.first {
            it.target.kind == TargetKind.ITEM_FIELD && it.target.itemId == "inv_inv-fast-1"
        }
        assertEquals("meta", metaWrite.target.key)
        val metaValue = metaWrite.documentData["value"] as Map<*, *>
        assertTrue("Rules limit meta map to <= 50 keys, got ${metaValue.size}", metaValue.size <= 50)

        // Optimistic replay preserves every packed field of both items.
        val intents = addBatches.map { batch ->
            com.kinderman.sdo.domain.model.SheetIntent(
                operationId = batch.operationId,
                accountId = "owner-1",
                sheetId = before.id,
                type = batch.type,
                targetKey = batch.targets.first().canonicalTargetKey(),
                targets = batch.targets,
                baseRevision = 1L,
            )
        }
        val replayed = com.kinderman.sdo.domain.model.applyPendingIntentsOnConfirmed(
            confirmed = com.kinderman.sdo.domain.model.AccountSheetSnapshot(
                accountId = "owner-1",
                sheetId = before.id,
                sheetRevision = 1L,
                confirmedCharacter = before,
            ),
            intents = intents,
        ).projectedCharacter
        val replayedSword = replayed.inventory.first { it.id == "inv-fast-1" }
        assertEquals("Espada Longa", replayedSword.name)
        assertEquals("Arma média", replayedSword.category)
        assertEquals(2, replayedSword.load)
        assertEquals(5, replayedSword.durabilityCurrent)
        assertEquals("Corte preciso", replayedSword.effect)
        assertEquals(5, replayedSword.heritageCost)

        // Follow-up edit on quantity patches ONLY the single quantity subfield in 1 transaction.
        val afterEdit = afterAdd.copy(inventory = listOf(sword, ammo.copy(quantity = 9)))
        val editBatches = SparkWireCodec.buildOperationBatches(
            before = afterAdd.toRecord(),
            after = afterEdit.toRecord(),
            operationIdPrefix = "op-fast-edit",
            actorUid = "owner-1",
            existingRevisions = mapOf(
                "item:inv_inv-fast-1" to 1L,
                "item:inv_inv-fast-1/field:meta" to 1L,
                "item:inv_inv-fast-2" to 1L,
                "item:inv_inv-fast-2/field:meta" to 1L,
                "item:istate_inv-fast-2" to 1L,
            ),
            sheetRevision = 1L,
        )
        assertEquals(1, editBatches.size)
        val invEditTargets = editBatches.single().targets.filter { it.itemId == "inv_inv-fast-2" }
        assertEquals(1, invEditTargets.size)
        assertEquals("quantity", invEditTargets.single().key)
    }

    @Test
    fun addingPowersMysticAndConditionsPacksEachIntoTwoTargetsWithinRulesLimits() {
        val before = Character(
            id = "sheet-fast-other",
            ownerId = "owner-1",
            name = "Ayla",
        )
        val power = com.kinderman.sdo.domain.model.Power(
            id = "pow-fast-1",
            name = "Golpe Solar",
            cost = "2 PE",
            costType = com.kinderman.sdo.domain.model.AbilityCostType.ENERGY,
            costValue = 2,
            action = "Ação",
            range = "Corpo a corpo",
            effect = "Causa dano radiante",
            sourceType = com.kinderman.sdo.domain.model.PowerSourceType.PATH,
        )
        val mystic = com.kinderman.sdo.domain.model.MysticAbility(
            id = "mys-fast-1",
            name = "Chama Arcana",
            cost = "3 PA",
            costType = com.kinderman.sdo.domain.model.AbilityCostType.ARCANE,
            costValue = 3,
            action = "Ação",
            range = "Médio",
            effect = "Incendeia o alvo",
        )
        val cond = com.kinderman.sdo.domain.model.ConditionInstance(
            instanceId = com.kinderman.sdo.domain.model.ConditionInstanceId("cond-fast-1"),
            payload = com.kinderman.sdo.domain.model.ConditionPayload(
                kind = com.kinderman.sdo.domain.model.ConditionKind.Abalado,
                intensity = 2,
                duration = com.kinderman.sdo.domain.model.Duration(
                    com.kinderman.sdo.domain.model.DurationKind.Instant,
                ),
            ),
        )
        val after = before.copy(
            powers = listOf(power),
            mysticAbilities = listOf(mystic),
            conditionInstances = listOf(cond),
        )

        val batches = SparkWireCodec.buildOperationBatches(
            before = before.toRecord(),
            after = after.toRecord(),
            operationIdPrefix = "op-fast-all",
            actorUid = "owner-1",
            existingRevisions = emptyMap(),
            sheetRevision = 1L,
        )
        val allWrites = batches.flatMap { it.writes }
        val powWrites = allWrites.filter { it.target.itemId == "pow_pow-fast-1" }
        val mysWrites = allWrites.filter { it.target.itemId == "mys_mys-fast-1" }
        val condWrites = allWrites.filter { it.target.itemId == "cond_cond-fast-1" }

        assertEquals(2, powWrites.size)
        assertEquals("meta", powWrites.first { it.target.kind == TargetKind.ITEM_FIELD }.target.key)
        assertTrue((powWrites.first { it.target.kind == TargetKind.ITEM_FIELD }.documentData["value"] as Map<*, *>).size <= 50)

        assertEquals(2, mysWrites.size)
        assertEquals("meta", mysWrites.first { it.target.kind == TargetKind.ITEM_FIELD }.target.key)
        assertTrue((mysWrites.first { it.target.kind == TargetKind.ITEM_FIELD }.documentData["value"] as Map<*, *>).size <= 50)

        assertEquals(2, condWrites.size)
        assertEquals("payload", condWrites.first { it.target.kind == TargetKind.ITEM_FIELD }.target.key)

        // Optimistic replay preserves packed Power & MysticAbility fields.
        val intents = batches.map { batch ->
            com.kinderman.sdo.domain.model.SheetIntent(
                operationId = batch.operationId,
                accountId = "owner-1",
                sheetId = before.id,
                type = batch.type,
                targetKey = batch.targets.first().canonicalTargetKey(),
                targets = batch.targets,
                baseRevision = 1L,
            )
        }
        val replayed = com.kinderman.sdo.domain.model.applyPendingIntentsOnConfirmed(
            confirmed = com.kinderman.sdo.domain.model.AccountSheetSnapshot(
                accountId = "owner-1",
                sheetId = before.id,
                sheetRevision = 1L,
                confirmedCharacter = before,
            ),
            intents = intents,
        ).projectedCharacter
        assertEquals("Golpe Solar", replayed.powers.single().name)
        assertEquals(2, replayed.powers.single().costValue)
        assertEquals("Chama Arcana", replayed.mysticAbilities.single().name)
        assertEquals(3, replayed.mysticAbilities.single().costValue)
    }

    @Test
    fun appGeneratedWireDocumentsMatchFirestoreRulesContract() {
        val full = Character(
            id = "sheet-rules-audit",
            ownerId = "uid-player-1",
            campaignId = "default",
            name = "Arion",
            race = "Humano",
            occupation = "Cronista",
            photoUri = "data:image/webp;base64,UklGRh4AAABXRUJQVlA4TBEAAAAvAAAAAAfQ//73v/+BiOh/AAA=",
            level = 5,
            money = 250,
            life = ResourceValue(current = 18, maximum = 22, adjustment = 2),
            sanity = ResourceValue(current = 15, maximum = 20, adjustment = 0),
            arcane = ResourceValue(current = 8, maximum = 10, adjustment = 1),
            energy = ResourceValue(current = 12, maximum = 14, adjustment = 0),
            destiny = ResourceValue(current = 3, maximum = 5, adjustment = 0),
            exhaustion = ResourceValue(current = 1, maximum = 10, adjustment = 0),
            corruption = ResourceValue(current = 5, maximum = 100, adjustment = 0),
            unrecognizedFieldsJson = """{"legacyExtra":"ok"}""",
            inventory = listOf(InventoryItem(id = "inv-1", name = "Lanterna", quantity = 1)),
            powers = listOf(Power(id = "pow-1", name = "Centelha")),
            mysticAbilities = listOf(MysticAbility(id = "mys-1", name = "Véu")),
            personalNotes = listOf(PersonalNote(id = "note-1", title = "Diário", text = "Entrada 1")),
            transports = listOf(com.kinderman.sdo.domain.model.CharacterTransport(id = "tr-1", name = "Cavalo")),
            familiars = listOf(com.kinderman.sdo.domain.model.CharacterFamiliar(id = "fam-1", name = "Corvo")),
        ).toRecord().normalizedForSparkV2()

        val docs = SparkWireCodec.toSparkDocuments(full, revision = 1L, lastOperationId = "op-create-1", updatedBy = "uid-player-1")

        // 1. Root document shape: exact 18 keys, normalized campaignId, protocolVersion=2, schemaVersion=2
        val expectedRootKeys = setOf(
            "id", "ownerId", "campaignId", "authorizedCampaignId", "deleted",
            "schemaVersion", "canonicalSchemaVersion", "itemSchemaVersion", "protocolVersion",
            "lockType", "isLocked", "lockedBy", "lockedAt",
            "revision", "lastOperationId", "updatedBy", "updatedAt", "appliedDeliveryIds",
        )
        assertEquals(expectedRootKeys, docs.root.keys)
        assertEquals("", docs.root["campaignId"])
        assertEquals(2, docs.root["schemaVersion"])
        assertEquals(2, docs.root["protocolVersion"])
        assertEquals("NONE", docs.root["lockType"])
        assertEquals(false, docs.root["isLocked"])
        assertNull(docs.root["lockedAt"])

        // 2. Field documents: exact 6 keys, every key in Rules allowlist, bounded types
        val allowedFieldKeys = setOf(
            "name", "race", "subRace", "raceAttribute", "occupation", "height", "age", "sex", "size",
            "avatarPreset", "pathName", "pathMotto", "story", "agilityLimit", "creationStatus",
            "itemCreationDraft", "migrationReviewPayload", "unrecognizedFieldsPayload", "canonicalBodyPayload",
            "activeModifiersPayload", "scopedItemCatalogPayload", "canonicalProgressionPayload",
            "transports", "familiars",
            "level", "creationStep", "creationRulesVersion", "creationCompletedAt",
            "money", "containerCapacity",
            "progressionLifeBonus", "progressionSanityBonus", "progressionArcaneBonus", "progressionEnergyBonus",
            "life", "sanity", "arcane", "energy", "destiny", "exhaustion", "corruption",
            "positiveTraits", "negativeTraits", "pathKeywords", "pathPillars",
            "learnedKnowledges", "arcaneKnowledges", "battleTechniques", "progressionHistory",
            "protectionAdjustments",
        )
        docs.fields.forEach { (fieldKey, doc) ->
            assertTrue("Field key '$fieldKey' is not allowlisted in firestore.rules", fieldKey in allowedFieldKeys)
            assertEquals(setOf("key", "value", "revision", "lastOperationId", "updatedBy", "updatedAt"), doc.keys)
            assertEquals(fieldKey, doc["key"])
            when (val v = doc["value"]) {
                is String -> assertTrue(v.length <= 250_000)
                is List<*> -> assertTrue(v.size <= 500)
                is Map<*, *> -> assertTrue(v.size <= 100)
            }
        }
        assertEquals("""{"legacyExtra":"ok"}""", docs.fields["unrecognizedFieldsPayload"]?.get("value"))

        // 3. Item documents & item field documents: exact shapes and valid prefixes
        val validPrefixes = listOf("inv_", "istate_", "abil_", "pow_", "mys_", "cond_", "lcond_", "note_", "attr_")
        docs.items.forEach { (itemId, bundle) ->
            assertTrue("Invalid itemId prefix: $itemId", validPrefixes.any { itemId.startsWith(it) })
            assertEquals(setOf("id", "kind", "deleted", "revision", "lastOperationId", "updatedBy", "updatedAt"), bundle.document.keys)
            bundle.fields.forEach { (fKey, fDoc) ->
                assertEquals(setOf("key", "value", "revision", "lastOperationId", "updatedBy", "updatedAt"), fDoc.keys)
                assertEquals(fKey, fDoc["key"])
            }
        }

        // 4. Media avatar shape matches Rules (dataUri <= 65536 bytes, mimeType, width/height in 1..256)
        val avatar = docs.mediaAvatar
        assertNotNull(avatar)
        assertEquals(setOf("dataUri", "mimeType", "width", "height", "updatedBy", "updatedAt"), avatar!!.keys)
        assertEquals("image/webp", avatar["mimeType"])
        assertTrue((avatar["width"] as Int) in 1..256)
        assertTrue((avatar["height"] as Int) in 1..256)
        assertTrue((avatar["dataUri"] as String).toByteArray(Charsets.UTF_8).size <= 65_536)
    }
}
