package com.kinderman.sdo.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.firestore
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.kinderman.sdo.SdoApplication
import com.kinderman.sdo.data.local.AccountSheetRecord
import com.kinderman.sdo.data.local.AppDatabase
import com.kinderman.sdo.data.local.CampaignMemberRecord
import com.kinderman.sdo.data.local.CampaignRecord
import com.kinderman.sdo.data.local.CharacterConverters
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.SessionOperationRecord
import com.kinderman.sdo.data.local.SheetConflictRecord
import com.kinderman.sdo.data.local.SheetIntentRecord
import com.kinderman.sdo.data.local.SheetOutboxRecord
import com.kinderman.sdo.data.local.SheetRouteRecord
import com.kinderman.sdo.data.local.normalizedForSparkV2
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.data.repository.FirestoreCollections
import com.kinderman.sdo.domain.model.Ability
import com.kinderman.sdo.domain.model.AccountSheetSnapshot
import com.kinderman.sdo.domain.model.AttributeValue
import com.kinderman.sdo.domain.model.CampaignMemberState
import com.kinderman.sdo.domain.model.CampaignRole
import com.kinderman.sdo.domain.model.CampaignState
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.CharacterLock
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.ConditionInstance
import com.kinderman.sdo.domain.model.ConflictResolutionStatus
import com.kinderman.sdo.domain.model.CURRENT_CHARACTER_SCHEMA_VERSION
import com.kinderman.sdo.domain.model.DomainError
import com.kinderman.sdo.domain.model.IdempotencyMatchResult
import com.kinderman.sdo.domain.model.ItemState
import com.kinderman.sdo.domain.model.OperationReceipt
import com.kinderman.sdo.domain.model.OperationType
import com.kinderman.sdo.domain.model.ProgressionRecord
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SessionResource
import com.kinderman.sdo.domain.model.SheetConflictItem
import com.kinderman.sdo.domain.model.SheetIntent
import com.kinderman.sdo.domain.model.SheetIntentStatus
import com.kinderman.sdo.domain.model.CharacterSyncConflict
import com.kinderman.sdo.domain.model.SheetSyncStatus
import com.kinderman.sdo.domain.model.SparkTargetSpec
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.TargetKind
import com.kinderman.sdo.domain.model.TargetOp
import com.kinderman.sdo.domain.model.UserProfile
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.allCanonicalAbilitiesSafely
import com.kinderman.sdo.domain.model.applyCharacterEditKeys
import com.kinderman.sdo.domain.model.applyPendingIntentsOnConfirmed
import com.kinderman.sdo.domain.model.applySessionCommand
import com.kinderman.sdo.domain.model.buildConflictResolutionIntent
import com.kinderman.sdo.domain.model.characterEditKeys
import com.kinderman.sdo.domain.model.computeCanonicalPayloadHash
import com.kinderman.sdo.domain.model.decodeWireElementId
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.domain.model.resourceMaximum
import com.kinderman.sdo.domain.model.resourceValue
import com.kinderman.sdo.domain.model.toSyncStatus
import com.kinderman.sdo.domain.model.validateAtomicSparkActionBudget
import com.kinderman.sdo.domain.policy.CharacterAccessPolicy
import com.kinderman.sdo.domain.repository.RemoteSyncEvent
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json


class SparkSheetSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val app = applicationContext as SdoApplication
        if (app.sparkSyncEngine.drain()) Result.success() else Result.retry()
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        Log.e("SDO_SYNC", "SparkSheetSyncWorker failed", error)
        Result.retry()
    }
}

internal val sharedPlainGson = GsonBuilder().create()
internal val sharedConverters = CharacterConverters()
internal val sharedJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal fun normalizeFirestoreMap(input: Map<String, Any?>): Map<String, Any?> =
    input.mapValues { (_, v) -> normalizeFirestoreValue(v) }

internal fun normalizeFirestoreValue(value: Any?): Any? = when (value) {
    is Timestamp -> value.toDate().time
    is Map<*, *> -> value.entries.associate { it.key.toString() to normalizeFirestoreValue(it.value) }
    is List<*> -> value.map(::normalizeFirestoreValue)
    else -> value
}

internal fun jsonToPlainValue(raw: String?): Any? {
    if (raw.isNullOrBlank()) return ""
    val parsed = runCatching { JsonParser.parseString(raw) }.getOrNull() ?: return raw
    return gsonElementToPlain(parsed)
}

internal fun gsonElementToPlain(element: JsonElement?): Any? = when {
    element == null || element.isJsonNull -> null
    element.isJsonObject -> element.asJsonObject.entrySet().associate { it.key to gsonElementToPlain(it.value) }
    element.isJsonArray -> element.asJsonArray.map(::gsonElementToPlain)
    element.asJsonPrimitive.isBoolean -> element.asBoolean
    element.asJsonPrimitive.isNumber -> element.asString.toLongOrNull() ?: element.asDouble
    else -> element.asString
}

internal fun asLong(raw: Any?): Long? = when (raw) {
    is Long -> raw
    is Int -> raw.toLong()
    is Number -> raw.toLong()
    is String -> raw.toLongOrNull()
    else -> null
}

internal fun asInt(raw: Any?): Int? = asLong(raw)?.toInt()

internal fun summarizePlainJson(raw: String): String {
    if (raw.isBlank()) return "â€”"
    val parsed = runCatching { JsonParser.parseString(raw) }.getOrNull() ?: return raw.take(120)
    return when {
        parsed.isJsonPrimitive -> parsed.asJsonPrimitive.asString.take(120)
        parsed.isJsonObject -> {
            val obj = parsed.asJsonObject
            if (obj.has("current") && obj.has("maximum")) {
                "${obj.get("current").asString}/${obj.get("maximum").asString}"
            } else {
                raw.take(120)
            }
        }
        else -> raw.take(120)
    }
}

internal fun decodeLegacyCharacterRecord(raw: String): CharacterRecord? {
    if (raw.isBlank() || !raw.trimStart().startsWith("{")) return null
    return runCatching {
        val defaults = sharedPlainGson.toJsonTree(CharacterRecord()).asJsonObject
        val incoming = JsonParser.parseString(raw).asJsonObject
        incoming.entrySet().forEach { (key, value) ->
            if (!value.isJsonNull || defaults.get(key)?.isJsonNull != false) {
                defaults.add(key, value)
            }
        }
        sharedPlainGson.fromJson(defaults, CharacterRecord::class.java)?.normalizedForSparkV2()
    }.getOrNull()
}

internal fun parseOperationReceipt(data: Map<String, Any?>): OperationReceipt {
    val opType = runCatching { OperationType.valueOf(data["type"]?.toString().orEmpty()) }
        .getOrDefault(OperationType.PATCH_FIELDS)
    val preconditions = data["preconditions"] as? Map<*, *>
    val payloadHash = preconditions?.get("payloadHash")?.toString().orEmpty()
    val rawTargets = (data["targets"] as? List<*>).orEmpty().mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val kind = runCatching { TargetKind.valueOf(map["kind"]?.toString().orEmpty()) }.getOrNull()
            ?: return@mapNotNull null
        val op = runCatching { TargetOp.valueOf(map["op"]?.toString().orEmpty()) }.getOrNull()
            ?: return@mapNotNull null
        SparkTargetSpec(
            kind = kind,
            itemId = map["itemId"]?.toString().orEmpty(),
            key = map["key"]?.toString().orEmpty(),
            op = op,
            expectedRevision = asLong(map["expectedRevision"]) ?: 0L,
            nextRevision = asLong(map["nextRevision"]) ?: 1L,
            delta = asInt(map["delta"]),
            baseValueJson = map["baseValue"]?.let(sharedPlainGson::toJson),
            valueJson = map["desiredValue"]?.let(sharedPlainGson::toJson),
        )
    }
    return OperationReceipt(
        id = data["id"]?.toString().orEmpty(),
        actorUid = data["actorUid"]?.toString().orEmpty(),
        type = opType,
        outcome = data["outcome"]?.toString().orEmpty(),
        schemaVersion = asInt(data["schemaVersion"]) ?: 2,
        createdAt = asLong(data["createdAt"]) ?: 0L,
        targets = rawTargets,
        payloadHash = payloadHash,
        campaignId = preconditions?.get("campaignId")?.toString(),
        lockType = preconditions?.get("lockType")?.toString(),
    )
}

internal fun expectedItemKindFromId(itemId: String): String = when {
    itemId.startsWith("inv_") -> "INVENTORY"
    itemId.startsWith("istate_") -> "ITEM_STATE"
    itemId.startsWith("abil_") -> "ABILITY"
    itemId.startsWith("pow_") -> "POWER"
    itemId.startsWith("mys_") -> "MYSTIC"
    itemId.startsWith("cond_") -> "CONDITION"
    itemId.startsWith("lcond_") -> "LEGACY_CONDITION"
    itemId.startsWith("note_") -> "NOTE"
    itemId.startsWith("attr_") -> "ATTRIBUTE"
    else -> ""
}

internal fun computeFieldDelta(
    key: String,
    prevValue: Any?,
    delta: Int,
    minValue: Int?,
    maxValue: Int?,
): Result<Any> {
    if (key == "money") {
        val current = asLong(prevValue)
            ?: return Result.failure(DomainError.ValidationError("Saldo monetÃ¡rio invÃ¡lido."))
        val min = (minValue ?: 0).toLong()
        val max = (maxValue ?: Int.MAX_VALUE).toLong()
        val next = current + delta
        return if (next < min || next > max) {
            Result.failure(DomainError.InsufficientResource("Dinheiro insuficiente (atual=$current, delta=$delta)."))
        } else {
            Result.success(next)
        }
    }
    val map = (prevValue as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value }?.toMutableMap()
        ?: return Result.failure(DomainError.ValidationError("Recurso $key invÃ¡lido."))
    val current = asLong(map["current"]) ?: 0L
    val min = (minValue ?: 0).toLong()
    val maxBound = minOf(if (delta > 0) maxValue?.toLong() ?: Int.MAX_VALUE.toLong() else Int.MAX_VALUE.toLong(),
        if (delta > 0) asLong(map["maximum"]) ?: 0L else Int.MAX_VALUE.toLong(),
        if (key == "corruption") 100L else Int.MAX_VALUE.toLong())
    val next = current + delta
    if (next < min || next > maxBound) {
        return Result.failure(DomainError.InsufficientResource("Recurso $key insuficiente (atual=$current, delta=$delta)."))
    }
    map["current"] = next
    return Result.success(map)
}

internal fun buildUpdatedRootDoc(
    sheetId: String,
    actorUid: String,
    remoteRoot: Map<String, Any?>?,
    target: SparkTargetSpec,
    operationId: String,
    nextRevision: Long,
    serverTimestampToken: Any,
    deliveryId: String,
): Map<String, Any?> {
    val desired = jsonToPlainValue(target.valueJson) as? Map<*, *> ?: emptyMap<String, Any?>()
    if (target.op == TargetOp.CREATE || remoteRoot == null) {
        val lockType = desired["lockType"]?.toString() ?: "NONE"
        val isLocked = lockType != "NONE"
        val deliveries = (desired["appliedDeliveryIds"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
        return linkedMapOf(
            "id" to sheetId,
            "ownerId" to (desired["ownerId"]?.toString()?.ifBlank { actorUid } ?: actorUid),
            "campaignId" to normalizeCampaignId(desired["campaignId"]?.toString().orEmpty()),
            "authorizedCampaignId" to normalizeCampaignId(desired["authorizedCampaignId"]?.toString().orEmpty()),
            "deleted" to false,
            "schemaVersion" to SparkWireCodec.SCHEMA_VERSION,
            "canonicalSchemaVersion" to (asInt(desired["canonicalSchemaVersion"]) ?: CURRENT_CHARACTER_SCHEMA_VERSION),
            "itemSchemaVersion" to (asInt(desired["itemSchemaVersion"]) ?: 0),
            "protocolVersion" to SparkWireCodec.PROTOCOL_VERSION,
            "lockType" to lockType,
            "isLocked" to isLocked,
            "lockedBy" to if (isLocked) actorUid else "",
            "lockedAt" to if (isLocked) serverTimestampToken else null,
            "revision" to nextRevision,
            "lastOperationId" to operationId,
            "updatedBy" to actorUid,
            "updatedAt" to serverTimestampToken,
            "appliedDeliveryIds" to deliveries,
        )
    }

    val result = LinkedHashMap<String, Any?>(remoteRoot)
    result["id"] = sheetId
    result["schemaVersion"] = SparkWireCodec.SCHEMA_VERSION
    result["protocolVersion"] = SparkWireCodec.PROTOCOL_VERSION
    result["revision"] = nextRevision
    result["lastOperationId"] = operationId
    result["updatedBy"] = actorUid
    result["updatedAt"] = serverTimestampToken
    when (target.op) {
        TargetOp.LOCK -> {
            val lockType = desired["lockType"]?.toString() ?: "NONE"
            val locked = lockType != "NONE"
            result["lockType"] = lockType
            result["isLocked"] = locked
            result["lockedBy"] = if (locked) actorUid else ""
            result["lockedAt"] = if (locked) serverTimestampToken else null
        }
        TargetOp.TRANSFER -> {
            result["ownerId"] = desired["ownerId"]?.toString() ?: result["ownerId"]
            result["authorizedCampaignId"] = ""
        }
        TargetOp.LINK -> {
            val nextCid = normalizeCampaignId(desired["campaignId"]?.toString().orEmpty())
            val authCid = normalizeCampaignId(desired["authorizedCampaignId"]?.toString().orEmpty())
            result["campaignId"] = nextCid
            result["authorizedCampaignId"] = authCid
        }
        TargetOp.UNLINK -> {
            result["campaignId"] = ""
            result["authorizedCampaignId"] = ""
        }
        TargetOp.TOMBSTONE -> {
            result["deleted"] = true
            result["authorizedCampaignId"] = ""
        }
        TargetOp.RESTORE -> {
            result["deleted"] = false
        }
        TargetOp.DELIVERY -> {
            val prev = (remoteRoot["appliedDeliveryIds"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
            result["appliedDeliveryIds"] = if (deliveryId.isNotBlank() && deliveryId !in prev) {
                prev + deliveryId
            } else {
                prev
            }
        }
        TargetOp.SET, TargetOp.CREATE, TargetOp.DELTA -> {
            desired["canonicalSchemaVersion"]?.let { result["canonicalSchemaVersion"] = asInt(it) ?: 0 }
            desired["itemSchemaVersion"]?.let { result["itemSchemaVersion"] = asInt(it) ?: 0 }
        }
    }
    return result
}

internal fun applyExtendedTargetProjection(
    character: Character,
    target: SparkTargetSpec,
    absoluteForDelta: Boolean = true,
): Character {
    if (target.op == TargetOp.DELTA && !absoluteForDelta) return character
    val raw = target.valueJson.orEmpty()
    if (target.kind == TargetKind.FIELD) {
        val plain = jsonToPlainValue(raw)
        return when (target.key) {
            "creationStatus" -> character.copy(
                creationStatus = runCatching {
                    com.kinderman.sdo.domain.model.CharacterCreationStatus.valueOf(plain?.toString().orEmpty())
                }.getOrDefault(character.creationStatus),
            )
            "creationRulesVersion" -> character.copy(creationRulesVersion = asInt(plain) ?: character.creationRulesVersion)
            "creationCompletedAt" -> character.copy(creationCompletedAt = asLong(plain))
            "progressionLifeBonus" -> character.copy(progressionLifeBonus = asInt(plain) ?: character.progressionLifeBonus)
            "progressionSanityBonus" -> character.copy(progressionSanityBonus = asInt(plain) ?: character.progressionSanityBonus)
            "progressionArcaneBonus" -> character.copy(progressionArcaneBonus = asInt(plain) ?: character.progressionArcaneBonus)
            "progressionEnergyBonus" -> character.copy(progressionEnergyBonus = asInt(plain) ?: character.progressionEnergyBonus)
            "canonicalBodyPayload" -> {
                val payload = plain?.toString().orEmpty()
                val rec = character.toRecord().copy(canonicalBodyPayload = payload).toDomain()
                character.copy(bodyState = rec.bodyState, bodyRegions = rec.bodyRegions, organs = rec.organs)
            }
            "activeModifiersPayload" -> {
                val payload = plain?.toString().orEmpty()
                val rec = character.toRecord().copy(activeModifiersPayload = payload).toDomain()
                character.copy(activeModifiers = rec.activeModifiers)
            }
            "scopedItemCatalogPayload" -> {
                val payload = plain?.toString().orEmpty()
                val rec = character.toRecord().copy(scopedItemCatalogPayload = payload).toDomain()
                character.copy(customItemCatalog = rec.customItemCatalog)
            }
            "canonicalProgressionPayload" -> {
                val payload = plain?.toString().orEmpty()
                val rec = character.toRecord().copy(canonicalProgressionPayload = payload).toDomain()
                character.copy(progression = rec.progression)
            }
            "migrationReviewPayload" -> {
                val payload = plain?.toString().orEmpty()
                val rec = character.toRecord().copy(migrationReviewPayload = payload).toDomain()
                character.copy(migrationReviews = rec.migrationReviews)
            }
            "unrecognizedFieldsPayload" -> {
                character.copy(unrecognizedFieldsJson = plain?.toString().orEmpty())
            }
            "itemCreationDraft" -> {
                val payload = plain?.toString().orEmpty()
                character.copy(itemCreationDraft = sharedConverters.stringToItemCreationDraft(payload))
            }
            "learnedKnowledges" -> character.copy(learnedKnowledges = decodeListViaGson<SpecialKnowledge>(plain))
            "transports" -> character.copy(transports = sharedConverters.stringToTransports(plain?.toString().orEmpty()))
            "familiars" -> character.copy(familiars = sharedConverters.stringToFamiliars(plain?.toString().orEmpty()))
            "arcaneKnowledges" -> character.copy(arcaneKnowledges = decodeListViaGson<SpecialKnowledge>(plain))
            "battleTechniques" -> character.copy(battleTechniques = decodeListViaGson<SpecialKnowledge>(plain))
            "progressionHistory" -> character.copy(progressionHistory = decodeListViaGson<ProgressionRecord>(plain))
            "money" -> if (target.op == TargetOp.DELTA && target.valueJson == null) {
                character
            } else {
                character.copy(money = asInt(plain) ?: character.money)
            }
            "life", "sanity", "arcane", "energy", "destiny", "exhaustion", "corruption" -> {
                val map = plain as? Map<*, *> ?: return character
                val res = ResourceValue(
                    current = asInt(map["current"]) ?: 0,
                    maximum = asInt(map["maximum"]) ?: 0,
                    adjustment = asInt(map["adjustment"]) ?: 0,
                )
                when (target.key) {
                    "life" -> character.copy(life = res)
                    "sanity" -> character.copy(sanity = res)
                    "arcane" -> character.copy(arcane = res)
                    "energy" -> character.copy(energy = res)
                    "destiny" -> character.copy(destiny = res)
                    "exhaustion" -> character.copy(exhaustion = res)
                    "corruption" -> character.copy(corruption = res)
                    else -> character
                }
            }
            else -> character
        }
    }
    if (target.kind == TargetKind.ITEM && target.op == TargetOp.TOMBSTONE) {
        return when {
            target.itemId.startsWith("abil_") -> {
                val id = decodeWireElementId("abil", target.itemId)
                character.copy(abilities = character.abilities.filterNot { it.id == id })
            }
            target.itemId.startsWith("cond_") -> {
                val id = decodeWireElementId("cond", target.itemId)
                character.copy(conditionInstances = character.conditionInstances.filterNot { it.instanceId.value == id })
            }
            target.itemId.startsWith("lcond_") -> {
                val id = decodeWireElementId("lcond", target.itemId)
                character.copy(conditions = character.conditions.filterNot { it.id == id })
            }
            target.itemId.startsWith("istate_") -> {
                val id = decodeWireElementId("istate", target.itemId)
                character.copy(itemStates = character.itemStates.filterNot { it.id.value == id })
            }
            else -> character
        }
    }
    if (target.kind == TargetKind.ITEM_FIELD) {
        val plain = jsonToPlainValue(raw)
        if (target.op == TargetOp.DELTA && target.itemId.startsWith("inv_") && target.key == "quantity" && target.valueJson != null) {
            val invId = decodeWireElementId("inv", target.itemId)
            val newQty = asInt(plain) ?: return character
            return character.copy(
                inventory = character.inventory.map { item ->
                    if (item.id == invId) item.copy(quantity = newQty) else item
                },
            )
        }
        if (target.itemId.startsWith("cond_") && target.key == "payload") {
            val instance = runCatching {
                sharedJson.decodeFromString(ConditionInstance.serializer(), sharedPlainGson.toJson(plain))
            }.getOrNull() ?: return character
            val existing = character.conditionInstances.any { it.instanceId == instance.instanceId }
            return character.copy(
                conditionInstances = if (existing) {
                    character.conditionInstances.map { if (it.instanceId == instance.instanceId) instance else it }
                } else {
                    character.conditionInstances + instance
                },
            )
        }
        if (target.itemId.startsWith("abil_")) {
            val id = decodeWireElementId("abil", target.itemId)
            val existing = character.abilities.firstOrNull { it.id == id }
                ?: Ability(
                    id = id,
                    name = id.ifBlank { "Habilidade" },
                    kind = com.kinderman.sdo.domain.model.AbilityKind.POWER,
                    cost = com.kinderman.sdo.domain.model.AbilityCost.PowerCost(0),
                )
            val updated = when (target.key) {
                "name" -> plain?.toString()?.takeIf { it.isNotBlank() }?.let { existing.copy(name = it) } ?: existing
                "effect" -> existing.copy(effect = plain?.toString().orEmpty())
                "favorite" -> existing.copy(favorite = plain as? Boolean ?: existing.favorite)
                "available" -> existing.copy(available = plain as? Boolean ?: existing.available)
                "cost" -> runCatching {
                    sharedJson.decodeFromString(
                        com.kinderman.sdo.domain.model.AbilityCost.serializer(),
                        sharedPlainGson.toJson(plain),
                    )
                }.getOrNull()?.let { existing.copy(cost = it) } ?: existing
                "action" -> runCatching {
                    sharedJson.decodeFromString(
                        com.kinderman.sdo.domain.model.Execution.serializer(),
                        sharedPlainGson.toJson(plain),
                    )
                }.getOrNull()?.let { existing.copy(execution = it) } ?: existing
                "range" -> runCatching {
                    sharedJson.decodeFromString(
                        com.kinderman.sdo.domain.model.RangeSpec.serializer(),
                        sharedPlainGson.toJson(plain),
                    )
                }.getOrNull()?.let { existing.copy(range = it) } ?: existing
                "duration" -> if (plain == null) {
                    existing.copy(duration = null)
                } else {
                    runCatching {
                        sharedJson.decodeFromString(
                            com.kinderman.sdo.domain.model.Duration.serializer(),
                            sharedPlainGson.toJson(plain),
                        )
                    }.getOrNull()?.let { existing.copy(duration = it) } ?: existing
                }
                "meta" -> {
                    val meta = (plain as? Map<*, *>)?.entries
                        ?.associate { it.key.toString() to it.value }
                        ?.toMutableMap()
                        ?: return character
                    val kindVal = runCatching {
                        com.kinderman.sdo.domain.model.AbilityKind.valueOf(meta["kind"]?.toString().orEmpty())
                    }.getOrDefault(existing.kind)
                    existing.copy(kind = kindVal)
                }
                else -> existing
            }
            val exists = character.abilities.any { it.id == id }
            return character.copy(
                abilities = if (exists) {
                    character.abilities.map { if (it.id == id) updated else it }
                } else {
                    character.abilities + updated
                },
            )
        }
    }
    return character
}

private inline fun <reified T> decodeListViaGson(plain: Any?): List<T> {
    val list = plain as? List<*> ?: return emptyList()
    val type = TypeToken.getParameterized(List::class.java, T::class.java).type
    return runCatching {
        sharedPlainGson.fromJson<List<T>>(sharedPlainGson.toJson(list), type)
    }.getOrDefault(emptyList())
}
