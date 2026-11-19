package com.kinderman.sdo.data.sync

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.catalog.BuiltInCatalog
import com.kinderman.sdo.domain.model.CatalogEntry
import com.kinderman.sdo.domain.model.CatalogKind
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterCreationStatus
import com.kinderman.sdo.domain.model.SpecialKnowledge
import java.io.File
import java.text.Normalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionSheetMigrationRunnerTest {

    private val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

    @Suppress("UNCHECKED_CAST")
    private fun normalizeNumbers(value: Any?): Any? = when (value) {
        is Double -> if (value % 1.0 == 0.0) value.toLong() else value
        is Map<*, *> -> value.entries.associate { (k, v) -> k.toString() to normalizeNumbers(v) }
        is List<*> -> value.map(::normalizeNumbers)
        else -> value
    }

    private fun slug(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')

    private fun refreshKnowledge(entry: SpecialKnowledge, kind: CatalogKind, catalog: List<CatalogEntry>): SpecialKnowledge {
        val cleanName = entry.name.trim()
        val match = catalog.firstOrNull {
            it.kind == kind && (
                (entry.catalogEntryId.isNotBlank() && it.id == entry.catalogEntryId) ||
                    slug(it.name) == slug(cleanName)
                )
        }
        return if (match != null) {
            entry.copy(
                name = match.name,
                catalogEntryId = match.id,
                catalogVersion = match.version,
                attribute = match.relatedAttribute.ifBlank { entry.attribute },
                category = match.group.ifBlank { entry.category },
                description = match.summary,
                mechanicalEffect = match.mechanicalEffect.ifBlank { match.summary },
                ruleReference = match.ruleReference,
                source = match.source,
                keywords = match.keywords,
                prerequisites = match.prerequisites,
                repeatable = match.repeatable,
            )
        } else {
            entry.copy(name = cleanName)
        }
    }

    private fun migrateCharacterDomain(raw: Character, catalog: List<CatalogEntry>): Character {
        val cleanedInventory = raw.inventory.map { item ->
            val cleanCat = when {
                item.category.equals("LEGACY_NARRATIVE", ignoreCase = true) ->
                    if (item.name.contains("Roupa", ignoreCase = true)) "Acessório" else "Item"
                item.category.isBlank() -> "Item"
                else -> item.category.trim()
            }
            val cleanQty = if (item.quantity <= 0) 1 else item.quantity
            item.copy(
                name = item.name.trim(),
                category = cleanCat,
                quantity = cleanQty,
            )
        }

        val cleanedLearned = raw.learnedKnowledges.map { refreshKnowledge(it, CatalogKind.ACQUIRED_KNOWLEDGE, catalog) }
        val cleanedArcane = raw.arcaneKnowledges.map { refreshKnowledge(it, CatalogKind.ARCANE_KNOWLEDGE, catalog) }
        val cleanedMartial = raw.battleTechniques
            .filterNot { it.name.trim().equals("meow", ignoreCase = true) && it.catalogEntryId.isBlank() }
            .map { refreshKnowledge(it, CatalogKind.BATTLE_TECHNIQUE, catalog) }

        val cleanedPowers = raw.powers.map { power ->
            val match = catalog.firstOrNull {
                it.kind == CatalogKind.POWER && (
                    (power.catalogEntryId.isNotBlank() && it.id == power.catalogEntryId) ||
                        slug(it.name) == slug(power.name)
                    )
            }
            if (match != null) {
                power.copy(
                    name = match.name,
                    catalogEntryId = match.id,
                    catalogVersion = match.version,
                    effect = match.summary.ifBlank { power.effect },
                    ruleReference = match.ruleReference.ifBlank { power.ruleReference },
                )
            } else {
                power.copy(name = power.name.trim())
            }
        }

        val fixedCreationStep = if (raw.creationStatus == CharacterCreationStatus.COMPLETED) 8 else raw.creationStep
        val fixedCompletedAt = if (raw.creationStatus == CharacterCreationStatus.COMPLETED) {
            raw.creationCompletedAt ?: 1789000000000L
        } else {
            raw.creationCompletedAt
        }

        val preCanonical = raw.copy(
            name = raw.name.trim(),
            inventory = cleanedInventory,
            learnedKnowledges = cleanedLearned,
            arcaneKnowledges = cleanedArcane,
            battleTechniques = cleanedMartial,
            powers = cleanedPowers,
            creationStep = fixedCreationStep,
            creationCompletedAt = fixedCompletedAt,
        ).toRecord().toDomain()

        // Ensure derived resource ceilings are populated and Eloah Fjorn (all-zero currents) starts at full resources.
        val isAllZeroResources = preCanonical.life.current == 0 &&
            preCanonical.energy.current == 0 &&
            preCanonical.arcane.current == 0 &&
            preCanonical.sanity.current == 0

        val lifeMax = preCanonical.life.maximum.coerceAtLeast(preCanonical.life.current).coerceAtLeast(10)
        val sanityMax = preCanonical.sanity.maximum.coerceAtLeast(preCanonical.sanity.current).coerceAtLeast(10)
        val energyMax = preCanonical.energy.maximum.coerceAtLeast(preCanonical.energy.current)
        val arcaneMax = preCanonical.arcane.maximum.coerceAtLeast(preCanonical.arcane.current)
        val destinyMax = preCanonical.destiny.maximum.coerceAtLeast(preCanonical.destiny.current).coerceAtLeast(1)
        val exhaustionMax = preCanonical.exhaustion.maximum.coerceAtLeast(10)
        val corruptionMax = preCanonical.corruption.maximum.coerceAtLeast(100)

        return preCanonical.copy(
            life = preCanonical.life.copy(
                current = if (isAllZeroResources) lifeMax else preCanonical.life.current,
                maximum = lifeMax,
            ),
            sanity = preCanonical.sanity.copy(
                current = if (isAllZeroResources) sanityMax else preCanonical.sanity.current,
                maximum = sanityMax,
            ),
            energy = preCanonical.energy.copy(
                current = if (isAllZeroResources) energyMax else preCanonical.energy.current,
                maximum = energyMax,
            ),
            arcane = preCanonical.arcane.copy(
                current = if (isAllZeroResources) arcaneMax else preCanonical.arcane.current,
                maximum = arcaneMax,
            ),
            destiny = preCanonical.destiny.copy(
                current = if (isAllZeroResources) 1 else preCanonical.destiny.current,
                maximum = destinyMax,
            ),
            exhaustion = preCanonical.exhaustion.copy(maximum = exhaustionMax),
            corruption = preCanonical.corruption.copy(maximum = corruptionMax),
        ).toRecord().toDomain()
    }

    @Test
    fun buildMigratedProductionSheetsBundle() {
        val dumpFile = File("C:/Users/kindb/.gemini/antigravity-cli/brain/5750635f-f024-4082-b957-8060afde0431/scratch/prod_sheets_dump.json")
        if (!dumpFile.exists()) return
        val catalog = BuiltInCatalog.entries
        val array = JsonParser.parseString(dumpFile.readText(Charsets.UTF_8)).asJsonArray
        val mapType = object : TypeToken<Map<String, Any?>>() {}.type

        val migratedList = mutableListOf<Map<String, Any?>>()

        for (elem in array) {
            val obj = elem.asJsonObject
            val id = obj.get("id").asString
            @Suppress("UNCHECKED_CAST")
            val rootMap = normalizeNumbers(gson.fromJson<Map<String, Any?>>(obj.get("root"), mapType)) as Map<String, Any?>
            val deleted = (rootMap["deleted"] as? Boolean) == true
            if (deleted) continue

            @Suppress("UNCHECKED_CAST")
            val fieldsMap = normalizeNumbers(
                gson.fromJson<Map<String, Map<String, Any?>>>(obj.get("fields"), object : TypeToken<Map<String, Map<String, Any?>>>() {}.type),
            ) as Map<String, Map<String, Any?>>
            @Suppress("UNCHECKED_CAST")
            val rawItemsMap = normalizeNumbers(
                gson.fromJson<Map<String, Map<String, Any?>>>(obj.get("items"), object : TypeToken<Map<String, Map<String, Any?>>>() {}.type),
            ) as Map<String, Map<String, Any?>>

            val itemBundles = linkedMapOf<String, SparkItemBundle>()
            val fieldRevisions = mutableMapOf<String, Long>()
            fieldsMap.forEach { (k, doc) ->
                val rev = (doc["revision"] as? Number)?.toLong() ?: 1L
                fieldRevisions["field:$k"] = rev
            }
            rawItemsMap.forEach { (itemId, bundleMap) ->
                @Suppress("UNCHECKED_CAST")
                val doc = bundleMap["document"] as? Map<String, Any?> ?: emptyMap()
                @Suppress("UNCHECKED_CAST")
                val iFields = bundleMap["fields"] as? Map<String, Map<String, Any?>> ?: emptyMap()
                val itemRev = (doc["revision"] as? Number)?.toLong() ?: 1L
                fieldRevisions["item:$itemId"] = itemRev
                iFields.forEach { (fk, fDoc) ->
                    val fRev = (fDoc["revision"] as? Number)?.toLong() ?: 1L
                    fieldRevisions["item:$itemId/field:$fk"] = fRev
                }
                itemBundles[itemId] = SparkItemBundle(document = doc, fields = iFields)
            }

            val mediaAvatar: Map<String, Any?>? = if (obj.has("mediaAvatar") && !obj.get("mediaAvatar").isJsonNull) {
                gson.fromJson(obj.get("mediaAvatar"), mapType)
            } else null

            val sparkDocs = SparkSheetDocuments(
                root = rootMap,
                fields = fieldsMap,
                items = itemBundles,
                mediaAvatar = mediaAvatar,
            )

            val decodedRecord = SparkWireCodec.fromSparkDocuments(sparkDocs)
            val decodedDomain = decodedRecord.toDomain()
            val migratedDomain = migrateCharacterDomain(decodedDomain, catalog)
            val currentRootRev = ((rootMap["revision"] as? Number)?.toLong() ?: 1L)
            val nextRootRev = currentRootRev + 1L
            val opId = "mig_v2_norm_${id.take(8)}"
            val actorUid = (rootMap["ownerId"] as? String)?.ifBlank { "admin" } ?: "admin"
            val nowMs = System.currentTimeMillis()

            val targetDocs = SparkWireCodec.toSparkDocuments(
                record = migratedDomain.toRecord(),
                revision = nextRootRev,
                fieldRevisions = fieldRevisions,
                lastOperationId = opId,
                updatedBy = actorUid,
                updatedAt = nowMs,
                authorizedCampaignId = (rootMap["authorizedCampaignId"] as? String).orEmpty(),
            )

            // Verify that re-decoding targetDocs and diffing against itself yields ZERO pending batches!
            val roundTripRecord = SparkWireCodec.fromSparkDocuments(targetDocs)
            val postBatches = SparkWireCodec.buildOperationBatches(
                before = roundTripRecord,
                after = roundTripRecord.toDomain().toRecord(),
                operationIdPrefix = "verify-zero-diff",
                actorUid = actorUid,
                existingRevisions = fieldRevisions,
                sheetRevision = nextRootRev,
            )
            assertEquals("Expected zero phantom diff after migration for ${migratedDomain.name}", 0, postBatches.size)
            assertTrue("Expected lifeMaximum > 0 for ${migratedDomain.name}", migratedDomain.lifeMaximum > 0)

            val obsoleteItemIds = rawItemsMap.keys.filter { it !in targetDocs.items }

            migratedList += linkedMapOf(
                "id" to id,
                "name" to migratedDomain.name,
                "root" to targetDocs.root,
                "fields" to targetDocs.fields,
                "items" to targetDocs.items.mapValues { (_, b) ->
                    linkedMapOf("document" to b.document, "fields" to b.fields)
                },
                "obsoleteItemIds" to obsoleteItemIds,
            )
        }

        val outFile = File("C:/Users/kindb/.gemini/antigravity-cli/brain/5750635f-f024-4082-b957-8060afde0431/scratch/prod_sheets_migrated.json")
        outFile.writeText(gson.toJson(migratedList), Charsets.UTF_8)
        assertEquals(10, migratedList.size)
    }
}
