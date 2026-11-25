package com.kinderman.sdo.data.sync

import com.google.firebase.Timestamp
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kinderman.sdo.data.local.CharacterConverters
import com.kinderman.sdo.data.local.CharacterMigrationManager
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.BodyRegion
import com.kinderman.sdo.domain.model.CharacterCreationStatus
import com.kinderman.sdo.domain.model.CharacterFamiliar
import com.kinderman.sdo.domain.model.CharacterTransport
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemCreationDraft
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.OrganStatus
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.ProgressionRecord
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.defaultAttributes
import com.kinderman.sdo.domain.model.normalizeCampaignId

/**
 * Normalizes unmigrated legacy v1 monolithic Firestore character documents (`protocolVersion < 2`)
 * into canonical [CharacterRecord] and [SparkSheetDocuments] before consumption by the app.
 */
object LegacyFirestoreNormalizer {
    private val itemQualityDeserializer = JsonDeserializer { json, _, _ ->
        val raw = json?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
        ItemQuality.entries.firstOrNull {
            it.name.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true)
        } ?: ItemQuality.COMMON
    }
    private val gson = GsonBuilder()
        .registerTypeAdapter(ItemQuality::class.java, itemQualityDeserializer)
        .create()
    private val converters = CharacterConverters()
    private val migrationManager = CharacterMigrationManager()

    private val KNOWN_LEGACY_KEYS = setOf(
        "id", "ownerId", "campaignId", "authorizedCampaignId", "deleted",
        "schemaVersion", "canonicalSchemaVersion", "itemSchemaVersion", "protocolVersion",
        "revision", "lastOperationId", "updatedBy", "updatedAt", "lastEditedBy", "fieldUpdatedAt",
        "appliedDeliveryIds", "lockType", "isLocked", "lockedBy", "lockedAt",
        "name", "race", "subRace", "raceAttribute", "occupation", "height", "age", "sex", "size",
        "photoUri", "avatarPreset", "level", "creationStatus", "creationStep", "creationCompletedAt",
        "creationRulesVersion", "migrationReviewPayload", "unrecognizedFieldsPayload", "unrecognizedFieldsJson",
        "canonicalAbilitiesPayload", "canonicalBodyPayload", "canonicalConditionsPayload",
        "activeModifiersPayload", "canonicalItemsPayload", "scopedItemCatalogPayload",
        "canonicalProgressionPayload", "progressionLifeBonus", "progressionSanityBonus",
        "progressionArcaneBonus", "progressionEnergyBonus", "progressionHistory",
        "money", "life", "sanity", "arcane", "energy", "destiny", "exhaustion", "corruption",
        "attributes", "protections", "protectionAdjustments", "positiveTraits", "negativeTraits",
        "pathName", "pathMotto", "pathKeywords", "pathPillars", "powers", "inventory",
        "itemCreationDraft", "story", "notes", "transports", "familiars", "divineAscensionPayload",
        "divineRuntimePayload", "personalNotes",
        "learnedKnowledges", "arcaneKnowledges", "battleTechniques", "containerCapacity",
        "bodyRegions", "agilityLimit", "organs", "mysticAbilities", "conditions",
    )

    fun normalizeToRecord(sheetId: String, rawRoot: Map<String, Any?>): CharacterRecord {
        val unrecognized = linkedMapOf<String, Any?>()
        rawRoot.keys.sorted().forEach { key ->
            if (key !in KNOWN_LEGACY_KEYS) unrecognized[key] = rawRoot[key]
        }
        val existingUnrecognized = asString(rawRoot["unrecognizedFieldsPayload"])
            .ifBlank { asString(rawRoot["unrecognizedFieldsJson"]) }
        val unrecognizedJson = mergeUnrecognizedFields(existingUnrecognized, unrecognized)

        val lockTypeRaw = asString(rawRoot["lockType"]).ifBlank {
            if (asBoolean(rawRoot["isLocked"]) == true) "HISTORIAN" else "NONE"
        }
        val normalizedLockType = if (lockTypeRaw in setOf("NONE", "PLAYER", "HISTORIAN")) lockTypeRaw else "NONE"
        val isLocked = normalizedLockType != "NONE"

        val legacyNotes = asString(rawRoot["notes"])
        val parsedPersonalNotes = decodeTypedList<PersonalNote>(rawRoot["personalNotes"]).ifEmpty {
            if (legacyNotes.isNotBlank()) {
                listOf(PersonalNote(id = "legacy-$sheetId", title = "Registro Pessoal 1", text = legacyNotes))
            } else emptyList()
        }

        val record = CharacterRecord(
            id = asString(rawRoot["id"]).ifBlank { sheetId },
            ownerId = asString(rawRoot["ownerId"]),
            campaignId = normalizeCampaignId(asString(rawRoot["campaignId"])),
            name = asString(rawRoot["name"]).ifBlank { "Novo personagem" },
            race = asString(rawRoot["race"]),
            subRace = asString(rawRoot["subRace"]),
            raceAttribute = asString(rawRoot["raceAttribute"]),
            occupation = asString(rawRoot["occupation"]),
            age = asString(rawRoot["age"]),
            level = (asInt(rawRoot["level"]) ?: 1).coerceIn(1, 20),
            creationStatus = asString(rawRoot["creationStatus"]).ifBlank { CharacterCreationStatus.COMPLETED.name },
            creationStep = (asInt(rawRoot["creationStep"]) ?: 1).coerceIn(1, 20),
            creationCompletedAt = asLong(rawRoot["creationCompletedAt"]),
            creationRulesVersion = (asInt(rawRoot["creationRulesVersion"]) ?: 1).coerceAtLeast(1),
            itemSchemaVersion = asInt(rawRoot["itemSchemaVersion"]) ?: 0,
            canonicalSchemaVersion = asInt(rawRoot["canonicalSchemaVersion"]) ?: 0,
            migrationReviewPayload = asString(rawRoot["migrationReviewPayload"]),
            unrecognizedFieldsJson = unrecognizedJson,
            canonicalAbilitiesPayload = asString(rawRoot["canonicalAbilitiesPayload"]),
            canonicalBodyPayload = asString(rawRoot["canonicalBodyPayload"]),
            canonicalConditionsPayload = asString(rawRoot["canonicalConditionsPayload"]),
            activeModifiersPayload = asString(rawRoot["activeModifiersPayload"]),
            canonicalItemsPayload = asString(rawRoot["canonicalItemsPayload"]),
            scopedItemCatalogPayload = asString(rawRoot["scopedItemCatalogPayload"]),
            canonicalProgressionPayload = asString(rawRoot["canonicalProgressionPayload"]),
            progressionLifeBonus = (asInt(rawRoot["progressionLifeBonus"]) ?: 0).coerceAtLeast(0),
            progressionSanityBonus = (asInt(rawRoot["progressionSanityBonus"]) ?: 0).coerceAtLeast(0),
            progressionArcaneBonus = (asInt(rawRoot["progressionArcaneBonus"]) ?: 0).coerceAtLeast(0),
            progressionEnergyBonus = (asInt(rawRoot["progressionEnergyBonus"]) ?: 0).coerceAtLeast(0),
            progressionHistory = decodeTypedList(rawRoot["progressionHistory"]),
            money = (asInt(rawRoot["money"]) ?: 0).coerceAtLeast(0),
            life = decodeResource(rawRoot["life"]),
            sanity = decodeResource(rawRoot["sanity"]),
            arcane = decodeResource(rawRoot["arcane"]),
            energy = decodeResource(rawRoot["energy"]),
            destiny = decodeResource(rawRoot["destiny"], ResourceValue(5, 5)),
            exhaustion = decodeResource(rawRoot["exhaustion"], ResourceValue(0, 10)),
            corruption = decodeResource(rawRoot["corruption"], ResourceValue(0, 100)),
            attributes = decodeTypedList<AttributeValue>(rawRoot["attributes"]).ifEmpty { defaultAttributes() },
            protectionAdjustments = decodeIntMap(rawRoot["protectionAdjustments"]),
            positiveTraits = decodeStringList(rawRoot["positiveTraits"]),
            negativeTraits = decodeStringList(rawRoot["negativeTraits"]),
            pathName = asString(rawRoot["pathName"]),
            pathMotto = asString(rawRoot["pathMotto"]),
            powers = decodeTypedList<Power>(rawRoot["powers"]),
            inventory = decodeTypedList<InventoryItem>(rawRoot["inventory"]),
            itemCreationDraft = decodeItemCreationDraft(rawRoot["itemCreationDraft"]),
            story = asString(rawRoot["story"]),
            notes = "",
            transports = decodeTransports(rawRoot["transports"]),
            familiars = decodeFamiliars(rawRoot["familiars"]),
            divineAscensionPayload = asString(rawRoot["divineAscensionPayload"]),
            divineRuntimePayload = asString(rawRoot["divineRuntimePayload"]),
            personalNotes = parsedPersonalNotes,
            updatedAt = asLong(rawRoot["updatedAt"]) ?: 0L,
            lastEditedBy = asString(rawRoot["lastEditedBy"]).ifBlank { asString(rawRoot["updatedBy"]) },
            height = asString(rawRoot["height"]),
            sex = asString(rawRoot["sex"]),
            size = asString(rawRoot["size"]),
            photoUri = asString(rawRoot["photoUri"]),
            learnedKnowledges = decodeTypedList(rawRoot["learnedKnowledges"]),
            arcaneKnowledges = decodeTypedList(rawRoot["arcaneKnowledges"]),
            battleTechniques = decodeTypedList(rawRoot["battleTechniques"]),
            pathKeywords = decodeStringList(rawRoot["pathKeywords"]),
            pathPillars = decodeStringList(rawRoot["pathPillars"]),
            containerCapacity = (asInt(rawRoot["containerCapacity"]) ?: 0).coerceAtLeast(0),
            bodyRegions = decodeTypedList<BodyRegion>(rawRoot["bodyRegions"]),
            agilityLimit = asString(rawRoot["agilityLimit"]),
            organs = decodeTypedList<OrganStatus>(rawRoot["organs"]),
            mysticAbilities = decodeTypedList<MysticAbility>(rawRoot["mysticAbilities"]),
            conditions = decodeTypedList<ConditionEffect>(rawRoot["conditions"]),
            isLocked = isLocked,
            lockType = normalizedLockType,
            lockedBy = if (isLocked) asString(rawRoot["lockedBy"]) else "",
            lockedAt = if (isLocked) asLong(rawRoot["lockedAt"]) else null,
            deleted = asBoolean(rawRoot["deleted"]) ?: false,
            appliedDeliveryIds = decodeStringList(rawRoot["appliedDeliveryIds"]),
        )
        return migrationManager.migrate(record, markDirty = false)
    }

    fun normalizeToSparkDocuments(sheetId: String, rawRoot: Map<String, Any?>): SparkSheetDocuments {
        val migrated = normalizeToRecord(sheetId, rawRoot)
        val revision = (asLong(rawRoot["revision"]) ?: 1L).coerceAtLeast(1L)
        val lastOperationId = asString(rawRoot["lastOperationId"]).ifBlank { "legacy-normalized" }
        val updatedBy = asString(rawRoot["updatedBy"]).ifBlank { migrated.lastEditedBy.ifBlank { migrated.ownerId } }
        val updatedAt = asLong(rawRoot["updatedAt"]) ?: migrated.updatedAt
        val authorizedCampaignId = normalizeCampaignId(asString(rawRoot["authorizedCampaignId"]))
        return SparkWireCodec.toSparkDocuments(
            record = migrated,
            revision = revision,
            lastOperationId = lastOperationId,
            updatedBy = updatedBy,
            updatedAt = updatedAt,
            authorizedCampaignId = authorizedCampaignId,
        )
    }

    private fun mergeUnrecognizedFields(existingJson: String, extra: Map<String, Any?>): String {
        if (extra.isEmpty()) return existingJson
        val rootObj = runCatching {
            if (existingJson.isNotBlank()) JsonParser.parseString(existingJson).asJsonObject else JsonObject()
        }.getOrElse { JsonObject() }
        extra.forEach { (k, v) ->
            if (!rootObj.has(k)) {
                rootObj.add(k, gson.toJsonTree(v))
            }
        }
        val sorted = JsonObject()
        rootObj.keySet().sorted().forEach { k -> sorted.add(k, rootObj.get(k)) }
        return gson.toJson(sorted)
    }

    private fun asString(v: Any?): String = when (v) {
        null -> ""
        is String -> v
        is Number, is Boolean -> v.toString()
        else -> gson.toJson(v)
    }

    private fun asInt(v: Any?): Int? = when (v) {
        is Int -> v
        is Number -> v.toInt()
        is String -> v.trim().toDoubleOrNull()?.toInt()
        else -> null
    }

    private fun asLong(v: Any?): Long? = when (v) {
        is Long -> v
        is Number -> v.toLong()
        is Timestamp -> v.toDate().time
        is String -> v.trim().toLongOrNull() ?: runCatching { java.time.Instant.parse(v.trim()).toEpochMilli() }.getOrNull()
        else -> null
    }

    private fun asBoolean(v: Any?): Boolean? = when (v) {
        is Boolean -> v
        is String -> v.trim().lowercase().let {
            when (it) {
                "true", "1", "yes" -> true
                "false", "0", "no" -> false
                else -> null
            }
        }
        is Number -> v.toInt() != 0
        else -> null
    }

    private fun decodeResource(v: Any?, default: ResourceValue = ResourceValue()): ResourceValue {
        if (v == null) return default
        if (v is Number) {
            val n = v.toInt().coerceAtLeast(0)
            return ResourceValue(current = n, maximum = maxOf(n, default.maximum), adjustment = 0)
        }
        if (v is Map<*, *>) {
            val cur = (asInt(v["current"]) ?: default.current).coerceAtLeast(0)
            val max = (asInt(v["maximum"]) ?: default.maximum).coerceAtLeast(0)
            val adj = asInt(v["adjustment"]) ?: default.adjustment
            return ResourceValue(current = cur, maximum = max, adjustment = adj)
        }
        return runCatching { gson.fromJson(gson.toJsonTree(v), ResourceValue::class.java) }.getOrDefault(default)
    }

    private fun decodeIntMap(v: Any?): Map<String, Int> {
        val map = v as? Map<*, *> ?: return emptyMap()
        return map.entries.mapNotNull { (k, raw) ->
            val key = k?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val num = asInt(raw) ?: return@mapNotNull null
            key to num
        }.toMap()
    }

    private fun decodeStringList(v: Any?): List<String> = when (v) {
        is List<*> -> v.mapNotNull { it?.toString() }
        is String -> runCatching { gson.fromJson<List<String>>(v, object : TypeToken<List<String>>() {}.type) }.getOrDefault(emptyList())
        else -> emptyList()
    }

    private inline fun <reified T> decodeTypedList(v: Any?): List<T> {
        val type = object : TypeToken<List<T>>() {}.type
        return when (v) {
            is List<*> -> runCatching { gson.fromJson<List<T>>(gson.toJsonTree(v), type) }.getOrDefault(emptyList())
            is String -> if (v.isBlank()) emptyList() else runCatching { gson.fromJson<List<T>>(v, type) }.getOrDefault(emptyList())
            else -> emptyList()
        }
    }

    private fun decodeItemCreationDraft(v: Any?): ItemCreationDraft? = when (v) {
        null -> null
        is String -> converters.stringToItemCreationDraft(v)
        else -> runCatching { gson.fromJson(gson.toJsonTree(v), ItemCreationDraft::class.java) }.getOrNull()
    }

    private fun decodeTransports(v: Any?): List<CharacterTransport> = when (v) {
        is String -> converters.stringToTransports(v)
        is List<*> -> decodeTypedList(v)
        else -> emptyList()
    }

    private fun decodeFamiliars(v: Any?): List<CharacterFamiliar> = when (v) {
        is String -> converters.stringToFamiliars(v)
        is List<*> -> decodeTypedList(v)
        else -> emptyList()
    }
}
