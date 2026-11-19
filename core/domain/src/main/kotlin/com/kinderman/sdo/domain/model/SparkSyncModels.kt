package com.kinderman.sdo.domain.model

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

enum class SheetIntentStatus {
    DRAFT,
    PENDING,
    SENDING,
    CONFIRMED,
    CONFLICT,
    REJECTED,
    ACCESS_DENIED,
    PAUSED_DEPENDENCY,
}

enum class TargetKind {
    SHEET,
    FIELD,
    ITEM,
    ITEM_FIELD,
}

enum class TargetOp {
    CREATE,
    SET,
    DELTA,
    TOMBSTONE,
    RESTORE,
    LOCK,
    TRANSFER,
    LINK,
    UNLINK,
    DELIVERY,
}

enum class OperationType {
    CREATE_SHEET,
    PATCH_FIELDS,
    UPSERT_ITEM,
    DELETE_ITEM,
    RESTORE_ITEM,
    ADJUST_RESOURCE,
    ITEM_CONSUME,
    SESSION_ACTION,
    SET_LOCK,
    DELETE_SHEET,
    RESTORE_SHEET,
    TRANSFER_OWNERSHIP,
    LINK_CAMPAIGN,
    UNLINK_CAMPAIGN,
    ACCEPT_DELIVERY,
    MIGRATE_V2,
}

enum class ConflictResolutionStatus {
    OPEN,
    RESOLVED_REMOTE,
    RESOLVED_LOCAL,
    RESOLVED_CUSTOM,
    DISCARDED,
}

enum class IdempotencyMatchResult {
    MATCHED_COMMITTED,
    HASH_MISMATCH,
    OUTCOME_NOT_COMMITTED,
}

@Serializable
data class SparkTargetSpec(
    val kind: TargetKind,
    val itemId: String = "",
    val key: String = "",
    val op: TargetOp,
    val expectedRevision: Long = 0L,
    val nextRevision: Long = expectedRevision + 1L,
    val delta: Int? = null,
    val baseValueJson: String? = null,
    val valueJson: String? = null,
    val minValue: Int? = null,
    val maxValue: Int? = null,
) {
    fun canonicalTargetKey(): String = when (kind) {
        TargetKind.SHEET -> if (key.isBlank()) "sheet" else "sheet:$key"
        TargetKind.FIELD -> "field:$key"
        TargetKind.ITEM -> "item:$itemId"
        TargetKind.ITEM_FIELD -> "item:$itemId/field:$key"
    }
}

@Serializable
data class SheetIntent(
    val operationId: String,
    val accountId: String,
    val sheetId: String,
    val type: OperationType,
    val targetKey: String,
    val targets: List<SparkTargetSpec> = emptyList(),
    val dependsOnOperationId: String? = null,
    val baseRevision: Long = 0L,
    val baseValueJson: String = "",
    val desiredPayloadJson: String = "",
    val payloadHash: String = computeCanonicalPayloadHash(
        sheetId = sheetId,
        type = type,
        targetKey = targetKey,
        targets = targets,
        desiredPayloadJson = desiredPayloadJson,
    ),
    val status: SheetIntentStatus = SheetIntentStatus.PENDING,
    val error: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    fun effectiveTargets(): List<SparkTargetSpec> {
        if (targets.isNotEmpty()) return targets
        return listOf(parseFallbackTarget(targetKey, type, baseRevision, desiredPayloadJson))
    }
}

@Serializable
data class OperationReceipt(
    val id: String,
    val actorUid: String,
    val type: OperationType,
    val outcome: String = "COMMITTED",
    val schemaVersion: Int = 2,
    val createdAt: Long = 0L,
    val targets: List<SparkTargetSpec> = emptyList(),
    val payloadHash: String = "",
    val campaignId: String? = null,
    val lockType: String? = null,
) {
    fun verifyAgainst(intent: SheetIntent): IdempotencyMatchResult {
        if (outcome != "COMMITTED") return IdempotencyMatchResult.OUTCOME_NOT_COMMITTED
        val expectedHash = payloadHash.ifBlank {
            computeCanonicalPayloadHash(
                sheetId = intent.sheetId,
                type = type,
                targetKey = intent.targetKey,
                targets = targets,
                desiredPayloadJson = intent.desiredPayloadJson,
            )
        }
        return if (type == intent.type && expectedHash == intent.payloadHash) {
            IdempotencyMatchResult.MATCHED_COMMITTED
        } else {
            IdempotencyMatchResult.HASH_MISMATCH
        }
    }
}

@Serializable
data class SheetConflictItem(
    val id: String,
    val accountId: String,
    val sheetId: String,
    val operationId: String,
    val targetKey: String,
    val label: String,
    val baseRevision: Long,
    val remoteRevision: Long,
    val baseSummary: String,
    val localSummary: String,
    val remoteSummary: String,
    val basePayloadJson: String,
    val localPayloadJson: String,
    val remotePayloadJson: String,
    val resolutionStatus: ConflictResolutionStatus = ConflictResolutionStatus.OPEN,
    val createdAt: Long = 0L,
)

data class AccountSheetSnapshot(
    val accountId: String,
    val sheetId: String,
    val sheetRevision: Long = 0L,
    val fieldRevisions: Map<String, Long> = emptyMap(),
    val confirmedCharacter: Character = Character(id = sheetId, dirty = false),
    val tombstonedItemIds: Set<String> = emptySet(),
    val sheetDeleted: Boolean = false,
    val localPhotoUri: String = "",
    val updatedAt: Long = 0L,
) {
    fun revisionFor(target: SparkTargetSpec): Long = when (target.kind) {
        TargetKind.SHEET -> sheetRevision
        TargetKind.FIELD -> fieldRevisions[target.canonicalTargetKey()]
            ?: fieldRevisions[target.key]
            ?: 0L
        TargetKind.ITEM -> fieldRevisions[target.canonicalTargetKey()]
            ?: fieldRevisions[target.itemId]
            ?: 0L
        TargetKind.ITEM_FIELD -> fieldRevisions[target.canonicalTargetKey()] ?: 0L
    }
}

data class OptimisticReplayResult(
    val projectedCharacter: Character,
    val intents: List<SheetIntent>,
    val conflicts: List<SheetConflictItem>,
)

const val MAX_SPARK_TARGETS_PER_OPERATION = 7
const val MAX_SPARK_ITEM_FIELD_TARGETS_PER_OPERATION = 5

fun isWithinSparkTransactionBudget(targets: List<SparkTargetSpec>): Boolean {
    if (targets.isEmpty() || targets.size > MAX_SPARK_TARGETS_PER_OPERATION) return false
    val hasItemField = targets.any { it.kind == TargetKind.ITEM_FIELD }
    return !hasItemField || targets.size <= MAX_SPARK_ITEM_FIELD_TARGETS_PER_OPERATION
}

/**
 * Rejects indivisible multi-effect actions exceeding Spark's Rules access budget before any
 * optimistic mutation rather than splitting them into partial commits.
 */
fun validateAtomicSparkActionBudget(targets: List<SparkTargetSpec>, actionLabel: String = "Ação") {
    if (!isWithinSparkTransactionBudget(targets)) {
        throw DomainError.ValidationError(
            "$actionLabel excede o orçamento atômico do Spark (${targets.size} alvos; " +
                "máximo $MAX_SPARK_TARGETS_PER_OPERATION campos ou $MAX_SPARK_ITEM_FIELD_TARGETS_PER_OPERATION subcampos de item). " +
                "Solicite ajuste direto do Mestre.",
        )
    }
}

private val sparkJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Computes a deterministic SHA-256 hex digest over canonicalized operation targets and payload.
 * Wall-clock timestamps and mutable client status fields are excluded so retries match exactly
 * while reusing an operationId for different content produces a distinct digest.
 */
fun computeCanonicalPayloadHash(
    sheetId: String,
    type: OperationType,
    targetKey: String,
    targets: List<SparkTargetSpec>,
    desiredPayloadJson: String = "",
): String {
    val canonicalTargets = targets
        .sortedWith(compareBy({ it.kind.name }, { it.itemId }, { it.key }, { it.op.name }))
        .map { target ->
            JsonObject(
                linkedMapOf(
                    "baseValue" to canonicalizeJsonOrString(target.baseValueJson.orEmpty()),
                    "delta" to (target.delta?.let(::JsonPrimitive) ?: JsonNull),
                    "itemId" to JsonPrimitive(target.itemId),
                    "key" to JsonPrimitive(target.key),
                    "kind" to JsonPrimitive(target.kind.name),
                    "op" to JsonPrimitive(target.op.name),
                    "value" to canonicalizeJsonOrString(target.valueJson.orEmpty()),
                ),
            )
        }
    val envelope = JsonObject(
        linkedMapOf(
            "desiredPayload" to canonicalizeJsonOrString(desiredPayloadJson),
            "sheetId" to JsonPrimitive(sheetId),
            "targetKey" to JsonPrimitive(targetKey),
            "targets" to JsonArray(canonicalTargets),
            "type" to JsonPrimitive(type.name),
        ),
    )
    val bytes = canonicalJsonString(envelope).toByteArray(Charsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}

private fun canonicalizeJsonOrString(raw: String): JsonElement {
    if (raw.isBlank()) return JsonPrimitive("")
    val parsed = runCatching { sparkJson.parseToJsonElement(raw) }.getOrNull() ?: return JsonPrimitive(raw)
    return sortJsonElement(parsed)
}

private fun sortJsonElement(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> JsonObject(
        element.entries.sortedBy { it.key }.associate { (k, v) -> k to sortJsonElement(v) },
    )
    is JsonArray -> JsonArray(element.map(::sortJsonElement))
    else -> element
}

private fun canonicalJsonString(element: JsonElement): String =
    sparkJson.encodeToString(JsonElement.serializer(), sortJsonElement(element))

fun List<SheetIntent>.toSyncStatus(
    isSyncing: Boolean = false,
    lastError: String? = null,
): SheetSyncStatus = SheetSyncStatus(
    pendingCount = count { it.status == SheetIntentStatus.PENDING },
    rejectedCount = count { it.status == SheetIntentStatus.REJECTED },
    isSyncing = isSyncing,
    lastError = lastError ?: firstOrNull {
        it.status in setOf(SheetIntentStatus.REJECTED, SheetIntentStatus.ACCESS_DENIED, SheetIntentStatus.CONFLICT) &&
            it.error.isNotBlank()
    }?.error,
    draftCount = count { it.status == SheetIntentStatus.DRAFT },
    sendingCount = count { it.status == SheetIntentStatus.SENDING },
    confirmedCount = count { it.status == SheetIntentStatus.CONFIRMED },
    conflictCount = count { it.status == SheetIntentStatus.CONFLICT },
    accessDeniedCount = count { it.status == SheetIntentStatus.ACCESS_DENIED },
    pausedCount = count { it.status == SheetIntentStatus.PAUSED_DEPENDENCY },
)

/**
 * Replays pending intents on top of the latest confirmed remote snapshot.
 * - Independent fields, items, and skills merge cleanly without clock-based arbitration.
 * - Absolute edits require expectedRevision == currentTargetRevision; mismatches yield conflicts.
 * - Remote tombstones prevent stale edits or silent resurrection.
 * - Resource/quantity DELTAs run against current remote balance and reject if out of bounds.
 * - Intents whose dependsOnOperationId is blocked transition to PAUSED_DEPENDENCY while
 *   independent intents continue normally.
 */
fun applyPendingIntentsOnConfirmed(
    confirmed: AccountSheetSnapshot,
    intents: List<SheetIntent>,
    receiptsByOperationId: Map<String, OperationReceipt> = emptyMap(),
): OptimisticReplayResult {
    var projected = confirmed.confirmedCharacter.copy(id = confirmed.sheetId, dirty = false)
    val workingRevisions = confirmed.fieldRevisions.toMutableMap()
    var sheetRev = confirmed.sheetRevision
    val workingTombstones = confirmed.tombstonedItemIds.toMutableSet()
    var workingSheetDeleted = confirmed.sheetDeleted
    val blockedOpIds = mutableSetOf<String>()
    val updatedIntents = ArrayList<SheetIntent>(intents.size)
    val conflicts = mutableListOf<SheetConflictItem>()

    for (intent in intents) {
        val receipt = receiptsByOperationId[intent.operationId]
        if (receipt != null) {
            when (receipt.verifyAgainst(intent)) {
                IdempotencyMatchResult.MATCHED_COMMITTED -> {
                    updatedIntents += intent.copy(status = SheetIntentStatus.CONFIRMED, error = "")
                    continue
                }
                IdempotencyMatchResult.HASH_MISMATCH -> {
                    blockedOpIds += intent.operationId
                    updatedIntents += intent.copy(
                        status = SheetIntentStatus.REJECTED,
                        error = "Idempotency mismatch: operationId reused with different payload.",
                    )
                    continue
                }
                IdempotencyMatchResult.OUTCOME_NOT_COMMITTED -> Unit
            }
        }

        when (intent.status) {
            SheetIntentStatus.CONFIRMED -> {
                updatedIntents += intent
                continue
            }
            SheetIntentStatus.DRAFT -> {
                updatedIntents += intent
                continue
            }
            SheetIntentStatus.CONFLICT,
            SheetIntentStatus.REJECTED,
            SheetIntentStatus.ACCESS_DENIED -> {
                blockedOpIds += intent.operationId
                updatedIntents += intent
                continue
            }
            SheetIntentStatus.PENDING,
            SheetIntentStatus.SENDING,
            SheetIntentStatus.PAUSED_DEPENDENCY -> Unit
        }

        val depId = intent.dependsOnOperationId
        if (!depId.isNullOrBlank() && depId in blockedOpIds) {
            blockedOpIds += intent.operationId
            updatedIntents += intent.copy(status = SheetIntentStatus.PAUSED_DEPENDENCY)
            continue
        }

        val activeStatus = if (intent.status == SheetIntentStatus.PAUSED_DEPENDENCY) {
            SheetIntentStatus.PENDING
        } else {
            intent.status
        }

        val effectiveTargets = intent.effectiveTargets()
        var stepCharacter = projected
        val stepRevisions = workingRevisions.toMutableMap()
        var stepSheetRev = sheetRev
        val stepTombstones = workingTombstones.toMutableSet()
        var stepSheetDeleted = workingSheetDeleted
        var failureStatus: SheetIntentStatus? = null
        var failureMessage = ""
        val stepConflicts = mutableListOf<SheetConflictItem>()

        for (target in effectiveTargets) {
            val targetKey = target.canonicalTargetKey()
            val currentRev = if (target.kind == TargetKind.SHEET) {
                stepSheetRev
            } else {
                stepRevisions[targetKey] ?: confirmed.revisionFor(target)
            }

            if (stepSheetDeleted && target.op != TargetOp.RESTORE) {
                failureStatus = SheetIntentStatus.CONFLICT
                failureMessage = "A ficha foi excluída remotamente."
                stepConflicts += buildConflictItem(
                    intent = intent,
                    target = target,
                    baseRev = target.expectedRevision,
                    remoteRev = currentRev,
                    remoteSummary = "Ficha excluída",
                    remoteJson = "{\"deleted\":true}",
                    localJson = target.valueJson ?: intent.desiredPayloadJson,
                )
                break
            }

            if ((target.kind == TargetKind.ITEM || target.kind == TargetKind.ITEM_FIELD) &&
                target.itemId in stepTombstones &&
                target.op != TargetOp.RESTORE
            ) {
                failureStatus = SheetIntentStatus.CONFLICT
                failureMessage = "Item ${target.itemId} foi excluído remotamente."
                stepConflicts += buildConflictItem(
                    intent = intent,
                    target = target,
                    baseRev = target.expectedRevision,
                    remoteRev = currentRev,
                    remoteSummary = "Excluído",
                    remoteJson = "{\"deleted\":true}",
                    localJson = target.valueJson ?: intent.desiredPayloadJson,
                )
                break
            }

            when (target.op) {
                TargetOp.DELTA -> {
                    val deltaResult = applyDeltaTarget(stepCharacter, target)
                    if (deltaResult.isFailure) {
                        failureStatus = SheetIntentStatus.REJECTED
                        failureMessage = deltaResult.exceptionOrNull()?.message
                            ?: "Saldo insuficiente para aplicar alteração."
                        break
                    }
                    stepCharacter = deltaResult.getOrThrow()
                    stepRevisions[targetKey] = currentRev + 1L
                }
                TargetOp.TOMBSTONE -> {
                    if (target.kind != TargetKind.SHEET && currentRev != target.expectedRevision) {
                        failureStatus = SheetIntentStatus.CONFLICT
                        failureMessage = "Revisão divergente ao excluir $targetKey."
                        stepConflicts += buildConflictItem(
                            intent = intent,
                            target = target,
                            baseRev = target.expectedRevision,
                            remoteRev = currentRev,
                            remoteSummary = summarizeTarget(stepCharacter, target),
                            remoteJson = readTargetJson(stepCharacter, target),
                            localJson = "{\"deleted\":true}",
                        )
                        break
                    }
                    if (target.kind == TargetKind.SHEET) {
                        stepSheetDeleted = true
                        stepSheetRev = maxOf(stepSheetRev + 1L, target.nextRevision)
                        stepCharacter = stepCharacter.copy(deleted = true)
                    } else if (target.kind == TargetKind.ITEM) {
                        stepTombstones += target.itemId
                        stepRevisions[targetKey] = target.nextRevision
                        stepCharacter = removeItemByWireId(stepCharacter, target.itemId)
                    }
                }
                TargetOp.RESTORE -> {
                    if (currentRev != target.expectedRevision) {
                        failureStatus = SheetIntentStatus.CONFLICT
                        failureMessage = "Revisão divergente ao restaurar $targetKey."
                        stepConflicts += buildConflictItem(
                            intent = intent,
                            target = target,
                            baseRev = target.expectedRevision,
                            remoteRev = currentRev,
                            remoteSummary = "Excluído",
                            remoteJson = "{\"deleted\":true}",
                            localJson = target.valueJson ?: intent.desiredPayloadJson,
                        )
                        break
                    }
                    if (target.kind == TargetKind.SHEET) {
                        stepSheetDeleted = false
                        stepSheetRev = target.nextRevision
                        stepCharacter = stepCharacter.copy(deleted = false)
                    } else if (target.kind == TargetKind.ITEM) {
                        stepTombstones -= target.itemId
                        stepRevisions[targetKey] = target.nextRevision
                        stepCharacter = applyTargetToCharacter(stepCharacter, target, intent.desiredPayloadJson)
                    }
                }
                TargetOp.CREATE,
                TargetOp.SET,
                TargetOp.LOCK,
                TargetOp.TRANSFER,
                TargetOp.LINK,
                TargetOp.UNLINK,
                TargetOp.DELIVERY -> {
                    if (currentRev != target.expectedRevision) {
                        failureStatus = SheetIntentStatus.CONFLICT
                        failureMessage = "Conflito de revisão em $targetKey (esperada ${target.expectedRevision}, remota $currentRev)."
                        stepConflicts += buildConflictItem(
                            intent = intent,
                            target = target,
                            baseRev = target.expectedRevision,
                            remoteRev = currentRev,
                            remoteSummary = summarizeTarget(stepCharacter, target),
                            remoteJson = readTargetJson(stepCharacter, target),
                            localJson = target.valueJson ?: intent.desiredPayloadJson,
                        )
                        break
                    }
                    stepCharacter = applyTargetToCharacter(stepCharacter, target, intent.desiredPayloadJson)
                    if (target.kind == TargetKind.SHEET) {
                        stepSheetRev = target.nextRevision
                    } else {
                        stepRevisions[targetKey] = target.nextRevision
                    }
                }
            }
        }

        if (failureStatus != null) {
            blockedOpIds += intent.operationId
            conflicts += stepConflicts
            updatedIntents += intent.copy(status = failureStatus, error = failureMessage)
        } else {
            projected = stepCharacter
            workingRevisions.clear()
            workingRevisions.putAll(stepRevisions)
            sheetRev = stepSheetRev
            workingTombstones.clear()
            workingTombstones.addAll(stepTombstones)
            workingSheetDeleted = stepSheetDeleted
            updatedIntents += intent.copy(status = activeStatus, error = "")
        }
    }

    return OptimisticReplayResult(
        projectedCharacter = projected,
        intents = updatedIntents,
        conflicts = conflicts,
    )
}

/**
 * Builds a new resolution intent anchored on [SheetConflictItem.remoteRevision], or returns null
 * when [useRemote] is true (accepting the remote value requires no new write).
 */
fun buildConflictResolutionIntent(
    conflict: SheetConflictItem,
    newOperationId: String,
    useRemote: Boolean,
    customValueJson: String? = null,
    createdAt: Long = 0L,
): SheetIntent? {
    if (useRemote) return null
    val chosenPayload = customValueJson ?: conflict.localPayloadJson
    val target = parseTargetFromConflict(conflict, chosenPayload)
    val type = when (target.kind) {
        TargetKind.SHEET -> OperationType.SET_LOCK
        TargetKind.FIELD -> OperationType.PATCH_FIELDS
        TargetKind.ITEM -> OperationType.UPSERT_ITEM
        TargetKind.ITEM_FIELD -> OperationType.PATCH_FIELDS
    }
    return SheetIntent(
        operationId = newOperationId,
        accountId = conflict.accountId,
        sheetId = conflict.sheetId,
        type = type,
        targetKey = conflict.targetKey,
        targets = listOf(target),
        dependsOnOperationId = null,
        baseRevision = conflict.remoteRevision,
        baseValueJson = conflict.remotePayloadJson,
        desiredPayloadJson = chosenPayload,
        status = SheetIntentStatus.PENDING,
        error = "",
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}

private fun parseTargetFromConflict(conflict: SheetConflictItem, chosenPayload: String): SparkTargetSpec {
    val parsed = parseFallbackTarget(
        targetKey = conflict.targetKey,
        type = OperationType.PATCH_FIELDS,
        baseRevision = conflict.remoteRevision,
        desiredPayloadJson = chosenPayload,
    )
    return parsed.copy(
        op = TargetOp.SET,
        expectedRevision = conflict.remoteRevision,
        nextRevision = conflict.remoteRevision + 1L,
        valueJson = chosenPayload,
    )
}

private fun parseFallbackTarget(
    targetKey: String,
    type: OperationType,
    baseRevision: Long,
    desiredPayloadJson: String,
): SparkTargetSpec {
    val op = when (type) {
        OperationType.DELETE_ITEM, OperationType.DELETE_SHEET -> TargetOp.TOMBSTONE
        OperationType.RESTORE_ITEM, OperationType.RESTORE_SHEET -> TargetOp.RESTORE
        OperationType.ADJUST_RESOURCE, OperationType.ITEM_CONSUME -> TargetOp.DELTA
        OperationType.SET_LOCK -> TargetOp.LOCK
        OperationType.TRANSFER_OWNERSHIP -> TargetOp.TRANSFER
        OperationType.LINK_CAMPAIGN -> TargetOp.LINK
        OperationType.UNLINK_CAMPAIGN -> TargetOp.UNLINK
        OperationType.ACCEPT_DELIVERY -> TargetOp.DELIVERY
        OperationType.CREATE_SHEET -> TargetOp.CREATE
        else -> TargetOp.SET
    }
    return when {
        targetKey.startsWith("item:") && "/field:" in targetKey -> {
            val itemId = targetKey.substringAfter("item:").substringBefore("/field:")
            val fieldKey = targetKey.substringAfter("/field:")
            SparkTargetSpec(
                kind = TargetKind.ITEM_FIELD,
                itemId = itemId,
                key = fieldKey,
                op = op,
                expectedRevision = baseRevision,
                nextRevision = baseRevision + 1L,
                valueJson = desiredPayloadJson,
            )
        }
        targetKey.startsWith("item:") -> SparkTargetSpec(
            kind = TargetKind.ITEM,
            itemId = targetKey.removePrefix("item:"),
            op = op,
            expectedRevision = baseRevision,
            nextRevision = baseRevision + 1L,
            valueJson = desiredPayloadJson,
        )
        targetKey.startsWith("field:") -> SparkTargetSpec(
            kind = TargetKind.FIELD,
            key = targetKey.removePrefix("field:"),
            op = op,
            expectedRevision = baseRevision,
            nextRevision = baseRevision + 1L,
            valueJson = desiredPayloadJson,
        )
        else -> SparkTargetSpec(
            kind = TargetKind.SHEET,
            key = targetKey.removePrefix("sheet:").takeUnless { it == "sheet" }.orEmpty(),
            op = op,
            expectedRevision = baseRevision,
            nextRevision = baseRevision + 1L,
            valueJson = desiredPayloadJson,
        )
    }
}

private fun buildConflictItem(
    intent: SheetIntent,
    target: SparkTargetSpec,
    baseRev: Long,
    remoteRev: Long,
    remoteSummary: String,
    remoteJson: String,
    localJson: String,
): SheetConflictItem {
    val targetKey = target.canonicalTargetKey()
    return SheetConflictItem(
        id = "${intent.operationId}::$targetKey",
        accountId = intent.accountId,
        sheetId = intent.sheetId,
        operationId = intent.operationId,
        targetKey = targetKey,
        label = target.key.ifBlank { target.itemId.ifBlank { "Ficha" } },
        baseRevision = baseRev,
        remoteRevision = remoteRev,
        baseSummary = intent.baseValueJson,
        localSummary = localJson,
        remoteSummary = remoteSummary,
        basePayloadJson = intent.baseValueJson,
        localPayloadJson = localJson,
        remotePayloadJson = remoteJson,
        resolutionStatus = ConflictResolutionStatus.OPEN,
        createdAt = intent.createdAt,
    )
}

private fun applyDeltaTarget(character: Character, target: SparkTargetSpec): Result<Character> {
    val delta = target.delta ?: 0
    return when (target.kind) {
        TargetKind.FIELD -> when (target.key) {
            "money" -> {
                val minVal = target.minValue ?: 0
                val nextVal = character.money + delta
                if (nextVal < minVal || (target.maxValue != null && nextVal > target.maxValue)) {
                    Result.failure(DomainError.InsufficientResource("Dinheiro insuficiente (atual=${character.money}, delta=$delta)."))
                } else {
                    Result.success(character.copy(money = nextVal))
                }
            }
            "life", "sanity", "arcane", "energy", "destiny", "exhaustion", "corruption" -> {
                val currentRes = character.resourceByKey(target.key)
                val minVal = target.minValue ?: 0
                val maxBound = target.maxValue ?: if (target.key == "corruption") 100 else null
                val nextCurrent = currentRes.current + delta
                if (nextCurrent < minVal || (maxBound != null && nextCurrent > maxBound)) {
                    Result.failure(
                        DomainError.InsufficientResource(
                            "Recurso ${target.key} insuficiente (atual=${currentRes.current}, delta=$delta).",
                        ),
                    )
                } else {
                    Result.success(character.withResourceByKey(target.key, currentRes.copy(current = nextCurrent)))
                }
            }
            else -> Result.success(character)
        }
        TargetKind.ITEM_FIELD -> {
            if ((target.key == "quantity" || target.key == "stack") && target.itemId.startsWith("inv_")) {
                val rawId = decodeWireElementId("inv", target.itemId)
                val item = character.inventory.firstOrNull { it.id == rawId }
                    ?: return Result.failure(DomainError.InvalidReference("Item $rawId não encontrado."))
                val minVal = target.minValue ?: 0
                val nextQty = item.quantity + delta
                if (nextQty < minVal || (target.maxValue != null && nextQty > target.maxValue)) {
                    Result.failure(
                        DomainError.InsufficientResource(
                            "Quantidade insuficiente para ${item.name} (atual=${item.quantity}, delta=$delta).",
                        ),
                    )
                } else {
                    Result.success(
                        character.copy(
                            inventory = character.inventory.map {
                                if (it.id == rawId) it.copy(quantity = nextQty) else it
                            },
                        ),
                    )
                }
            } else {
                Result.success(character)
            }
        }
        else -> Result.success(character)
    }
}

private fun Character.resourceByKey(key: String): ResourceValue = when (key) {
    "life" -> life
    "sanity" -> sanity
    "arcane" -> arcane
    "energy" -> energy
    "destiny" -> destiny
    "exhaustion" -> exhaustion
    "corruption" -> corruption
    else -> ResourceValue()
}

private fun Character.withResourceByKey(key: String, value: ResourceValue): Character = when (key) {
    "life" -> copy(life = value)
    "sanity" -> copy(sanity = value)
    "arcane" -> copy(arcane = value)
    "energy" -> copy(energy = value)
    "destiny" -> copy(destiny = value)
    "exhaustion" -> copy(exhaustion = value)
    "corruption" -> copy(corruption = value)
    else -> this
}

private fun removeItemByWireId(character: Character, wireItemId: String): Character = when {
    wireItemId.startsWith("inv_") -> {
        val id = decodeWireElementId("inv", wireItemId)
        character.copy(inventory = character.inventory.filterNot { it.id == id })
    }
    wireItemId.startsWith("note_") -> {
        val id = decodeWireElementId("note", wireItemId)
        character.copy(personalNotes = character.personalNotes.filterNot { it.id == id })
    }
    wireItemId.startsWith("pow_") -> {
        val id = decodeWireElementId("pow", wireItemId)
        character.copy(powers = character.powers.filterNot { it.id == id })
    }
    wireItemId.startsWith("mys_") -> {
        val id = decodeWireElementId("mys", wireItemId)
        character.copy(mysticAbilities = character.mysticAbilities.filterNot { it.id == id })
    }
    wireItemId.startsWith("abil_") -> {
        val id = decodeWireElementId("abil", wireItemId)
        character.copy(abilities = character.abilities.filterNot { it.id == id })
    }
    wireItemId.startsWith("lcond_") -> {
        val id = decodeWireElementId("lcond", wireItemId)
        character.copy(conditions = character.conditions.filterNot { it.id == id })
    }
    wireItemId.startsWith("cond_") -> {
        val id = decodeWireElementId("cond", wireItemId)
        character.copy(conditionInstances = character.conditionInstances.filterNot { it.instanceId.value == id })
    }
    wireItemId.startsWith("istate_") -> {
        val id = decodeWireElementId("istate", wireItemId)
        character.copy(itemStates = character.itemStates.filterNot { it.id.value == id })
    }
    else -> character
}

private fun applyTargetToCharacter(
    character: Character,
    target: SparkTargetSpec,
    fallbackPayloadJson: String,
): Character {
    val raw = target.valueJson ?: fallbackPayloadJson
    val jsonElem = runCatching { sparkJson.parseToJsonElement(raw) }.getOrNull()
    return when (target.kind) {
        TargetKind.SHEET -> applySheetTarget(character, target, jsonElem)
        TargetKind.FIELD -> applyFieldTarget(character, target.key, raw, jsonElem)
        TargetKind.ITEM -> applyItemRootTarget(character, target.itemId, jsonElem)
        TargetKind.ITEM_FIELD -> applyItemFieldTarget(character, target.itemId, target.key, raw, jsonElem)
    }
}

private fun applySheetTarget(character: Character, target: SparkTargetSpec, jsonElem: JsonElement?): Character {
    val obj = jsonElem as? JsonObject ?: return character
    return when (target.op) {
        TargetOp.LOCK -> {
            val lock = obj["lockType"]?.jsonPrimitive?.content?.let {
                runCatching { CharacterLock.valueOf(it) }.getOrNull()
            } ?: character.lockType
            character.copy(
                lockType = lock,
                lockedBy = obj["lockedBy"]?.jsonPrimitive?.content ?: character.lockedBy,
                lockedAt = obj["lockedAt"]?.jsonPrimitive?.longOrNull ?: character.lockedAt,
            )
        }
        TargetOp.TRANSFER -> character.copy(
            ownerId = obj["ownerId"]?.jsonPrimitive?.content ?: character.ownerId,
        )
        TargetOp.LINK, TargetOp.UNLINK -> character.copy(
            campaignId = obj["campaignId"]?.jsonPrimitive?.content ?: character.campaignId,
        )
        TargetOp.DELIVERY -> {
            val deliveryId = obj["deliveryId"]?.jsonPrimitive?.content.orEmpty()
            if (deliveryId.isBlank() || deliveryId in character.appliedDeliveryIds) character
            else character.copy(appliedDeliveryIds = character.appliedDeliveryIds + deliveryId)
        }
        else -> character
    }
}

private fun applyFieldTarget(
    character: Character,
    key: String,
    raw: String,
    jsonElem: JsonElement?,
): Character {
    fun strVal(): String = (jsonElem as? JsonPrimitive)?.content ?: raw
    fun intVal(default: Int): Int = (jsonElem as? JsonPrimitive)?.intOrNull ?: raw.toIntOrNull() ?: default
    fun strList(): List<String> = (jsonElem as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
    fun resVal(existing: ResourceValue): ResourceValue {
        val obj = jsonElem as? JsonObject ?: return existing
        return existing.copy(
            current = obj["current"]?.jsonPrimitive?.intOrNull ?: existing.current,
            maximum = obj["maximum"]?.jsonPrimitive?.intOrNull ?: existing.maximum,
            adjustment = obj["adjustment"]?.jsonPrimitive?.intOrNull ?: existing.adjustment,
        )
    }
    return when (key) {
        "name" -> character.copy(name = strVal())
        "race" -> character.copy(race = strVal())
        "subRace" -> character.copy(subRace = strVal())
        "raceAttribute" -> character.copy(raceAttribute = strVal())
        "occupation" -> character.copy(occupation = strVal())
        "height" -> character.copy(height = strVal())
        "age" -> character.copy(age = strVal())
        "sex" -> character.copy(sex = strVal())
        "size" -> character.copy(size = strVal())
        "avatarPreset" -> character.copy(photoUri = strVal())
        "pathName" -> character.copy(pathName = strVal())
        "pathMotto" -> character.copy(pathMotto = strVal())
        "story" -> character.copy(story = strVal())
        "agilityLimit" -> character.copy(agilityLimit = strVal())
        "level" -> character.copy(level = intVal(character.level).coerceIn(1, 20))
        "money" -> character.copy(money = intVal(character.money).coerceAtLeast(0))
        "creationStep" -> character.copy(creationStep = intVal(character.creationStep).coerceIn(1, 20))
        "life", "sanity", "arcane", "energy", "destiny", "exhaustion", "corruption" ->
            character.withResourceByKey(key, resVal(character.resourceByKey(key)))
        "positiveTraits" -> character.copy(positiveTraits = strList())
        "negativeTraits" -> character.copy(negativeTraits = strList())
        "pathKeywords" -> character.copy(pathKeywords = strList())
        "pathPillars" -> character.copy(pathPillars = strList())
        "protectionAdjustments" -> {
            val map = (jsonElem as? JsonObject)?.mapValues { (_, v) -> v.jsonPrimitive.intOrNull ?: 0 }
                ?: return character
            character.copy(protectionAdjustments = map)
        }
        "transports" -> character.copy(
            transports = runCatching {
                sparkJson.decodeFromString<List<CharacterTransport>>(strVal())
            }.getOrDefault(emptyList()),
        )
        "familiars" -> character.copy(
            familiars = runCatching {
                sparkJson.decodeFromString<List<CharacterFamiliar>>(strVal())
            }.getOrDefault(emptyList()),
        )
        else -> character
    }
}

private fun applyItemRootTarget(character: Character, wireItemId: String, jsonElem: JsonElement?): Character {
    val obj = jsonElem as? JsonObject
    return when {
        wireItemId.startsWith("inv_") -> {
            val id = decodeWireElementId("inv", wireItemId)
            val name = obj?.get("name")?.jsonPrimitive?.content ?: ""
            val qty = obj?.get("quantity")?.jsonPrimitive?.intOrNull ?: 1
            val existing = character.inventory.firstOrNull { it.id == id }
            if (existing == null) {
                character.copy(inventory = character.inventory + InventoryItem(id = id, name = name, quantity = qty))
            } else {
                character.copy(
                    inventory = character.inventory.map {
                        if (it.id == id) it.copy(name = name.ifBlank { it.name }, quantity = qty) else it
                    },
                )
            }
        }
        wireItemId.startsWith("note_") -> {
            val id = decodeWireElementId("note", wireItemId)
            val title = obj?.get("title")?.jsonPrimitive?.content ?: ""
            val text = obj?.get("text")?.jsonPrimitive?.content ?: ""
            val existing = character.personalNotes.firstOrNull { it.id == id }
            if (existing == null) {
                character.copy(personalNotes = character.personalNotes + PersonalNote(id = id, title = title, text = text))
            } else {
                character.copy(
                    personalNotes = character.personalNotes.map {
                        if (it.id == id) it.copy(title = title, text = text) else it
                    },
                )
            }
        }
        else -> character
    }
}

private fun applyItemFieldTarget(
    character: Character,
    wireItemId: String,
    fieldKey: String,
    raw: String,
    jsonElem: JsonElement?,
): Character {
    val obj = jsonElem as? JsonObject
    val prim = jsonElem as? JsonPrimitive
    fun strVal(default: String): String = prim?.content ?: raw
    fun intVal(default: Int): Int = prim?.intOrNull ?: raw.toIntOrNull() ?: default
    fun boolVal(default: Boolean): Boolean = prim?.booleanOrNull ?: raw.toBooleanStrictOrNull() ?: default

    return when {
        wireItemId.startsWith("inv_") -> {
            val id = decodeWireElementId("inv", wireItemId)
            val existing = character.inventory.firstOrNull { it.id == id } ?: InventoryItem(id = id)
            val updated = when (fieldKey) {
                "name" -> existing.copy(name = strVal(existing.name))
                "state" -> existing.copy(state = strVal(existing.state))
                "quantity" -> existing.copy(quantity = intVal(existing.quantity).coerceAtLeast(0))
                "durabilityCurrent" -> existing.copy(durabilityCurrent = intVal(existing.durabilityCurrent).coerceAtLeast(0))
                "durabilityMax" -> existing.copy(durabilityMax = intVal(existing.durabilityMax).coerceAtLeast(0))
                "itemCondition" -> existing.copy(
                    itemCondition = runCatching { ItemCondition.valueOf(strVal(existing.itemCondition.name)) }
                        .getOrDefault(existing.itemCondition),
                )
                "region" -> existing.copy(region = strVal(existing.region))
                "effect" -> existing.copy(effect = strVal(existing.effect))
                "load" -> existing.copy(load = intVal(existing.load).coerceAtLeast(0))
                "backpackCapacity" -> existing.copy(backpackCapacity = intVal(existing.backpackCapacity).coerceAtLeast(0))
                "agilityLimit" -> existing.copy(
                    agilityLimit = if (jsonElem is JsonNull || raw == "null") null else intVal(existing.agilityLimit ?: 0),
                )
                "quality" -> existing.copy(
                    quality = runCatching { ItemQuality.valueOf(strVal(existing.quality.name)) }
                        .getOrDefault(existing.quality),
                )
                "pg" -> existing.copy(pg = intVal(existing.pg))
                "pl" -> existing.copy(pl = intVal(existing.pl))
                "category" -> existing.copy(category = strVal(existing.category))
                "linkedAshId" -> existing.copy(linkedAshId = strVal(existing.linkedAshId))
                "ashPurity" -> existing.copy(
                    ashPurity = runCatching { AshPurity.valueOf(strVal(existing.ashPurity.name)) }
                        .getOrDefault(existing.ashPurity),
                )
                "enhancements" -> if (obj != null) existing.copy(
                    enhancementSlots = obj["enhancementSlots"]?.jsonPrimitive?.intOrNull ?: existing.enhancementSlots,
                    enhancementChargesCurrent = obj["enhancementChargesCurrent"]?.jsonPrimitive?.intOrNull
                        ?: existing.enhancementChargesCurrent,
                    enhancementChargesMax = obj["enhancementChargesMax"]?.jsonPrimitive?.intOrNull
                        ?: existing.enhancementChargesMax,
                ) else existing
                "meta" -> if (obj != null) existing.copy(
                    name = obj["name"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: existing.name,
                    state = obj["state"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: existing.state,
                    quantity = obj["quantity"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull?.coerceAtLeast(0)
                        ?: existing.quantity,
                    durabilityCurrent = obj["durabilityCurrent"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull?.coerceAtLeast(0)
                        ?: existing.durabilityCurrent,
                    durabilityMax = obj["durabilityMax"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull?.coerceAtLeast(0)
                        ?: existing.durabilityMax,
                    itemCondition = obj["itemCondition"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?.let { rawCond -> runCatching { ItemCondition.valueOf(rawCond) }.getOrNull() }
                        ?: existing.itemCondition,
                    region = obj["region"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: existing.region,
                    effect = obj["effect"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: existing.effect,
                    load = obj["load"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull?.coerceAtLeast(0)
                        ?: existing.load,
                    backpackCapacity = obj["backpackCapacity"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull?.coerceAtLeast(0)
                        ?: existing.backpackCapacity,
                    agilityLimit = if ("agilityLimit" in obj) {
                        obj["agilityLimit"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull
                    } else {
                        existing.agilityLimit
                    },
                    quality = obj["quality"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?.let { rawQual -> runCatching { ItemQuality.valueOf(rawQual) }.getOrNull() }
                        ?: existing.quality,
                    pg = obj["pg"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull ?: existing.pg,
                    pl = obj["pl"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull ?: existing.pl,
                    category = obj["category"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: existing.category,
                    linkedAshId = obj["linkedAshId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?: existing.linkedAshId,
                    ashPurity = obj["ashPurity"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?.let { rawPurity -> runCatching { AshPurity.valueOf(rawPurity) }.getOrNull() }
                        ?: existing.ashPurity,
                    enhancementSlots = obj["enhancementSlots"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull
                        ?: existing.enhancementSlots,
                    enhancementChargesCurrent = obj["enhancementChargesCurrent"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull
                        ?: existing.enhancementChargesCurrent,
                    enhancementChargesMax = obj["enhancementChargesMax"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull
                        ?: existing.enhancementChargesMax,
                    catalogEntryId = obj["catalogEntryId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?: existing.catalogEntryId,
                    catalogVersion = obj["catalogVersion"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull
                        ?: existing.catalogVersion,
                    baseId = obj["baseId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: existing.baseId,
                    materialId = obj["materialId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?: existing.materialId,
                    secondaryMaterialId = obj["secondaryMaterialId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?: existing.secondaryMaterialId,
                    modificationIds = (obj["modificationIds"] as? JsonArray)
                        ?.mapNotNull { it.takeUnless { el -> el is JsonNull }?.jsonPrimitive?.content }
                        ?: existing.modificationIds,
                    gemSlots = obj["gemSlots"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull ?: existing.gemSlots,
                    technologySlots = obj["technologySlots"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull
                        ?: existing.technologySlots,
                    acquisitionSource = obj["acquisitionSource"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?.let { rawSrc -> runCatching { ItemAcquisitionSource.valueOf(rawSrc) }.getOrNull() }
                        ?: existing.acquisitionSource,
                    heritageCost = if ("heritageCost" in obj) {
                        obj["heritageCost"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull
                    } else {
                        existing.heritageCost
                    },
                    purchasePrice = if ("purchasePrice" in obj) {
                        obj["purchasePrice"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull
                    } else {
                        existing.purchasePrice
                    },
                    acquiredAt = obj["acquiredAt"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.longOrNull
                        ?: existing.acquiredAt,
                    canonical = obj["canonical"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.booleanOrNull
                        ?: existing.canonical,
                    dataVersion = obj["dataVersion"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull
                        ?: existing.dataVersion,
                ) else existing
                else -> existing
            }
            val exists = character.inventory.any { it.id == id }
            character.copy(
                inventory = if (exists) {
                    character.inventory.map { if (it.id == id) updated else it }
                } else {
                    character.inventory + updated
                },
            )
        }
        wireItemId.startsWith("note_") -> {
            val id = decodeWireElementId("note", wireItemId)
            val existing = character.personalNotes.firstOrNull { it.id == id } ?: PersonalNote(id = id)
            val updated = when (fieldKey) {
                "title" -> existing.copy(title = strVal(existing.title))
                "text" -> existing.copy(text = strVal(existing.text))
                else -> existing
            }
            val exists = character.personalNotes.any { it.id == id }
            character.copy(
                personalNotes = if (exists) {
                    character.personalNotes.map { if (it.id == id) updated else it }
                } else {
                    character.personalNotes + updated
                },
            )
        }
        wireItemId.startsWith("pow_") -> {
            val id = decodeWireElementId("pow", wireItemId)
            val existing = character.powers.firstOrNull { it.id == id } ?: Power(id = id)
            val updated = when (fieldKey) {
                "name" -> existing.copy(name = strVal(existing.name))
                "cost" -> existing.copy(cost = strVal(existing.cost))
                "costType" -> existing.copy(
                    costType = runCatching { AbilityCostType.valueOf(strVal(existing.costType.name)) }
                        .getOrDefault(existing.costType),
                )
                "costValue" -> existing.copy(costValue = intVal(existing.costValue).coerceAtLeast(0))
                "action" -> existing.copy(action = strVal(existing.action))
                "range" -> existing.copy(range = strVal(existing.range))
                "duration" -> existing.copy(duration = strVal(existing.duration))
                "effect" -> existing.copy(effect = strVal(existing.effect))
                "active" -> existing.copy(active = boolVal(existing.active))
                "favorite" -> existing.copy(favorite = boolVal(existing.favorite))
                "available" -> existing.copy(available = boolVal(existing.available))
                "linkedItemId" -> existing.copy(linkedItemId = strVal(existing.linkedItemId))
                "meta" -> if (obj != null) existing.copy(
                    origin = obj["origin"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: existing.origin,
                    category = obj["category"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: existing.category,
                    limit = obj["limit"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: existing.limit,
                    sourceType = obj["sourceType"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?.let { rawSrc -> runCatching { PowerSourceType.valueOf(rawSrc) }.getOrNull() }
                        ?: existing.sourceType,
                    sourceId = obj["sourceId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: existing.sourceId,
                    catalogEntryId = obj["catalogEntryId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?: existing.catalogEntryId,
                    catalogVersion = obj["catalogVersion"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull
                        ?: existing.catalogVersion,
                    canonicalSource = obj["canonicalSource"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?.let { rawSrc -> runCatching { AbilitySource.valueOf(rawSrc) }.getOrNull() }
                        ?: existing.canonicalSource,
                    knowledgeId = obj["knowledgeId"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                        ?: existing.knowledgeId,
                    knowledgeLevel = if ("knowledgeLevel" in obj) {
                        obj["knowledgeLevel"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull
                    } else {
                        existing.knowledgeLevel
                    },
                ) else existing
                else -> existing
            }
            val exists = character.powers.any { it.id == id }
            character.copy(
                powers = if (exists) character.powers.map { if (it.id == id) updated else it } else character.powers + updated,
            )
        }
        wireItemId.startsWith("mys_") -> {
            val id = decodeWireElementId("mys", wireItemId)
            val existing = character.mysticAbilities.firstOrNull { it.id == id } ?: MysticAbility(id = id)
            val updated = when (fieldKey) {
                "name" -> existing.copy(name = strVal(existing.name))
                "cost" -> existing.copy(cost = strVal(existing.cost))
                "costType" -> existing.copy(
                    costType = runCatching { AbilityCostType.valueOf(strVal(existing.costType.name)) }
                        .getOrDefault(existing.costType),
                )
                "costValue" -> existing.copy(costValue = intVal(existing.costValue).coerceAtLeast(0))
                "action" -> existing.copy(action = strVal(existing.action))
                "range" -> existing.copy(range = strVal(existing.range))
                "duration" -> existing.copy(duration = strVal(existing.duration))
                "effect" -> existing.copy(effect = strVal(existing.effect))
                "favorite" -> existing.copy(favorite = boolVal(existing.favorite))
                "available" -> existing.copy(available = boolVal(existing.available))
                "linkedItemId" -> existing.copy(linkedInventoryItemId = strVal(existing.linkedInventoryItemId))
                else -> existing
            }
            val exists = character.mysticAbilities.any { it.id == id }
            character.copy(
                mysticAbilities = if (exists) character.mysticAbilities.map { if (it.id == id) updated else it } else character.mysticAbilities + updated,
            )
        }
        wireItemId.startsWith("attr_") -> {
            val acronym = decodeWireElementId("attr", wireItemId)
            character.copy(
                attributes = character.attributes.map { attr ->
                    if (!attr.acronym.equals(acronym, ignoreCase = true)) {
                        attr
                    } else when {
                        fieldKey == "name" -> attr.copy(name = strVal(attr.name))
                        fieldKey == "acronym" -> attr.copy(acronym = strVal(attr.acronym))
                        fieldKey == "value" -> attr.copy(value = intVal(attr.value))
                        fieldKey == "modifier" -> attr.copy(modifier = intVal(attr.modifier))
                        fieldKey.startsWith("skill_") &&
                            (fieldKey.endsWith("_value") || fieldKey.endsWith("_modifier") || fieldKey.endsWith("_name")) -> {
                            val prop = fieldKey.substringAfterLast('_')
                            val slug = fieldKey.removePrefix("skill_").substringBeforeLast('_')
                            val existingSkill = attr.skills.firstOrNull { encodeSkillSlug(it.name) == slug }
                            if (existingSkill != null) {
                                attr.copy(
                                    skills = attr.skills.map { skill ->
                                        if (encodeSkillSlug(skill.name) != slug) skill else when (prop) {
                                            "value" -> skill.copy(value = intVal(skill.value))
                                            "modifier" -> skill.copy(modifier = intVal(skill.modifier))
                                            "name" -> skill.copy(name = strVal(skill.name))
                                            else -> skill
                                        }
                                    },
                                )
                            } else {
                                val newSkill = when (prop) {
                                    "value" -> SkillValue(name = slug, value = intVal(0))
                                    "modifier" -> SkillValue(name = slug, modifier = intVal(0))
                                    else -> SkillValue(name = strVal(slug))
                                }
                                attr.copy(skills = attr.skills + newSkill)
                            }
                        }
                        else -> attr
                    }
                },
            )
        }
        else -> character
    }
}

private fun readTargetJson(character: Character, target: SparkTargetSpec): String = when (target.kind) {
    TargetKind.SHEET -> JsonObject(
        linkedMapOf(
            "ownerId" to JsonPrimitive(character.ownerId),
            "campaignId" to JsonPrimitive(character.campaignId),
            "lockType" to JsonPrimitive(character.lockType.name),
            "deleted" to JsonPrimitive(character.deleted),
        ),
    ).toString()
    TargetKind.FIELD -> when (target.key) {
        "name" -> JsonPrimitive(character.name).toString()
        "race" -> JsonPrimitive(character.race).toString()
        "subRace" -> JsonPrimitive(character.subRace).toString()
        "occupation" -> JsonPrimitive(character.occupation).toString()
        "story" -> JsonPrimitive(character.story).toString()
        "money" -> JsonPrimitive(character.money).toString()
        "level" -> JsonPrimitive(character.level).toString()
        "life", "sanity", "arcane", "energy", "destiny", "exhaustion", "corruption" -> {
            val res = character.resourceByKey(target.key)
            JsonObject(
                linkedMapOf(
                    "current" to JsonPrimitive(res.current),
                    "maximum" to JsonPrimitive(res.maximum),
                    "adjustment" to JsonPrimitive(res.adjustment),
                ),
            ).toString()
        }
        else -> ""
    }
    TargetKind.ITEM, TargetKind.ITEM_FIELD -> {
        if (target.itemId.startsWith("inv_")) {
            val id = decodeWireElementId("inv", target.itemId)
            val item = character.inventory.firstOrNull { it.id == id }
            if (item == null) "{}" else when (target.key) {
                "name" -> JsonPrimitive(item.name).toString()
                "state" -> JsonPrimitive(item.state).toString()
                "quantity" -> JsonPrimitive(item.quantity).toString()
                "durabilityCurrent" -> JsonPrimitive(item.durabilityCurrent).toString()
                "durabilityMax" -> JsonPrimitive(item.durabilityMax).toString()
                "itemCondition" -> JsonPrimitive(item.itemCondition.name).toString()
                "region" -> JsonPrimitive(item.region).toString()
                "effect" -> JsonPrimitive(item.effect).toString()
                "load" -> JsonPrimitive(item.load).toString()
                "backpackCapacity" -> JsonPrimitive(item.backpackCapacity).toString()
                "agilityLimit" -> item.agilityLimit?.let(::JsonPrimitive)?.toString() ?: "null"
                "quality" -> JsonPrimitive(item.quality.name).toString()
                "pg" -> JsonPrimitive(item.pg).toString()
                "pl" -> JsonPrimitive(item.pl).toString()
                "category" -> JsonPrimitive(item.category).toString()
                "linkedAshId" -> JsonPrimitive(item.linkedAshId).toString()
                "ashPurity" -> JsonPrimitive(item.ashPurity.name).toString()
                else -> JsonObject(
                    linkedMapOf(
                        "id" to JsonPrimitive(item.id),
                        "name" to JsonPrimitive(item.name),
                        "quantity" to JsonPrimitive(item.quantity),
                    ),
                ).toString()
            }
        } else if (target.itemId.startsWith("note_")) {
            val id = decodeWireElementId("note", target.itemId)
            val note = character.personalNotes.firstOrNull { it.id == id }
            when (target.key) {
                "title" -> JsonPrimitive(note?.title.orEmpty()).toString()
                "text" -> JsonPrimitive(note?.text.orEmpty()).toString()
                else -> JsonObject(
                    linkedMapOf(
                        "title" to JsonPrimitive(note?.title.orEmpty()),
                        "text" to JsonPrimitive(note?.text.orEmpty()),
                    ),
                ).toString()
            }
        } else if (target.itemId.startsWith("pow_")) {
            val id = decodeWireElementId("pow", target.itemId)
            val pow = character.powers.firstOrNull { it.id == id }
            if (pow == null) "{}" else when (target.key) {
                "name" -> JsonPrimitive(pow.name).toString()
                "cost" -> JsonPrimitive(pow.cost).toString()
                "costType" -> JsonPrimitive(pow.costType.name).toString()
                "costValue" -> JsonPrimitive(pow.costValue).toString()
                "action" -> JsonPrimitive(pow.action).toString()
                "range" -> JsonPrimitive(pow.range).toString()
                "duration" -> JsonPrimitive(pow.duration).toString()
                "effect" -> JsonPrimitive(pow.effect).toString()
                "active" -> JsonPrimitive(pow.active).toString()
                "favorite" -> JsonPrimitive(pow.favorite).toString()
                "available" -> JsonPrimitive(pow.available).toString()
                "linkedItemId" -> JsonPrimitive(pow.linkedItemId).toString()
                else -> JsonPrimitive(pow.name).toString()
            }
        } else if (target.itemId.startsWith("attr_")) {
            val acronym = decodeWireElementId("attr", target.itemId)
            val attr = character.attributes.firstOrNull { it.acronym.equals(acronym, ignoreCase = true) }
            if (attr == null) "{}" else when {
                target.key == "name" -> JsonPrimitive(attr.name).toString()
                target.key == "acronym" -> JsonPrimitive(attr.acronym).toString()
                target.key == "value" -> JsonPrimitive(attr.value).toString()
                target.key == "modifier" -> JsonPrimitive(attr.modifier).toString()
                target.key.startsWith("skill_") && target.key.contains('_') -> {
                    val prop = target.key.substringAfterLast('_')
                    val slug = target.key.removePrefix("skill_").substringBeforeLast('_')
                    val skill = attr.skills.firstOrNull { encodeSkillSlug(it.name) == slug }
                    when (prop) {
                        "value" -> JsonPrimitive(skill?.value ?: 0).toString()
                        "modifier" -> JsonPrimitive(skill?.modifier ?: 0).toString()
                        "name" -> JsonPrimitive(skill?.name.orEmpty()).toString()
                        else -> "{}"
                    }
                }
                else -> "{}"
            }
        } else "{}"
    }
}

private val skillSlugAllowedRegex = Regex("[^A-Za-z0-9_.-]")

fun encodeSkillSlug(skillName: String): String {
    val decomposed = Normalizer.normalize(skillName, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    val slug = decomposed.trim().replace(Regex("\\s+"), "_").replace(skillSlugAllowedRegex, "")
    val candidate = if (slug.isNotBlank() && slug.length <= 54) slug else {
        "b64_" + Base64.getUrlEncoder().withoutPadding().encodeToString(skillName.toByteArray(Charsets.UTF_8))
    }
    return candidate.take(54)
}

private fun summarizeTarget(character: Character, target: SparkTargetSpec): String = when (target.kind) {
    TargetKind.FIELD -> when (target.key) {
        "name" -> character.name
        "race" -> character.race
        "occupation" -> character.occupation
        "money" -> character.money.toString()
        "life", "sanity", "arcane", "energy", "destiny", "exhaustion", "corruption" -> {
            val res = character.resourceByKey(target.key)
            "${res.current}/${res.maximum}"
        }
        else -> readTargetJson(character, target)
    }
    else -> readTargetJson(character, target)
}

private val safeWireIdRegex = Regex("^[A-Za-z0-9_.:=-]{1,110}$")

fun encodeWireElementId(prefix: String, rawId: String): String {
    val clean = if (rawId.matches(safeWireIdRegex) && !rawId.startsWith("b64:")) {
        rawId
    } else {
        "b64:" + Base64.getUrlEncoder().withoutPadding().encodeToString(rawId.toByteArray(Charsets.UTF_8))
    }
    return "${prefix}_$clean"
}

fun decodeWireElementId(prefix: String, wireItemId: String): String {
    val suffix = wireItemId.removePrefix("${prefix}_")
    return if (suffix.startsWith("b64:")) {
        runCatching {
            String(Base64.getUrlDecoder().decode(suffix.removePrefix("b64:")), Charsets.UTF_8)
        }.getOrDefault(suffix)
    } else {
        suffix
    }
}
