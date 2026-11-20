package com.kinderman.sdo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SparkSyncDomainTest {

    private fun sampleSnapshot(
        sheetRevision: Long = 1L,
        fieldRevisions: Map<String, Long> = mapOf(
            "field:name" to 1L,
            "field:race" to 1L,
            "field:occupation" to 1L,
            "field:money" to 1L,
            "field:life" to 1L,
            "item:inv_potion-1" to 1L,
            "item:inv_potion-1/field:name" to 1L,
            "item:inv_potion-1/field:quantity" to 1L,
        ),
        character: Character = Character(
            id = "sheet-1",
            ownerId = "player-1",
            campaignId = "camp-1",
            name = "Ayla",
            race = "Humano",
            occupation = "Batedora",
            money = 50,
            life = ResourceValue(12, 20),
            destiny = ResourceValue(1, 5),
            inventory = listOf(
                InventoryItem(id = "potion-1", name = "Poção Menor", quantity = 2, load = 1),
            ),
            dirty = false,
        ),
        tombstonedItemIds: Set<String> = emptySet(),
    ) = AccountSheetSnapshot(
        accountId = "player-1",
        sheetId = "sheet-1",
        sheetRevision = sheetRevision,
        fieldRevisions = fieldRevisions,
        confirmedCharacter = character,
        tombstonedItemIds = tombstonedItemIds,
        updatedAt = 1_000L,
    )

    @Test
    fun twoSessionMergePreservesIndependentFields() {
        // GM already committed life = 8/20 (field:life rev 2), while player offline edited name & money at rev 1.
        val confirmedByGm = sampleSnapshot(
            sheetRevision = 2L,
            fieldRevisions = mapOf(
                "field:name" to 1L,
                "field:money" to 1L,
                "field:life" to 2L,
            ),
            character = sampleSnapshot().confirmedCharacter.copy(life = ResourceValue(8, 20)),
        )
        val playerIntent = SheetIntent(
            operationId = "op-player-1",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:name",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "name",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "\"Ayla Vael\"",
                ),
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "money",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "75",
                ),
            ),
            baseRevision = 1L,
            createdAt = 50L,
        )

        val replay = applyPendingIntentsOnConfirmed(confirmedByGm, listOf(playerIntent))

        assertTrue(replay.conflicts.isEmpty())
        assertEquals(SheetIntentStatus.PENDING, replay.intents.single().status)
        assertEquals("Ayla Vael", replay.projectedCharacter.name)
        assertEquals(75, replay.projectedCharacter.money)
        assertEquals(8, replay.projectedCharacter.life.current)
    }

    @Test
    fun sameFieldConflictPreservesBaseLocalRemoteAndResolutionUsesRemoteRevision() {
        val remote = sampleSnapshot(
            sheetRevision = 2L,
            fieldRevisions = mapOf("field:name" to 2L),
            character = sampleSnapshot().confirmedCharacter.copy(name = "Nome do Mestre", updatedAt = 10L),
        )
        val offlineEdit = SheetIntent(
            operationId = "op-name-conflict",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:name",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "name",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "\"Nome do Jogador\"",
                ),
            ),
            baseRevision = 1L,
            baseValueJson = "\"Ayla\"",
            desiredPayloadJson = "\"Nome do Jogador\"",
            createdAt = Long.MAX_VALUE / 2,
        )

        val replay = applyPendingIntentsOnConfirmed(remote, listOf(offlineEdit))

        assertEquals(SheetIntentStatus.CONFLICT, replay.intents.single().status)
        assertEquals("Nome do Mestre", replay.projectedCharacter.name)
        val conflict = replay.conflicts.single()
        assertEquals(1L, conflict.baseRevision)
        assertEquals(2L, conflict.remoteRevision)
        assertEquals("\"Ayla\"", conflict.basePayloadJson)
        assertEquals("\"Nome do Jogador\"", conflict.localPayloadJson)
        assertEquals("\"Nome do Mestre\"", conflict.remotePayloadJson)

        assertNull(buildConflictResolutionIntent(conflict, "op-res-remote", useRemote = true))

        val resolution = buildConflictResolutionIntent(
            conflict = conflict,
            newOperationId = "op-res-local",
            useRemote = false,
            customValueJson = "\"Ayla Unificada\"",
            createdAt = 2_000L,
        )!!
        assertEquals(2L, resolution.baseRevision)
        assertEquals(2L, resolution.targets.single().expectedRevision)
        assertEquals(3L, resolution.targets.single().nextRevision)
        assertEquals("\"Ayla Unificada\"", resolution.desiredPayloadJson)

        val afterResolution = applyPendingIntentsOnConfirmed(remote, listOf(resolution))
        assertTrue(afterResolution.conflicts.isEmpty())
        assertEquals("Ayla Unificada", afterResolution.projectedCharacter.name)
    }

    @Test
    fun itemPropertyMergePreservesIndependentPropertiesOfSameItem() {
        val remote = sampleSnapshot(
            sheetRevision = 2L,
            fieldRevisions = mapOf(
                "item:inv_potion-1" to 1L,
                "item:inv_potion-1/field:name" to 1L,
                "item:inv_potion-1/field:quantity" to 2L,
            ),
            character = sampleSnapshot().confirmedCharacter.copy(
                inventory = listOf(InventoryItem(id = "potion-1", name = "Poção Menor", quantity = 5, load = 1)),
            ),
        )
        val renameIntent = SheetIntent(
            operationId = "op-rename-item",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "item:inv_potion-1/field:name",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.ITEM_FIELD,
                    itemId = "inv_potion-1",
                    key = "name",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "\"Poção de Cura\"",
                ),
            ),
            baseRevision = 1L,
        )

        val replay = applyPendingIntentsOnConfirmed(remote, listOf(renameIntent))

        assertTrue(replay.conflicts.isEmpty())
        val item = replay.projectedCharacter.inventory.single()
        assertEquals("Poção de Cura", item.name)
        assertEquals(5, item.quantity)
    }

    @Test
    fun deletionTombstoneBlocksStaleEditAndPreventsResurrection() {
        val remoteTombstoned = sampleSnapshot(
            sheetRevision = 2L,
            fieldRevisions = mapOf("item:inv_potion-1" to 2L),
            character = sampleSnapshot().confirmedCharacter.copy(inventory = emptyList()),
            tombstonedItemIds = setOf("inv_potion-1"),
        )
        val staleEdit = SheetIntent(
            operationId = "op-stale-item-edit",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "item:inv_potion-1/field:name",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.ITEM_FIELD,
                    itemId = "inv_potion-1",
                    key = "name",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "\"Poção Ressuscitada\"",
                ),
            ),
            baseRevision = 1L,
        )

        val replay = applyPendingIntentsOnConfirmed(remoteTombstoned, listOf(staleEdit))

        assertEquals(SheetIntentStatus.CONFLICT, replay.intents.single().status)
        assertEquals("Excluído", replay.conflicts.single().remoteSummary)
        assertTrue(replay.projectedCharacter.inventory.isEmpty())
    }

    @Test
    fun lastResourceExhaustionRejectsOverdrawWhileValidDeltaAdjustsCurrentBalance() {
        val validRemote = sampleSnapshot(
            sheetRevision = 2L,
            fieldRevisions = mapOf("item:inv_potion-1/field:quantity" to 2L),
            character = sampleSnapshot().confirmedCharacter.copy(
                inventory = listOf(InventoryItem(id = "potion-1", name = "Poção", quantity = 3)),
            ),
        )
        val consumeOne = SheetIntent(
            operationId = "op-consume-1",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.ITEM_CONSUME,
            targetKey = "item:inv_potion-1/field:quantity",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.ITEM_FIELD,
                    itemId = "inv_potion-1",
                    key = "quantity",
                    op = TargetOp.DELTA,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    delta = -1,
                    minValue = 0,
                ),
            ),
            baseRevision = 1L,
        )

        val okReplay = applyPendingIntentsOnConfirmed(validRemote, listOf(consumeOne))
        assertEquals(SheetIntentStatus.PENDING, okReplay.intents.single().status)
        assertEquals(2, okReplay.projectedCharacter.inventory.single().quantity)

        val exhaustedRemote = validRemote.copy(
            confirmedCharacter = validRemote.confirmedCharacter.copy(
                inventory = listOf(InventoryItem(id = "potion-1", name = "Poção", quantity = 0)),
                destiny = ResourceValue(0, 5),
            ),
        )
        val consumeDestiny = SheetIntent(
            operationId = "op-consume-destiny",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.ADJUST_RESOURCE,
            targetKey = "field:destiny",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "destiny",
                    op = TargetOp.DELTA,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    delta = -1,
                    minValue = 0,
                    maxValue = 5,
                ),
            ),
            baseRevision = 1L,
        )

        val exhaustedReplay = applyPendingIntentsOnConfirmed(exhaustedRemote, listOf(consumeOne, consumeDestiny))
        assertEquals(listOf(SheetIntentStatus.REJECTED, SheetIntentStatus.REJECTED), exhaustedReplay.intents.map { it.status })
        assertEquals(0, exhaustedReplay.projectedCharacter.inventory.single().quantity)
        assertEquals(0, exhaustedReplay.projectedCharacter.destiny.current)
    }

    @Test
    fun dependencyPauseBlocksDependentsWhileIndependentTargetsContinue() {
        val remote = sampleSnapshot(
            sheetRevision = 2L,
            fieldRevisions = mapOf(
                "field:name" to 2L,
                "field:race" to 1L,
                "field:occupation" to 1L,
            ),
            character = sampleSnapshot().confirmedCharacter.copy(name = "Nome Remoto"),
        )
        val conflictedOp = SheetIntent(
            operationId = "op-1",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:name",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "name",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "\"Ayla II\"",
                ),
            ),
            baseRevision = 1L,
        )
        val dependentOp = SheetIntent(
            operationId = "op-2",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:race",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "race",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "\"Elfa\"",
                ),
            ),
            dependsOnOperationId = "op-1",
            baseRevision = 1L,
        )
        val independentOp = SheetIntent(
            operationId = "op-3",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:occupation",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "occupation",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "\"Guardiã\"",
                ),
            ),
            dependsOnOperationId = null,
            baseRevision = 1L,
        )

        val replay = applyPendingIntentsOnConfirmed(remote, listOf(conflictedOp, dependentOp, independentOp))

        assertEquals(SheetIntentStatus.CONFLICT, replay.intents[0].status)
        assertEquals(SheetIntentStatus.PAUSED_DEPENDENCY, replay.intents[1].status)
        assertEquals(SheetIntentStatus.PENDING, replay.intents[2].status)
        assertEquals("Humano", replay.projectedCharacter.race)
        assertEquals("Guardiã", replay.projectedCharacter.occupation)

        // Once op-1 is confirmed, op-2 unpauses cleanly on the next replay.
        val afterOp1Confirmed = applyPendingIntentsOnConfirmed(
            remote,
            listOf(
                replay.intents[0].copy(status = SheetIntentStatus.CONFIRMED, error = ""),
                replay.intents[1],
                replay.intents[2],
            ),
        )
        assertEquals(SheetIntentStatus.PENDING, afterOp1Confirmed.intents[1].status)
        assertEquals("Elfa", afterOp1Confirmed.projectedCharacter.race)
    }

    @Test
    fun clockSkewNeverSelectsWinner() {
        val remote = sampleSnapshot(
            sheetRevision = 2L,
            fieldRevisions = mapOf("field:name" to 2L, "field:occupation" to 1L),
            character = sampleSnapshot().confirmedCharacter.copy(name = "Remoto", updatedAt = 1L),
        )
        val skewedFutureSameField = SheetIntent(
            operationId = "op-future",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:name",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "name",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "\"Futuro\"",
                ),
            ),
            baseRevision = 1L,
            createdAt = 9_999_999_999_999L,
        )
        val skewedPastIndependent = SheetIntent(
            operationId = "op-past",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:occupation",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "occupation",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "\"Exploradora\"",
                ),
            ),
            baseRevision = 1L,
            createdAt = 1L,
        )

        val replay = applyPendingIntentsOnConfirmed(remote, listOf(skewedFutureSameField, skewedPastIndependent))
        assertEquals(SheetIntentStatus.CONFLICT, replay.intents[0].status)
        assertEquals(SheetIntentStatus.PENDING, replay.intents[1].status)
        assertEquals("Remoto", replay.projectedCharacter.name)
        assertEquals("Exploradora", replay.projectedCharacter.occupation)
    }

    @Test
    fun canonicalPayloadHashIsDeterministicAndDetectsIdempotencyMismatch() {
        val hashA = computeCanonicalPayloadHash(
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:life",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "life",
                    op = TargetOp.SET,
                    valueJson = """{"adjustment":0,"current":10,"maximum":20}""",
                ),
            ),
            desiredPayloadJson = """{"b":2,"a":1}""",
        )
        val hashB = computeCanonicalPayloadHash(
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:life",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "life",
                    op = TargetOp.SET,
                    valueJson = """{ "maximum" : 20, "current" : 10, "adjustment" : 0 }""",
                ),
            ),
            desiredPayloadJson = """{"a":1,"b":2}""",
        )
        val hashDifferent = computeCanonicalPayloadHash(
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:life",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "life",
                    op = TargetOp.SET,
                    valueJson = """{"adjustment":0,"current":9,"maximum":20}""",
                ),
            ),
            desiredPayloadJson = """{"a":1,"b":2}""",
        )

        assertEquals(64, hashA.length)
        assertEquals(hashA, hashB)
        assertNotEquals(hashA, hashDifferent)

        val intent = SheetIntent(
            operationId = "op-idem",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.ADJUST_RESOURCE,
            targetKey = "field:life",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "life",
                    op = TargetOp.DELTA,
                    delta = -2,
                ),
            ),
            payloadHash = hashA,
        )
        val matchingReceipt = OperationReceipt(
            id = "op-idem",
            actorUid = "player-1",
            type = OperationType.ADJUST_RESOURCE,
            outcome = "COMMITTED",
            payloadHash = hashA,
        )
        val mismatchedReceipt = matchingReceipt.copy(payloadHash = hashDifferent)

        assertEquals(IdempotencyMatchResult.MATCHED_COMMITTED, matchingReceipt.verifyAgainst(intent))
        assertEquals(IdempotencyMatchResult.HASH_MISMATCH, mismatchedReceipt.verifyAgainst(intent))

        val confirmedReplay = applyPendingIntentsOnConfirmed(
            sampleSnapshot(),
            listOf(intent),
            receiptsByOperationId = mapOf("op-idem" to matchingReceipt),
        )
        assertEquals(SheetIntentStatus.CONFIRMED, confirmedReplay.intents.single().status)
        // Confirmed receipt must NOT double-apply the delta.
        assertEquals(12, confirmedReplay.projectedCharacter.life.current)

        val mismatchReplay = applyPendingIntentsOnConfirmed(
            sampleSnapshot(),
            listOf(intent),
            receiptsByOperationId = mapOf("op-idem" to mismatchedReceipt),
        )
        assertEquals(SheetIntentStatus.REJECTED, mismatchReplay.intents.single().status)
    }

    @Test
    fun statusCountsAllEightStatesAndRetryPreservesBaseRevision() {
        val intents = SheetIntentStatus.entries.mapIndexed { idx, status ->
            SheetIntent(
                operationId = "op-$idx",
                accountId = "acc-1",
                sheetId = "sheet-1",
                type = OperationType.PATCH_FIELDS,
                targetKey = "field:name",
                baseRevision = 4L,
                status = status,
                error = if (status == SheetIntentStatus.ACCESS_DENIED) "Sem permissão" else "",
            )
        }
        val summary = intents.toSyncStatus(isSyncing = true)

        assertEquals(1, summary.draftCount)
        assertEquals(1, summary.pendingCount)
        assertEquals(1, summary.sendingCount)
        assertEquals(1, summary.confirmedCount)
        assertEquals(1, summary.conflictCount)
        assertEquals(1, summary.rejectedCount)
        assertEquals(1, summary.accessDeniedCount)
        assertEquals(1, summary.pausedCount)
        assertTrue(summary.isSyncing)
        assertTrue(summary.hasActionRequired)
        assertEquals(7, summary.totalQueuedCount)

        val denied = intents.first { it.status == SheetIntentStatus.ACCESS_DENIED }
        val retried = denied.retryAsPending(now = 42L)
        assertEquals(SheetIntentStatus.PENDING, retried.status)
        assertEquals(4L, retried.baseRevision)
        assertEquals("", retried.error)
        assertFalse(retried.payloadHash.isBlank())
    }

    @Test
    fun attributeSkillValueAndModifierMergeIndependentlyAndAtomicBudgetGuardRejectsOversizedActions() {
        val remote = sampleSnapshot(
            sheetRevision = 2L,
            fieldRevisions = mapOf(
                "item:attr_CAR/field:skill_Labia_value" to 2L,
                "item:attr_CAR/field:skill_Labia_modifier" to 1L,
            ),
            character = sampleSnapshot().confirmedCharacter.copy(
                attributes = defaultAttributes().map { attr ->
                    if (attr.acronym == "CAR") {
                        attr.copy(skills = attr.skills.map { if (it.name == "Lábia") it.copy(value = 3) else it })
                    } else attr
                },
            ),
        )
        val modIntent = SheetIntent(
            operationId = "op-skill-mod",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "item:attr_CAR/field:skill_Labia_modifier",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.ITEM_FIELD,
                    itemId = "attr_CAR",
                    key = "skill_Labia_modifier",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "2",
                ),
            ),
            baseRevision = 1L,
        )
        val replay = applyPendingIntentsOnConfirmed(remote, listOf(modIntent))
        assertTrue(replay.conflicts.isEmpty())
        val labia = replay.projectedCharacter.attributes.first { it.acronym == "CAR" }.skills.first { it.name == "Lábia" }
        assertEquals(3, labia.value)
        assertEquals(2, labia.modifier)

        val tooManyFields = (1..8).map { idx ->
            SparkTargetSpec(kind = TargetKind.FIELD, key = "f$idx", op = TargetOp.SET)
        }
        assertFalse(isWithinSparkTransactionBudget(tooManyFields))
        assertTrue(isWithinSparkTransactionBudget(tooManyFields.take(7)))

        val tooManyItemFields = (1..6).map { idx ->
            SparkTargetSpec(kind = TargetKind.ITEM_FIELD, itemId = "inv_1", key = "k$idx", op = TargetOp.SET)
        }
        assertFalse(isWithinSparkTransactionBudget(tooManyItemFields))
        assertTrue(isWithinSparkTransactionBudget(tooManyItemFields.take(5)))

        val rejected = runCatching { validateAtomicSparkActionBudget(tooManyItemFields, "Consumo composto") }
        assertTrue(rejected.exceptionOrNull() is DomainError.ValidationError)
    }

    @Test
    fun identicalRemoteValueConvergesWithoutConflict() {
        val remote = sampleSnapshot(
            sheetRevision = 3L,
            fieldRevisions = mapOf("field:occupation" to 3L),
            character = sampleSnapshot().confirmedCharacter.copy(occupation = "Cronista"),
        )
        val staleSameValue = SheetIntent(
            operationId = "op-same-value",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:occupation",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "occupation",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    valueJson = "\"Cronista\"",
                ),
            ),
            baseRevision = 1L,
        )

        val replay = applyPendingIntentsOnConfirmed(remote, listOf(staleSameValue))

        assertTrue(replay.conflicts.isEmpty())
        assertEquals(SheetIntentStatus.PENDING, replay.intents.single().status)
        assertEquals("Cronista", replay.projectedCharacter.occupation)
    }
}
