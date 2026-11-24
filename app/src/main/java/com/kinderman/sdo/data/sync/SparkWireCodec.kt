package com.kinderman.sdo.data.sync

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kinderman.sdo.data.local.CharacterConverters
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.normalizedForSparkV2
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AbilityCostType
import com.kinderman.sdo.domain.model.AshPurity
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.ConditionInstance
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemCondition
import com.kinderman.sdo.domain.model.ItemQuality
import com.kinderman.sdo.domain.model.ItemState
import com.kinderman.sdo.domain.model.MAX_SPARK_ITEM_FIELD_TARGETS_PER_OPERATION
import com.kinderman.sdo.domain.model.MAX_SPARK_TARGETS_PER_OPERATION
import com.kinderman.sdo.domain.model.MysticAbility
import com.kinderman.sdo.domain.model.OperationType
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.ProgressionRecord
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SkillValue
import com.kinderman.sdo.domain.model.SparkTargetSpec
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.TargetKind
import com.kinderman.sdo.domain.model.TargetOp
import com.kinderman.sdo.domain.model.computeCanonicalPayloadHash
import com.kinderman.sdo.domain.model.decodeWireElementId
import com.kinderman.sdo.domain.model.defaultAttributes
import com.kinderman.sdo.domain.model.encodeSkillSlug
import com.kinderman.sdo.domain.model.encodeWireElementId
import com.kinderman.sdo.domain.model.isWithinSparkTransactionBudget
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.domain.model.toLegacyEffect

data class SparkItemBundle(
    val document: Map<String, Any?>,
    val fields: Map<String, Map<String, Any?>>,
)

data class SparkSheetDocuments(
    val root: Map<String, Any?>,
    val fields: Map<String, Map<String, Any?>>,
    val items: Map<String, SparkItemBundle>,
    val mediaAvatar: Map<String, Any?>? = null,
    val localPhotoUri: String = "",
)

data class SparkTargetWrite(
    val target: SparkTargetSpec,
    val documentData: Map<String, Any?>,
    val isMedia: Boolean = false,
)

data class SparkOperationBatch(
    val operationId: String,
    val type: OperationType,
    val targets: List<SparkTargetSpec>,
    val writes: List<SparkTargetWrite>,
    val payloadHash: String,
) {
    init {
        require(isWithinSparkTransactionBudget(targets)) {
            "Spark operation batch exceeds budget (targets=${targets.size}, " +
                "max=$MAX_SPARK_TARGETS_PER_OPERATION or $MAX_SPARK_ITEM_FIELD_TARGETS_PER_OPERATION with ITEM_FIELD)."
        }
    }
}

/**
 * Lossless Spark v2 wire codec converting [CharacterRecord] to/from Firestore protocol v2 documents
 * (characters root, fields, items, granular item fields, and media/avatar) and chunking
 * mutations into bounded batches within Spark's Rules access budget.
 */
object SparkWireCodec {
    const val PROTOCOL_VERSION = 2
    const val SCHEMA_VERSION = 2
    const val MAX_AVATAR_THUMBNAIL_BYTES = 65_536
    const val MAX_AVATAR_DIMENSION_PX = 256

    private val gson = GsonBuilder().serializeNulls().create()
    private val itemQualityDeserializer = JsonDeserializer { json, _, _ ->
        val raw = json?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
        ItemQuality.entries.firstOrNull {
            it.name.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true)
        } ?: ItemQuality.COMMON
    }
    private val plainGson = GsonBuilder()
        .registerTypeAdapter(ItemQuality::class.java, itemQualityDeserializer)
        .create()
    private val converters = CharacterConverters()
    private val canonicalJson = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toSparkDocuments(
        record: CharacterRecord,
        revision: Long = 1L,
        fieldRevisions: Map<String, Long> = emptyMap(),
        lastOperationId: String = "seed",
        updatedBy: String = record.lastEditedBy.ifBlank { record.ownerId },
        updatedAt: Long = record.updatedAt,
        authorizedCampaignId: String = "",
    ): SparkSheetDocuments {
        val normalized = record.normalizedForSparkV2()
        val root = linkedMapOf<String, Any?>(
            "id" to normalized.id,
            "ownerId" to normalized.ownerId,
            "campaignId" to normalizeCampaignId(normalized.campaignId),
            "authorizedCampaignId" to normalizeCampaignId(authorizedCampaignId),
            "deleted" to normalized.deleted,
            "schemaVersion" to SCHEMA_VERSION,
            "canonicalSchemaVersion" to normalized.canonicalSchemaVersion,
            "itemSchemaVersion" to normalized.itemSchemaVersion,
            "protocolVersion" to PROTOCOL_VERSION,
            "lockType" to normalized.lockType,
            "isLocked" to normalized.isLocked,
            "lockedBy" to normalized.lockedBy,
            "lockedAt" to normalized.lockedAt,
            "revision" to revision.coerceAtLeast(1L),
            "lastOperationId" to lastOperationId,
            "updatedBy" to updatedBy,
            "updatedAt" to updatedAt,
            "appliedDeliveryIds" to normalized.appliedDeliveryIds,
        )

        val (avatarPreset, mediaAvatar, localPhotoUri) = encodeAvatar(
            photoUri = normalized.photoUri,
            updatedBy = updatedBy,
            updatedAt = updatedAt,
        )

        fun fieldDoc(key: String, value: Any?): Map<String, Any?> {
            val rev = (fieldRevisions["field:$key"] ?: fieldRevisions[key] ?: revision).coerceAtLeast(1L)
            return linkedMapOf(
                "key" to key,
                "value" to value,
                "revision" to rev,
                "lastOperationId" to lastOperationId,
                "updatedBy" to updatedBy,
                "updatedAt" to updatedAt,
            )
        }

        val fields = linkedMapOf<String, Map<String, Any?>>()
        fun putField(key: String, value: Any?) {
            fields[key] = fieldDoc(key, value)
        }

        // Strings
        putField("name", normalized.name)
        putField("race", normalized.race)
        putField("subRace", normalized.subRace)
        putField("raceAttribute", normalized.raceAttribute)
        putField("occupation", normalized.occupation)
        putField("height", normalized.height)
        putField("age", normalized.age)
        putField("sex", normalized.sex)
        putField("size", normalized.size)
        putField("avatarPreset", avatarPreset)
        putField("pathName", normalized.pathName)
        putField("pathMotto", normalized.pathMotto)
        putField("story", normalized.story)
        putField("agilityLimit", normalized.agilityLimit)
        putField("creationStatus", normalized.creationStatus)
        putField("itemCreationDraft", converters.itemCreationDraftToString(normalized.itemCreationDraft).orEmpty())
        putField("migrationReviewPayload", normalized.migrationReviewPayload)
        putField("unrecognizedFieldsPayload", normalized.unrecognizedFieldsJson)
        putField("canonicalBodyPayload", normalized.canonicalBodyPayload)
        putField("activeModifiersPayload", normalized.activeModifiersPayload)
        putField("scopedItemCatalogPayload", normalized.scopedItemCatalogPayload)
        putField("canonicalProgressionPayload", normalized.canonicalProgressionPayload)
        putField("transports", converters.transportsToString(normalized.transports))
        putField("familiars", converters.familiarsToString(normalized.familiars))
        putField("divineAscensionPayload", normalized.divineAscensionPayload)
        putField("divineRuntimePayload", normalized.divineRuntimePayload)

        // Ints
        putField("level", normalized.level.coerceIn(1, 20))
        putField("money", normalized.money.coerceAtLeast(0))
        putField("creationStep", normalized.creationStep.coerceIn(1, 20))
        putField("creationRulesVersion", normalized.creationRulesVersion.coerceAtLeast(1))
        normalized.creationCompletedAt?.let { putField("creationCompletedAt", it.coerceAtLeast(0L)) }
        putField("containerCapacity", normalized.containerCapacity.coerceAtLeast(0))
        putField("progressionLifeBonus", normalized.progressionLifeBonus.coerceAtLeast(0))
        putField("progressionSanityBonus", normalized.progressionSanityBonus.coerceAtLeast(0))
        putField("progressionArcaneBonus", normalized.progressionArcaneBonus.coerceAtLeast(0))
        putField("progressionEnergyBonus", normalized.progressionEnergyBonus.coerceAtLeast(0))

        // Resource maps
        putField("life", resourceMap(normalized.life))
        putField("sanity", resourceMap(normalized.sanity))
        putField("arcane", resourceMap(normalized.arcane))
        putField("energy", resourceMap(normalized.energy))
        putField("destiny", resourceMap(normalized.destiny))
        putField("exhaustion", resourceMap(normalized.exhaustion))
        putField("corruption", resourceMap(normalized.corruption))

        // Lists & Maps
        putField("positiveTraits", normalized.positiveTraits.take(500))
        putField("negativeTraits", normalized.negativeTraits.take(500))
        putField("pathKeywords", normalized.pathKeywords.take(500))
        putField("pathPillars", normalized.pathPillars.take(500))
        putField("learnedKnowledges", toPlainList(normalized.learnedKnowledges).take(500))
        putField("arcaneKnowledges", toPlainList(normalized.arcaneKnowledges).take(500))
        putField("battleTechniques", toPlainList(normalized.battleTechniques).take(500))
        putField("progressionHistory", toPlainList(normalized.progressionHistory).take(500))
        putField("protectionAdjustments", normalized.protectionAdjustments)

        val items = linkedMapOf<String, SparkItemBundle>()
        fun itemFieldDoc(itemId: String, fieldKey: String, value: Any?): Map<String, Any?> {
            val rev = (fieldRevisions["item:$itemId/field:$fieldKey"] ?: revision).coerceAtLeast(1L)
            return linkedMapOf(
                "key" to fieldKey,
                "value" to value,
                "revision" to rev,
                "lastOperationId" to lastOperationId,
                "updatedBy" to updatedBy,
                "updatedAt" to updatedAt,
            )
        }

        fun itemDoc(itemId: String, kind: String): Map<String, Any?> {
            val rev = (fieldRevisions["item:$itemId"] ?: revision).coerceAtLeast(1L)
            return linkedMapOf(
                "id" to itemId,
                "kind" to kind,
                "deleted" to false,
                "revision" to rev,
                "lastOperationId" to lastOperationId,
                "updatedBy" to updatedBy,
                "updatedAt" to updatedAt,
            )
        }

        normalized.inventory.forEach { item ->
            val itemId = encodeWireElementId("inv", item.id)
            val plain = toPlainMap(item)
            val enhancementsMap = linkedMapOf<String, Any?>(
                "installedEnhancements" to plain["installedEnhancements"],
                "enhancementSlots" to item.enhancementSlots,
                "enhancementChargesCurrent" to item.enhancementChargesCurrent,
                "enhancementChargesMax" to item.enhancementChargesMax,
            )
            val metaMap = linkedMapOf<String, Any?>(
                "id" to item.id,
                "catalogEntryId" to item.catalogEntryId,
                "catalogVersion" to item.catalogVersion,
                "baseId" to item.baseId,
                "materialId" to item.materialId,
                "secondaryMaterialId" to item.secondaryMaterialId,
                "modificationIds" to plain["modificationIds"],
                "gemIds" to plain["gemIds"],
                "technologyIds" to plain["technologyIds"],
                "gemSlots" to item.gemSlots,
                "technologySlots" to item.technologySlots,
                "acquisitionSource" to item.acquisitionSource.name,
                "heritageCost" to item.heritageCost,
                "purchasePrice" to item.purchasePrice,
                "acquiredAt" to item.acquiredAt,
                "canonical" to item.canonical,
                "dataVersion" to item.dataVersion,
            )
            items[itemId] = SparkItemBundle(
                document = itemDoc(itemId, "INVENTORY"),
                fields = linkedMapOf(
                    "name" to itemFieldDoc(itemId, "name", item.name.take(2000)),
                    "state" to itemFieldDoc(itemId, "state", item.state.take(2000)),
                    "quantity" to itemFieldDoc(itemId, "quantity", item.quantity.coerceAtLeast(0)),
                    "durabilityCurrent" to itemFieldDoc(itemId, "durabilityCurrent", item.durabilityCurrent.coerceAtLeast(0)),
                    "durabilityMax" to itemFieldDoc(itemId, "durabilityMax", item.durabilityMax.coerceAtLeast(0)),
                    "itemCondition" to itemFieldDoc(itemId, "itemCondition", item.itemCondition.name),
                    "region" to itemFieldDoc(itemId, "region", item.region.take(2000)),
                    "effect" to itemFieldDoc(itemId, "effect", item.effect.take(2000)),
                    "load" to itemFieldDoc(itemId, "load", item.load.coerceAtLeast(0)),
                    "backpackCapacity" to itemFieldDoc(itemId, "backpackCapacity", item.backpackCapacity.coerceAtLeast(0)),
                    "agilityLimit" to itemFieldDoc(itemId, "agilityLimit", item.agilityLimit?.coerceAtLeast(0)),
                    "quality" to itemFieldDoc(itemId, "quality", item.quality.name),
                    "pg" to itemFieldDoc(itemId, "pg", item.pg.coerceAtLeast(0)),
                    "pl" to itemFieldDoc(itemId, "pl", item.pl.coerceAtLeast(0)),
                    "category" to itemFieldDoc(itemId, "category", item.category.take(2000)),
                    "linkedAshId" to itemFieldDoc(itemId, "linkedAshId", item.linkedAshId.take(2000)),
                    "ashPurity" to itemFieldDoc(itemId, "ashPurity", item.ashPurity.name),
                    "enhancements" to itemFieldDoc(itemId, "enhancements", enhancementsMap),
                    "mechanicalEffects" to itemFieldDoc(
                        itemId,
                        "mechanicalEffects",
                        (plain["mechanicalEffects"] as? List<*> ?: emptyList<Any?>()).take(100),
                    ),
                    "meta" to itemFieldDoc(itemId, "meta", metaMap),
                ),
            )
        }

        if (normalized.canonicalItemsPayload.isNotBlank()) {
            converters.stringToCanonicalItems(normalized.canonicalItemsPayload).forEach { state ->
                val itemId = encodeWireElementId("istate", state.id.value)
                val encoded = JsonParser.parseString(
                    canonicalJson.encodeToString(ItemState.serializer(), state),
                ).asJsonObject
                items[itemId] = SparkItemBundle(
                    document = itemDoc(itemId, "ITEM_STATE"),
                    fields = linkedMapOf(
                        "definition" to itemFieldDoc(itemId, "definition", plain(encoded.get("definition"))),
                        "scope" to itemFieldDoc(itemId, "scope", plain(encoded.get("scope"))),
                        "material" to itemFieldDoc(itemId, "material", plain(encoded.get("material"))),
                        "installations" to itemFieldDoc(
                            itemId,
                            "installations",
                            (plain(encoded.get("installations")) as? List<*> ?: emptyList<Any?>()).take(50),
                        ),
                        "stack" to itemFieldDoc(itemId, "stack", plain(encoded.get("stack")) ?: 0),
                        "consumable" to itemFieldDoc(itemId, "consumable", plain(encoded.get("consumable"))),
                        "ammo" to itemFieldDoc(itemId, "ammo", plain(encoded.get("ammo"))),
                        "reload" to itemFieldDoc(itemId, "reload", plain(encoded.get("reload"))),
                        "container" to itemFieldDoc(itemId, "container", plain(encoded.get("container"))),
                    ),
                )
            }
        }

        if (normalized.canonicalAbilitiesPayload.isNotBlank()) {
            converters.stringToCanonicalAbilities(normalized.canonicalAbilitiesPayload).forEach { ability ->
                val itemId = encodeWireElementId("abil", ability.id)
                val rawMap = objectMap(
                    JsonParser.parseString(canonicalJson.encodeToString(Ability.serializer(), ability)).asJsonObject,
                )
                val metaMap = rawMap.filterKeys {
                    it !in setOf(
                        "name", "cost", "execution", "range", "duration", "effect",
                        "favorite", "available", "mechanicalEffect",
                    )
                }.toMutableMap()
                metaMap["id"] = ability.id
                metaMap["mechanicalEffect"] = rawMap["mechanicalEffect"]
                items[itemId] = SparkItemBundle(
                    document = itemDoc(itemId, "ABILITY"),
                    fields = linkedMapOf(
                        "name" to itemFieldDoc(itemId, "name", ability.name.take(2000)),
                        "cost" to itemFieldDoc(itemId, "cost", rawMap["cost"] ?: ""),
                        "costType" to itemFieldDoc(itemId, "costType", ability.cost::class.simpleName.orEmpty().take(2000)),
                        "costValue" to itemFieldDoc(itemId, "costValue", ability.cost.amount.coerceAtLeast(0)),
                        "action" to itemFieldDoc(itemId, "action", rawMap["execution"] ?: ""),
                        "range" to itemFieldDoc(itemId, "range", rawMap["range"] ?: ""),
                        "duration" to itemFieldDoc(itemId, "duration", rawMap["duration"]),
                        "effect" to itemFieldDoc(itemId, "effect", ability.effect.take(2000)),
                        "active" to itemFieldDoc(itemId, "active", false),
                        "favorite" to itemFieldDoc(itemId, "favorite", ability.favorite),
                        "available" to itemFieldDoc(itemId, "available", ability.available),
                        "modifiers" to itemFieldDoc(itemId, "modifiers", rawMap["mechanicalEffect"]),
                        "linkedItemId" to itemFieldDoc(itemId, "linkedItemId", ability.ashData?.linkedItemId?.value.orEmpty().take(2000)),
                        "meta" to itemFieldDoc(itemId, "meta", metaMap),
                    ),
                )
            }
        }

        normalized.powers.forEach { power ->
            val itemId = encodeWireElementId("pow", power.id)
            val plain = toPlainMap(power)
            val extractedKeys = setOf(
                "name", "cost", "costType", "costValue", "action", "range", "duration",
                "effect", "active", "favorite", "available", "modifiers", "linkedItemId",
            )
            val metaMap = plain.filterKeys { it !in extractedKeys }
            items[itemId] = SparkItemBundle(
                document = itemDoc(itemId, "POWER"),
                fields = linkedMapOf(
                    "name" to itemFieldDoc(itemId, "name", power.name.take(2000)),
                    "cost" to itemFieldDoc(itemId, "cost", power.cost.take(2000)),
                    "costType" to itemFieldDoc(itemId, "costType", power.costType.name),
                    "costValue" to itemFieldDoc(itemId, "costValue", power.costValue.coerceAtLeast(0)),
                    "action" to itemFieldDoc(itemId, "action", power.action.take(2000)),
                    "range" to itemFieldDoc(itemId, "range", power.range.take(2000)),
                    "duration" to itemFieldDoc(itemId, "duration", power.duration.take(2000)),
                    "effect" to itemFieldDoc(itemId, "effect", power.effect.take(2000)),
                    "active" to itemFieldDoc(itemId, "active", power.active),
                    "favorite" to itemFieldDoc(itemId, "favorite", power.favorite),
                    "available" to itemFieldDoc(itemId, "available", power.available),
                    "modifiers" to itemFieldDoc(itemId, "modifiers", (plain["modifiers"] as? List<*>)?.take(100)),
                    "linkedItemId" to itemFieldDoc(itemId, "linkedItemId", power.linkedItemId.take(2000)),
                    "meta" to itemFieldDoc(itemId, "meta", metaMap),
                ),
            )
        }

        normalized.mysticAbilities.forEach { mystic ->
            val itemId = encodeWireElementId("mys", mystic.id)
            val plain = toPlainMap(mystic)
            val extractedKeys = setOf(
                "name", "cost", "costType", "costValue", "action", "range", "duration",
                "effect", "favorite", "available", "linkedInventoryItemId",
            )
            val metaMap = plain.filterKeys { it !in extractedKeys }
            items[itemId] = SparkItemBundle(
                document = itemDoc(itemId, "MYSTIC"),
                fields = linkedMapOf(
                    "name" to itemFieldDoc(itemId, "name", mystic.name.take(2000)),
                    "cost" to itemFieldDoc(itemId, "cost", mystic.cost.take(2000)),
                    "costType" to itemFieldDoc(itemId, "costType", mystic.costType.name),
                    "costValue" to itemFieldDoc(itemId, "costValue", mystic.costValue.coerceAtLeast(0)),
                    "action" to itemFieldDoc(itemId, "action", mystic.action.take(2000)),
                    "range" to itemFieldDoc(itemId, "range", mystic.range.take(2000)),
                    "duration" to itemFieldDoc(itemId, "duration", mystic.duration.take(2000)),
                    "effect" to itemFieldDoc(itemId, "effect", mystic.effect.take(2000)),
                    "active" to itemFieldDoc(itemId, "active", false),
                    "favorite" to itemFieldDoc(itemId, "favorite", mystic.favorite),
                    "available" to itemFieldDoc(itemId, "available", mystic.available),
                    "modifiers" to itemFieldDoc(itemId, "modifiers", emptyList<Any?>()),
                    "linkedItemId" to itemFieldDoc(itemId, "linkedItemId", mystic.linkedInventoryItemId.take(2000)),
                    "meta" to itemFieldDoc(itemId, "meta", metaMap),
                ),
            )
        }

        val conditionSummaryById = normalized.conditions.associate { it.id to it.summary }
        val canonicalConditionIds = linkedSetOf<String>()
        if (normalized.canonicalConditionsPayload.isNotBlank()) {
            converters.stringToCanonicalConditions(normalized.canonicalConditionsPayload).forEach { cond ->
                canonicalConditionIds += cond.instanceId.value
                val itemId = encodeWireElementId("cond", cond.instanceId.value)
                val fullMap = objectMap(
                    JsonParser.parseString(canonicalJson.encodeToString(ConditionInstance.serializer(), cond)).asJsonObject,
                )
                val customSummary = conditionSummaryById[cond.instanceId.value].orEmpty().take(20000)
                items[itemId] = SparkItemBundle(
                    document = itemDoc(itemId, "CONDITION"),
                    fields = linkedMapOf(
                        "name" to itemFieldDoc(itemId, "name", cond.name),
                        "intensity" to itemFieldDoc(itemId, "intensity", cond.intensity ?: 0),
                        "duration" to itemFieldDoc(itemId, "duration", fullMap["payload"]?.let { asMap(it)["duration"] }),
                        "summary" to itemFieldDoc(itemId, "summary", customSummary),
                        "origin" to itemFieldDoc(itemId, "origin", fullMap["source"]),
                        "payload" to itemFieldDoc(itemId, "payload", fullMap),
                    ),
                )
            }
        }

        normalized.conditions.forEach { cond ->
            if (cond.id in canonicalConditionIds) return@forEach
            val itemId = encodeWireElementId("lcond", cond.id)
            items[itemId] = SparkItemBundle(
                document = itemDoc(itemId, "LEGACY_CONDITION"),
                fields = linkedMapOf(
                    "name" to itemFieldDoc(itemId, "name", cond.name.take(20000)),
                    "intensity" to itemFieldDoc(itemId, "intensity", cond.intensity.take(200)),
                    "duration" to itemFieldDoc(itemId, "duration", cond.duration.take(20000)),
                    "summary" to itemFieldDoc(itemId, "summary", cond.summary.take(20000)),
                    "origin" to itemFieldDoc(itemId, "origin", cond.origin.take(20000)),
                    "payload" to itemFieldDoc(itemId, "payload", linkedMapOf("id" to cond.id)),
                ),
            )
        }

        normalized.personalNotes.forEach { note ->
            val itemId = encodeWireElementId("note", note.id)
            items[itemId] = SparkItemBundle(
                document = itemDoc(itemId, "NOTE"),
                fields = linkedMapOf(
                    "title" to itemFieldDoc(itemId, "title", note.title.take(200)),
                    "text" to itemFieldDoc(itemId, "text", note.text.take(20000)),
                ),
            )
        }

        normalized.attributes.forEach { attr ->
            val acronym = attr.acronym.ifBlank { attr.name }.take(16)
            val itemId = encodeWireElementId("attr", acronym)
            val itemFields = linkedMapOf<String, Map<String, Any?>>()
            itemFields["name"] = itemFieldDoc(itemId, "name", attr.name.take(64))
            itemFields["acronym"] = itemFieldDoc(itemId, "acronym", attr.acronym.take(16))
            itemFields["value"] = itemFieldDoc(itemId, "value", attr.value)
            itemFields["modifier"] = itemFieldDoc(itemId, "modifier", attr.modifier)
            attr.skills.forEach { skill ->
                val slug = encodeSkillSlug(skill.name)
                val nameKey = "skill_${slug}_name"
                val valueKey = "skill_${slug}_value"
                val modKey = "skill_${slug}_modifier"
                itemFields[nameKey] = itemFieldDoc(itemId, nameKey, skill.name.take(64))
                itemFields[valueKey] = itemFieldDoc(itemId, valueKey, skill.value)
                itemFields[modKey] = itemFieldDoc(itemId, modKey, skill.modifier)
            }
            items[itemId] = SparkItemBundle(
                document = itemDoc(itemId, "ATTRIBUTE"),
                fields = itemFields,
            )
        }

        return SparkSheetDocuments(
            root = root,
            fields = fields,
            items = items,
            mediaAvatar = mediaAvatar,
            localPhotoUri = localPhotoUri,
        )
    }

    fun fromSparkDocuments(docs: SparkSheetDocuments): CharacterRecord {
        val root = docs.root
        val fields = docs.fields
        fun fieldVal(key: String): Any? = fields[key]?.get("value")
        fun str(key: String, default: String = ""): String = fieldVal(key) as? String ?: default
        fun intVal(key: String, default: Int = 0): Int = asInt(fieldVal(key)) ?: default
        fun longOpt(key: String): Long? = asLong(fieldVal(key))

        val avatarFromMedia = docs.mediaAvatar?.let { media ->
            val dataUri = media["dataUri"] as? String
            if (!dataUri.isNullOrBlank()) {
                dataUri
            } else {
                val base64 = media["bytesBase64"] as? String
                val mime = media["mimeType"] as? String ?: "image/jpeg"
                if (!base64.isNullOrBlank()) "data:$mime;base64,$base64" else null
            }
        }
        val resolvedPhotoUri = docs.localPhotoUri.ifBlank {
            avatarFromMedia ?: str("avatarPreset")
        }

        val activeItems = docs.items.filterValues { (it.document["deleted"] as? Boolean) != true }
        val inventory = mutableListOf<InventoryItem>()
        val itemStates = mutableListOf<ItemState>()
        val abilities = mutableListOf<Ability>()
        val powers = mutableListOf<Power>()
        val mysticAbilities = mutableListOf<MysticAbility>()
        val conditionInstances = mutableListOf<ConditionInstance>()
        val condSummaries = linkedMapOf<String, String>()
        val condFallbackSummaries = linkedMapOf<String, String>()
        val legacyConditions = mutableListOf<ConditionEffect>()
        val notes = mutableListOf<PersonalNote>()
        val attributes = mutableListOf<AttributeValue>()

        activeItems.forEach { (itemId, bundle) ->
            val kind = bundle.document["kind"] as? String ?: ""
            fun prop(key: String): Any? = bundle.fields[key]?.get("value")
            when {
                kind == "INVENTORY" || itemId.startsWith("inv_") -> {
                    val merged = LinkedHashMap<String, Any?>()
                    merged.putAll(asMap(prop("meta")))
                    merged.putAll(asMap(prop("enhancements")))
                    merged["id"] = merged["id"] ?: decodeWireElementId("inv", itemId)
                    prop("name")?.let { merged["name"] = it }
                    prop("state")?.let { merged["state"] = it }
                    prop("quantity")?.let { merged["quantity"] = it }
                    prop("durabilityCurrent")?.let { merged["durabilityCurrent"] = it }
                    prop("durabilityMax")?.let { merged["durabilityMax"] = it }
                    prop("itemCondition")?.let { merged["itemCondition"] = it }
                    prop("region")?.let { merged["region"] = it }
                    prop("effect")?.let { merged["effect"] = it }
                    prop("load")?.let { merged["load"] = it }
                    prop("backpackCapacity")?.let { merged["backpackCapacity"] = it }
                    if (bundle.fields.containsKey("agilityLimit")) merged["agilityLimit"] = prop("agilityLimit")
                    prop("quality")?.let { merged["quality"] = it }
                    prop("pg")?.let { merged["pg"] = it }
                    prop("pl")?.let { merged["pl"] = it }
                    prop("category")?.let { merged["category"] = it }
                    prop("linkedAshId")?.let { merged["linkedAshId"] = it }
                    prop("ashPurity")?.let { merged["ashPurity"] = it }
                    prop("mechanicalEffects")?.let { merged["mechanicalEffects"] = it }
                    inventory += fromPlainMap(merged, InventoryItem::class.java)
                }
                kind == "ITEM_STATE" || itemId.startsWith("istate_") -> {
                    val merged = linkedMapOf<String, Any?>(
                        "id" to decodeWireElementId("istate", itemId),
                        "definition" to prop("definition"),
                        "scope" to prop("scope"),
                        "material" to prop("material"),
                        "installations" to (prop("installations") ?: emptyList<Any?>()),
                        "stack" to (prop("stack") ?: mapOf("quantity" to 1)),
                        "consumable" to prop("consumable"),
                        "ammo" to prop("ammo"),
                        "reload" to prop("reload"),
                        "container" to prop("container"),
                    )
                    itemStates += canonicalJson.decodeFromString(
                        ItemState.serializer(), plainGson.toJson(normalizeItemStateIds(merged)),
                    )
                }
                kind == "ABILITY" || itemId.startsWith("abil_") -> {
                    val merged = LinkedHashMap<String, Any?>()
                    merged.putAll(asMap(prop("meta")))
                    merged["id"] = merged["id"] ?: decodeWireElementId("abil", itemId)
                    prop("name")?.let { merged["name"] = it }
                    prop("cost")?.let { merged["cost"] = it }
                    prop("action")?.let { merged["execution"] = it }
                    prop("range")?.let { merged["range"] = it }
                    if (bundle.fields.containsKey("duration")) merged["duration"] = prop("duration")
                    prop("effect")?.let { merged["effect"] = it }
                    prop("favorite")?.let { merged["favorite"] = it }
                    prop("available")?.let { merged["available"] = it }
                    abilities += canonicalJson.decodeFromString(Ability.serializer(), plainGson.toJson(merged))
                }
                kind == "POWER" || itemId.startsWith("pow_") -> {
                    val merged = LinkedHashMap<String, Any?>()
                    merged.putAll(asMap(prop("meta")))
                    merged["id"] = merged["id"] ?: decodeWireElementId("pow", itemId)
                    prop("name")?.let { merged["name"] = it }
                    prop("cost")?.let { merged["cost"] = it }
                    prop("costType")?.let { merged["costType"] = it }
                    prop("costValue")?.let { merged["costValue"] = it }
                    prop("action")?.let { merged["action"] = it }
                    prop("range")?.let { merged["range"] = it }
                    prop("duration")?.let { merged["duration"] = it }
                    prop("effect")?.let { merged["effect"] = it }
                    prop("active")?.let { merged["active"] = it }
                    prop("favorite")?.let { merged["favorite"] = it }
                    prop("available")?.let { merged["available"] = it }
                    prop("modifiers")?.let { merged["modifiers"] = it }
                    prop("linkedItemId")?.let { merged["linkedItemId"] = it }
                    powers += fromPlainMap(merged, Power::class.java)
                }
                kind == "MYSTIC" || itemId.startsWith("mys_") -> {
                    val merged = LinkedHashMap<String, Any?>()
                    merged.putAll(asMap(prop("meta")))
                    merged["id"] = merged["id"] ?: decodeWireElementId("mys", itemId)
                    prop("name")?.let { merged["name"] = it }
                    prop("cost")?.let { merged["cost"] = it }
                    prop("costType")?.let { merged["costType"] = it }
                    prop("costValue")?.let { merged["costValue"] = it }
                    prop("action")?.let { merged["action"] = it }
                    prop("range")?.let { merged["range"] = it }
                    prop("duration")?.let { merged["duration"] = it }
                    prop("effect")?.let { merged["effect"] = it }
                    prop("favorite")?.let { merged["favorite"] = it }
                    prop("available")?.let { merged["available"] = it }
                    prop("linkedItemId")?.let { merged["linkedInventoryItemId"] = it }
                    mysticAbilities += fromPlainMap(merged, MysticAbility::class.java)
                }
                kind == "CONDITION" || itemId.startsWith("cond_") -> {
                    val payloadMap = asMap(prop("payload"))
                    val decodedInst = canonicalJson.decodeFromString(
                        ConditionInstance.serializer(),
                        plainGson.toJson(payloadMap),
                    )
                    conditionInstances += decodedInst
                    val summaryDoc = bundle.fields["summary"]
                    val rawSummary = summaryDoc?.get("value") as? String
                    val summaryRev = asLong(summaryDoc?.get("revision")) ?: 0L
                    val isMigratedMirror = summaryRev <= 1L &&
                        rawSummary == decodedInst.name &&
                        bundle.fields.containsKey("origin")
                    if (rawSummary != null && !isMigratedMirror) {
                        condSummaries[decodedInst.instanceId.value] = rawSummary
                    }
                }
                kind == "LEGACY_CONDITION" || itemId.startsWith("lcond_") -> {
                    val payloadMap = asMap(prop("payload"))
                    val rawId = payloadMap["id"] as? String ?: decodeWireElementId("lcond", itemId)
                    val canonicalDoc = docs.items[encodeWireElementId("cond", rawId)]?.document
                    if (canonicalDoc != null) {
                        if ((canonicalDoc["deleted"] as? Boolean) != true) {
                            val legacySummary = prop("summary") as? String ?: ""
                            if (legacySummary.isNotBlank()) {
                                condFallbackSummaries[rawId] = legacySummary
                            }
                        }
                    } else {
                        legacyConditions += ConditionEffect(
                            id = rawId,
                            name = prop("name") as? String ?: "",
                            intensity = prop("intensity") as? String ?: "",
                            duration = prop("duration") as? String ?: "",
                            origin = prop("origin") as? String ?: "",
                            summary = prop("summary") as? String ?: "",
                        )
                    }
                }
                kind == "NOTE" || itemId.startsWith("note_") -> {
                    notes += PersonalNote(
                        id = decodeWireElementId("note", itemId),
                        title = prop("title") as? String ?: "",
                        text = prop("text") as? String ?: "",
                    )
                }
                kind == "ATTRIBUTE" || itemId.startsWith("attr_") -> {
                    val acronym = prop("acronym") as? String ?: decodeWireElementId("attr", itemId)
                    val slugs = bundle.fields.keys
                        .filter { it.startsWith("skill_") && it.contains('_') }
                        .map { it.removePrefix("skill_").substringBeforeLast('_') }
                        .distinct()
                    val decodedSkills = slugs.map { slug ->
                        val skillName = prop("skill_${slug}_name") as? String ?: slug
                        val skillVal = asInt(prop("skill_${slug}_value")) ?: 0
                        val skillMod = asInt(prop("skill_${slug}_modifier")) ?: 0
                        SkillValue(name = skillName, value = skillVal, modifier = skillMod)
                    }
                    attributes += AttributeValue(
                        name = prop("name") as? String ?: "",
                        acronym = acronym,
                        value = asInt(prop("value")) ?: 0,
                        modifier = asInt(prop("modifier")) ?: 0,
                        skills = orderSkillsDeterministically(acronym, decodedSkills),
                    )
                }
            }
        }

        val orderedAttributes = orderAttributesDeterministically(attributes)
        val lockType = root["lockType"] as? String ?: "NONE"
        val isLocked = root["isLocked"] as? Boolean ?: (lockType != "NONE")
        val updatedAt = asLong(root["updatedAt"]) ?: 0L
        val rootRev = asLong(root["revision"]) ?: 0L

        val resolvedConditions = if (conditionInstances.isNotEmpty()) {
            val withSummaries = conditionInstances.map { inst ->
                val resolvedSummary = condSummaries[inst.instanceId.value]
                    ?: condFallbackSummaries[inst.instanceId.value].orEmpty()
                inst.toLegacyEffect().copy(summary = resolvedSummary)
            }
            if (withSummaries.any { it.summary.isNotBlank() } || legacyConditions.isNotEmpty()) {
                withSummaries + legacyConditions
            } else {
                emptyList()
            }
        } else {
            legacyConditions
        }

        return CharacterRecord(
            id = root["id"] as? String ?: "",
            ownerId = root["ownerId"] as? String ?: "",
            campaignId = normalizeCampaignId(root["campaignId"] as? String ?: ""),
            name = str("name", "Novo personagem"),
            race = str("race"),
            subRace = str("subRace"),
            raceAttribute = str("raceAttribute"),
            occupation = str("occupation"),
            age = str("age"),
            level = intVal("level", 1),
            creationStatus = str("creationStatus", "COMPLETED"),
            creationStep = intVal("creationStep", 1),
            creationCompletedAt = longOpt("creationCompletedAt"),
            creationRulesVersion = intVal("creationRulesVersion", 1),
            itemSchemaVersion = asInt(root["itemSchemaVersion"]) ?: 0,
            canonicalSchemaVersion = asInt(root["canonicalSchemaVersion"]) ?: 0,
            migrationReviewPayload = str("migrationReviewPayload"),
            unrecognizedFieldsJson = str("unrecognizedFieldsPayload"),
            canonicalAbilitiesPayload = if (abilities.isEmpty()) "" else converters.canonicalAbilitiesToString(abilities),
            canonicalBodyPayload = str("canonicalBodyPayload"),
            canonicalConditionsPayload = if (conditionInstances.isEmpty()) "" else converters.canonicalConditionsToString(conditionInstances),
            activeModifiersPayload = str("activeModifiersPayload"),
            canonicalItemsPayload = if (itemStates.isEmpty()) "" else converters.canonicalItemsToString(itemStates),
            scopedItemCatalogPayload = str("scopedItemCatalogPayload"),
            canonicalProgressionPayload = str("canonicalProgressionPayload"),
            progressionLifeBonus = intVal("progressionLifeBonus"),
            progressionSanityBonus = intVal("progressionSanityBonus"),
            progressionArcaneBonus = intVal("progressionArcaneBonus"),
            progressionEnergyBonus = intVal("progressionEnergyBonus"),
            progressionHistory = decodeTypedList(fieldVal("progressionHistory")),
            money = intVal("money"),
            life = decodeResource(fieldVal("life")),
            sanity = decodeResource(fieldVal("sanity")),
            arcane = decodeResource(fieldVal("arcane")),
            energy = decodeResource(fieldVal("energy")),
            destiny = decodeResource(fieldVal("destiny"), ResourceValue(5, 5)),
            exhaustion = decodeResource(fieldVal("exhaustion"), ResourceValue(0, 10)),
            corruption = decodeResource(fieldVal("corruption"), ResourceValue(0, 100)),
            attributes = orderedAttributes.ifEmpty { defaultAttributes() },
            protectionAdjustments = decodeIntMap(fieldVal("protectionAdjustments")),
            positiveTraits = decodeStringList(fieldVal("positiveTraits")),
            negativeTraits = decodeStringList(fieldVal("negativeTraits")),
            pathName = str("pathName"),
            pathMotto = str("pathMotto"),
            powers = powers,
            inventory = inventory,
            itemCreationDraft = converters.stringToItemCreationDraft(str("itemCreationDraft")),
            story = str("story"),
            transports = converters.stringToTransports(str("transports")),
            familiars = converters.stringToFamiliars(str("familiars")),
            divineAscensionPayload = str("divineAscensionPayload"),
            divineRuntimePayload = str("divineRuntimePayload"),
            personalNotes = notes,
            updatedAt = updatedAt,
            lastEditedBy = root["updatedBy"] as? String ?: "",
            fieldUpdatedAt = if (rootRev > 0L) mapOf("@protocol" to 2L, "@revision" to rootRev) else emptyMap(),
            dirty = false,
            lastSyncedAt = 0L,
            height = str("height"),
            sex = str("sex"),
            size = str("size"),
            photoUri = resolvedPhotoUri,
            learnedKnowledges = decodeTypedList(fieldVal("learnedKnowledges")),
            arcaneKnowledges = decodeTypedList(fieldVal("arcaneKnowledges")),
            battleTechniques = decodeTypedList(fieldVal("battleTechniques")),
            pathKeywords = decodeStringList(fieldVal("pathKeywords")),
            pathPillars = decodeStringList(fieldVal("pathPillars")),
            containerCapacity = intVal("containerCapacity"),
            agilityLimit = str("agilityLimit"),
            mysticAbilities = mysticAbilities,
            conditions = resolvedConditions,
            isLocked = isLocked,
            lockType = lockType,
            lockedBy = root["lockedBy"] as? String ?: "",
            lockedAt = asLong(root["lockedAt"]),
            deleted = root["deleted"] as? Boolean ?: false,
            appliedDeliveryIds = decodeStringList(root["appliedDeliveryIds"]),
        )
    }

    /**
     * Produces bounded operation batches satisfying Spark's Rules access budget
     * (at most 5 targets when any ITEM_FIELD is included, otherwise at most 7 targets).
     */
    fun buildOperationBatches(
        before: CharacterRecord?,
        after: CharacterRecord,
        operationIdPrefix: String,
        actorUid: String,
        existingRevisions: Map<String, Long> = emptyMap(),
        sheetRevision: Long = 0L,
        updatedAt: Long = after.updatedAt,
        authorizedCampaignId: String = "",
        skipDefaultFieldsOnCreate: Boolean = false,
    ): List<SparkOperationBatch> {
        val afterDocs = toSparkDocuments(
            record = after,
            revision = (sheetRevision + 1L).coerceAtLeast(1L),
            fieldRevisions = existingRevisions,
            lastOperationId = operationIdPrefix,
            updatedBy = actorUid,
            updatedAt = updatedAt,
            authorizedCampaignId = authorizedCampaignId,
        )
        val beforeDocs = before?.let {
            toSparkDocuments(
                record = it,
                revision = sheetRevision.coerceAtLeast(1L),
                fieldRevisions = existingRevisions,
                lastOperationId = operationIdPrefix,
                updatedBy = actorUid,
                updatedAt = it.updatedAt,
                authorizedCampaignId = authorizedCampaignId,
            )
        }

        val writes = mutableListOf<SparkTargetWrite>()

        if (beforeDocs == null || rootChanged(beforeDocs.root, afterDocs.root)) {
            val nextRev = if (beforeDocs == null) 1L else sheetRevision + 1L
            writes += SparkTargetWrite(
                target = SparkTargetSpec(
                    kind = TargetKind.SHEET,
                    op = if (beforeDocs == null) TargetOp.CREATE else TargetOp.SET,
                    expectedRevision = if (beforeDocs == null) 0L else sheetRevision,
                    nextRevision = nextRev,
                    baseValueJson = beforeDocs?.root?.let(gson::toJson),
                    valueJson = gson.toJson(afterDocs.root),
                ),
                documentData = afterDocs.root + ("revision" to nextRev),
            )
        }

        // Fast creation: omit every field/item whose absence decodes to the identical domain sheet.
        // Readers rebuild those values from their defaults, so creation fits in a single transaction.
        val redundantFields = mutableSetOf<String>()
        val redundantItemIds = mutableSetOf<String>()
        if (beforeDocs == null && skipDefaultFieldsOnCreate) {
            val fullDomain = fromSparkDocuments(afterDocs).toDomain()
            afterDocs.fields.keys.forEach { key ->
                if (fromSparkDocuments(afterDocs.copy(fields = afterDocs.fields - key)).toDomain() == fullDomain) {
                    redundantFields += key
                }
            }
            val trimmed = afterDocs.copy(fields = afterDocs.fields - redundantFields)
            val attributeIds = afterDocs.items.filter { (id, bundle) ->
                bundle.document["kind"] == "ATTRIBUTE" || id.startsWith("attr_")
            }.keys
            if (attributeIds.isNotEmpty() &&
                fromSparkDocuments(trimmed.copy(items = trimmed.items - attributeIds)).toDomain() == fullDomain
            ) {
                redundantItemIds += attributeIds
            }
        }

        afterDocs.fields.forEach { (fieldKey, doc) ->
            val oldValue = beforeDocs?.fields?.get(fieldKey)?.get("value")
            val newValue = doc["value"]
            val matchesDefaultOnCreate = fieldKey in redundantFields
            if (!matchesDefaultOnCreate && (beforeDocs == null || oldValue != newValue)) {
                val expectedRev = if (beforeDocs == null) {
                    0L
                } else {
                    existingRevisions["field:$fieldKey"] ?: existingRevisions[fieldKey] ?: 0L
                }
                val nextRev = expectedRev + 1L
                writes += SparkTargetWrite(
                    target = SparkTargetSpec(
                        kind = TargetKind.FIELD,
                        key = fieldKey,
                        op = if (beforeDocs == null || expectedRev == 0L) TargetOp.CREATE else TargetOp.SET,
                        expectedRevision = expectedRev,
                        nextRevision = nextRev,
                        baseValueJson = if (beforeDocs == null) null else gson.toJson(oldValue),
                        valueJson = gson.toJson(newValue),
                    ),
                    documentData = doc + ("revision" to nextRev),
                )
            }
        }

        afterDocs.items.forEach { (itemId, bundle) ->
            if (itemId in redundantItemIds) return@forEach
            val oldBundle = beforeDocs?.items?.get(itemId)
            val itemExpectedRev = if (oldBundle == null) 0L else (existingRevisions["item:$itemId"] ?: 0L)
            val changedFields = bundle.fields.filter { (fieldKey, fieldDoc) ->
                oldBundle == null || oldBundle.fields[fieldKey]?.get("value") != fieldDoc["value"]
            }
            val needsItemCreate = oldBundle == null || (itemExpectedRev == 0L && changedFields.isNotEmpty())
            if (needsItemCreate) {
                val itemNextRev = 1L
                writes += SparkTargetWrite(
                    target = SparkTargetSpec(
                        kind = TargetKind.ITEM,
                        itemId = itemId,
                        op = TargetOp.CREATE,
                        expectedRevision = 0L,
                        nextRevision = itemNextRev,
                        baseValueJson = null,
                        valueJson = gson.toJson(bundle.document),
                    ),
                    documentData = bundle.document + ("revision" to itemNextRev),
                )
            }
            val fieldsToWrite = if (oldBundle != null && needsItemCreate) {
                val skillNameKeys = changedFields.keys
                    .filter { it.startsWith("skill_") && (it.endsWith("_value") || it.endsWith("_modifier")) }
                    .map { it.substringBeforeLast('_') + "_name" }
                    .toSet()
                bundle.fields.filterKeys { key ->
                    key in changedFields || key == "name" || key == "acronym" || key in skillNameKeys
                }
            } else if (oldBundle == null && itemId.startsWith("inv_")) {
                // Pack new inventory items into a single `meta` field (<= 50 map keys per Rules) so
                // creating 1-2 items commits in ONE atomic Firestore transaction instead of 2-5 chained ones.
                // Subsequent edits still patch individual subfields which override `meta` on decode.
                val metaDoc = bundle.fields["meta"]
                if (metaDoc != null) {
                    val fullMeta = LinkedHashMap<String, Any?>()
                    fullMeta.putAll(asMap(metaDoc["value"]))
                    fullMeta.putAll(asMap(bundle.fields["enhancements"]?.get("value")))
                    listOf(
                        "name", "state", "quantity", "durabilityCurrent", "durabilityMax",
                        "itemCondition", "region", "effect", "load", "backpackCapacity",
                        "agilityLimit", "quality", "pg", "pl", "category", "linkedAshId",
                        "ashPurity", "mechanicalEffects",
                    ).forEach { key ->
                        if (bundle.fields.containsKey(key)) {
                            fullMeta[key] = bundle.fields[key]?.get("value")
                        }
                    }
                    linkedMapOf("meta" to (metaDoc + ("value" to fullMeta)))
                } else {
                    changedFields.filter { (fieldKey, fieldDoc) ->
                        !isDefaultItemFieldOnCreate(itemId, fieldKey, fieldDoc["value"])
                    }
                }
            } else if (oldBundle == null && itemId.startsWith("pow_")) {
                val metaDoc = bundle.fields["meta"]
                if (metaDoc != null) {
                    val fullMeta = LinkedHashMap<String, Any?>()
                    fullMeta.putAll(asMap(metaDoc["value"]))
                    listOf(
                        "name", "cost", "costType", "costValue", "action", "range", "duration",
                        "effect", "active", "favorite", "available", "modifiers", "linkedItemId",
                    ).forEach { key ->
                        if (bundle.fields.containsKey(key)) fullMeta[key] = bundle.fields[key]?.get("value")
                    }
                    linkedMapOf("meta" to (metaDoc + ("value" to fullMeta)))
                } else {
                    changedFields.filter { (fieldKey, fieldDoc) ->
                        !isDefaultItemFieldOnCreate(itemId, fieldKey, fieldDoc["value"])
                    }
                }
            } else if (oldBundle == null && itemId.startsWith("mys_")) {
                val metaDoc = bundle.fields["meta"]
                if (metaDoc != null) {
                    val fullMeta = LinkedHashMap<String, Any?>()
                    fullMeta.putAll(asMap(metaDoc["value"]))
                    listOf(
                        "name", "cost", "costType", "costValue", "action", "range", "duration",
                        "effect", "favorite", "available",
                    ).forEach { key ->
                        if (bundle.fields.containsKey(key)) fullMeta[key] = bundle.fields[key]?.get("value")
                    }
                    bundle.fields["linkedItemId"]?.get("value")?.let { fullMeta["linkedInventoryItemId"] = it }
                    linkedMapOf("meta" to (metaDoc + ("value" to fullMeta)))
                } else {
                    changedFields.filter { (fieldKey, fieldDoc) ->
                        !isDefaultItemFieldOnCreate(itemId, fieldKey, fieldDoc["value"])
                    }
                }
            } else if (oldBundle == null && itemId.startsWith("abil_")) {
                val metaDoc = bundle.fields["meta"]
                if (metaDoc != null) {
                    val fullMeta = LinkedHashMap<String, Any?>()
                    fullMeta.putAll(asMap(metaDoc["value"]))
                    bundle.fields["name"]?.get("value")?.let { fullMeta["name"] = it }
                    bundle.fields["cost"]?.get("value")?.let { fullMeta["cost"] = it }
                    bundle.fields["action"]?.get("value")?.let { fullMeta["execution"] = it }
                    bundle.fields["range"]?.get("value")?.let { fullMeta["range"] = it }
                    if (bundle.fields.containsKey("duration")) fullMeta["duration"] = bundle.fields["duration"]?.get("value")
                    bundle.fields["effect"]?.get("value")?.let { fullMeta["effect"] = it }
                    bundle.fields["favorite"]?.get("value")?.let { fullMeta["favorite"] = it }
                    bundle.fields["available"]?.get("value")?.let { fullMeta["available"] = it }
                    if (bundle.fields.containsKey("modifiers")) fullMeta["mechanicalEffect"] = bundle.fields["modifiers"]?.get("value")
                    linkedMapOf("meta" to (metaDoc + ("value" to fullMeta)))
                } else {
                    changedFields.filter { (fieldKey, fieldDoc) ->
                        !isDefaultItemFieldOnCreate(itemId, fieldKey, fieldDoc["value"])
                    }
                }
            } else if (itemId.startsWith("cond_")) {
                // Canonical ConditionInstance is stored in `payload` and its optional user note in `summary`.
                // Omit derived mirror fields (name/intensity/duration/origin) so edits never split across batches.
                changedFields.filter { (fieldKey, fieldDoc) ->
                    fieldKey == "payload" ||
                        (fieldKey == "summary" && (oldBundle != null || (fieldDoc["value"] as? String).orEmpty().isNotEmpty()))
                }
            } else if (oldBundle == null) {
                // Skip default/empty item fields on creation so new items/powers fit in 1-2 transactions instead of 4-5.
                changedFields.filter { (fieldKey, fieldDoc) ->
                    !isDefaultItemFieldOnCreate(itemId, fieldKey, fieldDoc["value"])
                }
            } else {
                changedFields
            }
            fieldsToWrite.forEach { (fieldKey, fieldDoc) ->
                val oldFieldValue = oldBundle?.fields?.get(fieldKey)?.get("value")
                val newFieldValue = fieldDoc["value"]
                val expectedRev = if (oldBundle == null || needsItemCreate) {
                    0L
                } else {
                    existingRevisions["item:$itemId/field:$fieldKey"] ?: 0L
                }
                val nextRev = expectedRev + 1L
                writes += SparkTargetWrite(
                    target = SparkTargetSpec(
                        kind = TargetKind.ITEM_FIELD,
                        itemId = itemId,
                        key = fieldKey,
                        op = if (oldBundle == null || expectedRev == 0L) TargetOp.CREATE else TargetOp.SET,
                        expectedRevision = expectedRev,
                        nextRevision = nextRev,
                        baseValueJson = if (oldBundle == null || needsItemCreate) null else gson.toJson(oldFieldValue),
                        valueJson = gson.toJson(newFieldValue),
                    ),
                    documentData = fieldDoc + ("revision" to nextRev),
                )
            }
        }

        beforeDocs?.items?.keys?.forEach { oldItemId ->
            if (oldItemId !in afterDocs.items) {
                val expectedRev = existingRevisions["item:$oldItemId"] ?: 0L
                val nextRev = expectedRev + 1L
                val oldKind = beforeDocs.items[oldItemId]?.document?.get("kind") as? String ?: ""
                writes += SparkTargetWrite(
                    target = SparkTargetSpec(
                        kind = TargetKind.ITEM,
                        itemId = oldItemId,
                        op = TargetOp.TOMBSTONE,
                        expectedRevision = expectedRev,
                        nextRevision = nextRev,
                        baseValueJson = gson.toJson(beforeDocs.items[oldItemId]?.document),
                        valueJson = "{\"deleted\":true}",
                    ),
                    documentData = linkedMapOf(
                        "id" to oldItemId,
                        "kind" to oldKind,
                        "deleted" to true,
                        "revision" to nextRev,
                        "lastOperationId" to operationIdPrefix,
                        "updatedBy" to actorUid,
                        "updatedAt" to updatedAt,
                    ),
                )
            }
        }

        return chunkWritesWithinBudget(writes).mapIndexed { index, chunk ->
            val opId = if (index == 0) operationIdPrefix else "${operationIdPrefix}_$index"
            val stampedWrites = chunk.map { write ->
                if (write.isMedia || "lastOperationId" !in write.documentData) {
                    write
                } else {
                    write.copy(documentData = write.documentData + ("lastOperationId" to opId))
                }
            }
            val targets = stampedWrites.map { it.target }
            val opType = inferOperationType(before == null, targets)
            val hash = computeCanonicalPayloadHash(
                sheetId = after.id,
                type = opType,
                targetKey = targets.first().canonicalTargetKey(),
                targets = targets,
            )
            SparkOperationBatch(
                operationId = opId,
                type = opType,
                targets = targets,
                writes = stampedWrites,
                payloadHash = hash,
            )
        }
    }

    private fun chunkWritesWithinBudget(writes: List<SparkTargetWrite>): List<List<SparkTargetWrite>> {
        if (writes.isEmpty()) return emptyList()
        // Firestore Rules require TOMBSTONE item updates to execute under DELETE_ITEM (never mixed with UPSERT_ITEM/PATCH_FIELDS).
        val (tombstones, mutations) = writes.partition {
            it.target.kind == TargetKind.ITEM && it.target.op == TargetOp.TOMBSTONE
        }
        val batches = mutableListOf<List<SparkTargetWrite>>()
        fun appendChunked(group: List<SparkTargetWrite>) {
            var current = mutableListOf<SparkTargetWrite>()
            for (write in group) {
                val candidate = current + write
                if (!isWithinSparkTransactionBudget(candidate.map { it.target })) {
                    if (current.isNotEmpty()) {
                        batches += current.toList()
                        current = mutableListOf()
                    }
                }
                current += write
            }
            if (current.isNotEmpty()) batches += current.toList()
        }
        appendChunked(mutations)
        appendChunked(tombstones)
        return batches
    }

    fun isPresetAvatar(photoUri: String): Boolean =
        photoUri.startsWith("preset:", ignoreCase = true)

    fun isLocalOnlyPhotoUri(photoUri: String): Boolean {
        if (photoUri.isBlank() || isPresetAvatar(photoUri)) return false
        if (photoUri.startsWith("data:image/", ignoreCase = true)) {
            return photoUri.toByteArray(Charsets.UTF_8).size > MAX_AVATAR_THUMBNAIL_BYTES
        }
        return true
    }

    private data class EncodedAvatar(
        val preset: String,
        val mediaDoc: Map<String, Any?>?,
        val localPhotoUri: String,
    )

    private fun encodeAvatar(photoUri: String, updatedBy: String, updatedAt: Long): EncodedAvatar {
        if (photoUri.isBlank()) return EncodedAvatar("", null, "")
        if (isPresetAvatar(photoUri)) return EncodedAvatar(photoUri, null, "")
        if (photoUri.startsWith("data:image/", ignoreCase = true)) {
            val byteSize = photoUri.toByteArray(Charsets.UTF_8).size
            if (byteSize <= MAX_AVATAR_THUMBNAIL_BYTES) {
                val mimeType = photoUri.substringAfter("data:", "image/jpeg").substringBefore(';').ifBlank { "image/jpeg" }
                val mediaDoc = linkedMapOf<String, Any?>(
                    "dataUri" to photoUri,
                    "mimeType" to mimeType,
                    "width" to MAX_AVATAR_DIMENSION_PX,
                    "height" to MAX_AVATAR_DIMENSION_PX,
                    "updatedBy" to updatedBy,
                    "updatedAt" to updatedAt,
                )
                return EncodedAvatar("", mediaDoc, photoUri)
            }
        }
        return EncodedAvatar("", null, photoUri)
    }

    private fun isDefaultItemFieldOnCreate(itemId: String, fieldKey: String, value: Any?): Boolean = when {
        itemId.startsWith("inv_") -> when (fieldKey) {
            "pg", "pl", "backpackCapacity", "load", "quantity" -> value == 0
            "agilityLimit" -> value == null
            "region", "effect", "linkedAshId" -> value == ""
            "itemCondition" -> value == ItemCondition.NORMAL.name
            "quality" -> value == ItemQuality.COMMON.name
            "ashPurity" -> value == AshPurity.RAW.name
            "mechanicalEffects" -> value is List<*> && value.isEmpty()
            "enhancements" -> value is Map<*, *> &&
                (value["installedEnhancements"] as? List<*>).orEmpty().isEmpty() &&
                asInt(value["enhancementSlots"]) == 0 &&
                asInt(value["enhancementChargesCurrent"]) == 0 &&
                asInt(value["enhancementChargesMax"]) == 0
            else -> false
        }
        itemId.startsWith("pow_") || itemId.startsWith("mys_") -> when (fieldKey) {
            "cost", "action", "range", "duration", "effect", "linkedItemId" -> value == ""
            "costValue" -> value == 0
            "active", "favorite" -> value == false
            "available" -> value == true
            "modifiers" -> value == null || (value is List<*> && value.isEmpty())
            else -> false
        }
        itemId.startsWith("istate_") -> when (fieldKey) {
            "material", "consumable", "ammo", "reload", "container" -> value == null
            "installations" -> value is List<*> && value.isEmpty()
            "stack" -> value == 1 || (value is Map<*, *> && value.size == 1 && asInt(value["quantity"]) == 1)
            else -> false
        }
        itemId.startsWith("abil_") -> when (fieldKey) {
            "effect", "linkedItemId" -> value == ""
            "duration", "modifiers" -> value == null
            "active", "favorite" -> value == false
            "available" -> value == true
            else -> false
        }
        itemId.startsWith("lcond_") -> when (fieldKey) {
            "intensity", "duration", "summary", "origin" -> value == ""
            else -> false
        }
        itemId.startsWith("note_") -> when (fieldKey) {
            "title", "text" -> value == ""
            else -> false
        }
        else -> false
    }

    private fun rootChanged(before: Map<String, Any?>, after: Map<String, Any?>): Boolean {
        val ignored = setOf("revision", "lastOperationId", "updatedBy", "updatedAt")
        return (before.keys + after.keys).any { key ->
            key !in ignored && before[key] != after[key]
        }
    }

    private fun inferOperationType(isCreate: Boolean, targets: List<SparkTargetSpec>): OperationType = when {
        isCreate && targets.any { it.kind == TargetKind.SHEET } -> OperationType.CREATE_SHEET
        targets.all { it.op == TargetOp.TOMBSTONE && it.kind == TargetKind.ITEM } -> OperationType.DELETE_ITEM
        targets.any { it.kind == TargetKind.ITEM } -> OperationType.UPSERT_ITEM
        else -> OperationType.PATCH_FIELDS
    }

    private fun resourceMap(value: ResourceValue): Map<String, Any?> = linkedMapOf(
        "current" to value.current.coerceAtLeast(0),
        "maximum" to value.maximum.coerceAtLeast(0),
        "adjustment" to value.adjustment,
    )

    private fun decodeResource(raw: Any?, default: ResourceValue = ResourceValue()): ResourceValue {
        val map = asMap(raw)
        if (map.isEmpty()) return default
        return ResourceValue(
            current = asInt(map["current"]) ?: default.current,
            maximum = asInt(map["maximum"]) ?: default.maximum,
            adjustment = asInt(map["adjustment"]) ?: default.adjustment,
        )
    }

    private fun decodeStringList(raw: Any?): List<String> =
        (raw as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()

    private fun decodeIntMap(raw: Any?): Map<String, Int> =
        asMap(raw).mapNotNull { (k, v) -> asInt(v)?.let { k to it } }.toMap(linkedMapOf())

    private inline fun <reified T> decodeTypedList(raw: Any?): List<T> {
        val list = raw as? List<*> ?: return emptyList()
        val type = TypeToken.getParameterized(List::class.java, T::class.java).type
        return runCatching { plainGson.fromJson<List<T>>(plainGson.toJson(list), type) }.getOrDefault(emptyList())
    }

    private fun toPlainList(value: List<*>): List<Any?> =
        plain(JsonParser.parseString(plainGson.toJson(value))) as? List<*> ?: emptyList()

    private fun toPlainMap(value: Any): Map<String, Any?> =
        objectMap(JsonParser.parseString(plainGson.toJson(value)).asJsonObject)

    /** Accept pre-cutover Gson ID wrappers as well as canonical migration scalar IDs. */
    private fun normalizeItemStateIds(value: Any?, key: String = ""): Any? = when {
        key in setOf("id", "ammoTypeId") && value is Map<*, *> &&
            value.keys == setOf("value") -> value["value"]
        value is Map<*, *> -> value.entries.associate {
            it.key.toString() to normalizeItemStateIds(it.value, it.key.toString())
        }
        value is List<*> -> value.map {
            normalizeItemStateIds(it, if (key == "containedItemIds") "id" else "")
        }
        else -> value
    }

    private fun <T> fromPlainMap(map: Map<String, Any?>, clazz: Class<T>): T =
        plainGson.fromJson(plainGson.toJson(map), clazz)

    private fun objectMap(value: JsonObject): Map<String, Any?> =
        value.entrySet().associate { it.key to plain(it.value) }

    private fun plain(value: JsonElement?): Any? = when {
        value == null || value.isJsonNull -> null
        value.isJsonObject -> objectMap(value.asJsonObject)
        value.isJsonArray -> value.asJsonArray.map(::plain)
        value.asJsonPrimitive.isBoolean -> value.asBoolean
        value.asJsonPrimitive.isNumber -> value.asString.toLongOrNull() ?: value.asDouble
        else -> value.asString
    }

    private fun asMap(raw: Any?): Map<String, Any?> =
        (raw as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value } ?: emptyMap()

    private fun asLong(raw: Any?): Long? = when (raw) {
        is Long -> raw
        is Int -> raw.toLong()
        is Number -> raw.toLong()
        is String -> raw.toLongOrNull()
        else -> null
    }

    private fun asInt(raw: Any?): Int? = asLong(raw)?.toInt()

    private fun orderSkillsDeterministically(acronym: String, skills: List<SkillValue>): List<SkillValue> {
        val defaultSkills = defaultAttributes()
            .firstOrNull { it.acronym.equals(acronym, ignoreCase = true) }
            ?.skills
            .orEmpty()
        val byName = LinkedHashMap<String, SkillValue>()
        defaultSkills.forEach { byName[it.name] = it }
        skills.forEach { byName[it.name] = it }
        val defaultOrder = defaultSkills.mapIndexed { idx, s -> s.name to idx }.toMap()
        return byName.values.sortedWith(compareBy({ defaultOrder[it.name] ?: Int.MAX_VALUE }, { it.name }))
    }

    private fun orderAttributesDeterministically(attributes: List<AttributeValue>): List<AttributeValue> {
        val defaults = defaultAttributes()
        val byAcronym = LinkedHashMap<String, AttributeValue>()
        defaults.forEach { byAcronym[it.acronym] = it }
        attributes.forEach { attr ->
            val fallback = byAcronym[attr.acronym]
            byAcronym[attr.acronym] = attr.copy(
                name = attr.name.ifBlank { fallback?.name.orEmpty() },
                skills = orderSkillsDeterministically(attr.acronym, attr.skills),
            )
        }
        val defaultOrder = defaults.mapIndexed { idx, attr -> attr.acronym to idx }.toMap()
        return byAcronym.values.sortedWith(compareBy({ defaultOrder[it.acronym] ?: Int.MAX_VALUE }, { it.acronym }))
    }
}
