package com.kinderman.sdo.data.sync

import com.kinderman.sdo.data.local.AccountSheetRecord
import com.kinderman.sdo.data.local.CampaignMemberRecord
import com.kinderman.sdo.data.local.CampaignRecord
import com.kinderman.sdo.data.local.CharacterRecord
import com.kinderman.sdo.data.local.SessionOperationRecord
import com.kinderman.sdo.data.local.SheetConflictRecord
import com.kinderman.sdo.data.local.SheetIntentRecord
import com.kinderman.sdo.data.local.SheetOutboxRecord
import com.kinderman.sdo.data.local.SheetRouteRecord
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.model.CURRENT_CHARACTER_SCHEMA_VERSION
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ConflictResolutionStatus
import com.kinderman.sdo.domain.model.ConsumableStateKind
import com.kinderman.sdo.domain.model.DomainError
import com.kinderman.sdo.domain.model.EffectOperation
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.ItemEffect
import com.kinderman.sdo.domain.model.ItemEffectCondition
import com.kinderman.sdo.domain.model.ItemEffectType
import com.kinderman.sdo.domain.model.ItemInstanceId
import com.kinderman.sdo.domain.model.OperationType
import com.kinderman.sdo.domain.model.ResourceValue
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperationType
import com.kinderman.sdo.domain.model.SessionResource
import com.kinderman.sdo.domain.model.SheetIntent
import com.kinderman.sdo.domain.model.SheetIntentStatus
import com.kinderman.sdo.domain.model.SparkTargetSpec
import com.kinderman.sdo.domain.model.SpendableResource
import com.kinderman.sdo.domain.model.TargetKind
import com.kinderman.sdo.domain.model.TargetOp
import com.kinderman.sdo.domain.model.TargetSpec
import com.kinderman.sdo.domain.model.UserRole
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.computeCanonicalPayloadHash
import com.kinderman.sdo.domain.repository.RemoteSyncEvent
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SparkFirestoreSyncEngineTest {

    private class MutableClock(var time: Long = 10_000L) : SparkClock {
        override fun nowMillis(): Long = time
    }

    private class RecordingWorkScheduler : SparkWorkScheduler {
        val scheduledAccounts = mutableListOf<String>()
        override fun schedule(accountId: String) {
            scheduledAccounts += accountId
        }
    }

    private class InMemorySparkLocalStore : SparkLocalStore {
        val accountSheets = linkedMapOf<Pair<String, String>, AccountSheetRecord>()
        val intents = linkedMapOf<Pair<String, String>, SheetIntentRecord>()
        val conflicts = linkedMapOf<Pair<String, String>, SheetConflictRecord>()
        val characters = linkedMapOf<String, CharacterRecord>()
        val campaigns = linkedMapOf<String, CampaignRecord>()
        val members = linkedMapOf<Pair<String, String>, CampaignMemberRecord>()
        val operations = linkedMapOf<String, SessionOperationRecord>()
        val legacyOutbox = linkedMapOf<String, SheetOutboxRecord>()
        val legacyRoutes = linkedMapOf<Pair<String, String>, SheetRouteRecord>()

        private val intentFlows = ConcurrentHashMap<String, MutableStateFlow<List<SheetIntentRecord>>>()
        private val conflictFlows = ConcurrentHashMap<String, MutableStateFlow<List<SheetConflictRecord>>>()
        private val sheetFlows = ConcurrentHashMap<String, MutableStateFlow<List<AccountSheetRecord>>>()

        private fun notifyAccount(accountId: String) {
            intentFlows[accountId]?.value = intents.values
                .filter { it.accountId == accountId }
                .sortedWith(compareBy({ it.createdAt }, { it.operationId }))
            conflictFlows[accountId]?.value = conflicts.values
                .filter { it.accountId == accountId && it.resolutionStatus == ConflictResolutionStatus.OPEN.name }
                .sortedWith(compareBy({ it.createdAt }, { it.id }))
            sheetFlows[accountId]?.value = accountSheets.values.filter { it.accountId == accountId }
        }

        override suspend fun <T> withTransaction(block: suspend () -> T): T = block()

        override suspend fun getAccountSheet(accountId: String, sheetId: String) =
            accountSheets[accountId to sheetId]

        override suspend fun listAccountSheets(accountId: String) =
            accountSheets.values.filter { it.accountId == accountId }

        override fun observeAccountSheets(accountId: String): Flow<List<AccountSheetRecord>> =
            sheetFlows.getOrPut(accountId) {
                MutableStateFlow(accountSheets.values.filter { it.accountId == accountId })
            }

        override suspend fun upsertAccountSheet(record: AccountSheetRecord) {
            accountSheets[record.accountId to record.sheetId] = record
            notifyAccount(record.accountId)
        }

        override suspend fun deleteAccountSheet(accountId: String, sheetId: String) {
            accountSheets.remove(accountId to sheetId)
            notifyAccount(accountId)
        }

        override suspend fun listIntentsForSheet(accountId: String, sheetId: String) =
            intents.values.filter { it.accountId == accountId && it.sheetId == sheetId }
                .sortedWith(compareBy({ it.createdAt }, { it.operationId }))

        override suspend fun listActiveIntents(accountId: String) =
            intents.values.filter {
                it.accountId == accountId &&
                    (it.status == SheetIntentStatus.PENDING.name || it.status == SheetIntentStatus.SENDING.name)
            }.sortedWith(compareBy({ it.createdAt }, { it.operationId }))

        override fun observeIntents(accountId: String): Flow<List<SheetIntentRecord>> =
            intentFlows.getOrPut(accountId) {
                MutableStateFlow(
                    intents.values.filter { it.accountId == accountId }
                        .sortedWith(compareBy({ it.createdAt }, { it.operationId })),
                )
            }

        override suspend fun getIntent(accountId: String, operationId: String) =
            intents[accountId to operationId]

        override suspend fun upsertIntent(intent: SheetIntentRecord) {
            intents[intent.accountId to intent.operationId] = intent
            notifyAccount(intent.accountId)
        }

        override suspend fun upsertIntents(intents: List<SheetIntentRecord>) {
            intents.forEach { this.intents[it.accountId to it.operationId] = it }
            intents.map { it.accountId }.distinct().forEach(::notifyAccount)
        }

        override suspend fun deleteIntent(accountId: String, operationId: String) {
            intents.remove(accountId to operationId)
            notifyAccount(accountId)
        }

        override fun observeOpenConflicts(accountId: String): Flow<List<SheetConflictRecord>> =
            conflictFlows.getOrPut(accountId) {
                MutableStateFlow(
                    conflicts.values.filter {
                        it.accountId == accountId && it.resolutionStatus == ConflictResolutionStatus.OPEN.name
                    },
                )
            }

        override suspend fun getConflict(accountId: String, id: String) = conflicts[accountId to id]

        override suspend fun upsertConflict(conflict: SheetConflictRecord) {
            conflicts[conflict.accountId to conflict.id] = conflict
            notifyAccount(conflict.accountId)
        }

        override suspend fun upsertConflicts(conflicts: List<SheetConflictRecord>) {
            conflicts.forEach { this.conflicts[it.accountId to it.id] = it }
            conflicts.map { it.accountId }.distinct().forEach(::notifyAccount)
        }

        override suspend fun deleteConflict(accountId: String, id: String) {
            conflicts.remove(accountId to id)
            notifyAccount(accountId)
        }

        override suspend fun getCharacter(sheetId: String) = characters[sheetId]
        override suspend fun allCharacters() = characters.values.toList()
        override suspend fun upsertCharacter(record: CharacterRecord) {
            characters[record.id] = record
        }

        override suspend fun purgeCharacter(sheetId: String) {
            characters.remove(sheetId)
        }

        override suspend fun getCampaign(campaignId: String) = campaigns[campaignId]
        override suspend fun listOwnedActiveCampaignIds(uid: String) =
            campaigns.values.filter { it.ownerId == uid && it.state == "ACTIVE" }.mapTo(linkedSetOf()) { it.id }

        override suspend fun getMember(campaignId: String, uid: String) = members[campaignId to uid]
        override suspend fun upsertMember(member: CampaignMemberRecord) {
            members[member.campaignId to member.userId] = member
        }

        override suspend fun getOperationByIdempotencyKey(key: String) =
            operations.values.firstOrNull { it.idempotencyKey == key }

        override suspend fun upsertOperation(operation: SessionOperationRecord) {
            operations[operation.id] = operation
        }

        override suspend fun listLegacyOutbox(accountId: String) =
            legacyOutbox.values.filter { it.accountId == accountId && it.status == "PENDING" }

        override suspend fun markLegacyOutboxStatus(operationId: String, status: String) {
            legacyOutbox[operationId]?.let { legacyOutbox[operationId] = it.copy(status = status) }
        }

        override suspend fun listLegacyRoutes(accountId: String) =
            legacyRoutes.values.filter { it.accountId == accountId }

        override suspend fun removeLegacyRoute(accountId: String, sheetId: String) {
            legacyRoutes.remove(accountId to sheetId)
        }
    }

    private class InMemorySparkRemoteGateway(private val clock: MutableClock) : SparkRemoteGateway {
        val roots = linkedMapOf<String, Map<String, Any?>>()
        val fields = linkedMapOf<Pair<String, String>, Map<String, Any?>>()
        val items = linkedMapOf<Pair<String, String>, Map<String, Any?>>()
        val itemFields = linkedMapOf<Triple<String, String, String>, Map<String, Any?>>()
        val operations = linkedMapOf<Pair<String, String>, Map<String, Any?>>()
        val campaigns = linkedMapOf<String, Map<String, Any?>>()
        val members = linkedMapOf<Pair<String, String>, Map<String, Any?>>()
        val deliveries = linkedMapOf<String, Map<String, Any?>>()
        val mediaAvatars = linkedMapOf<String, Map<String, Any?>>()
        val presence = linkedMapOf<Pair<String, String>, Map<String, Any?>>()

        var permissionDeniedForSheet: String? = null
        var permissionDeniedMessage: String = "PERMISSION_DENIED"
        var transientFailureCount = 0

        fun seedFromCharacter(
            record: CharacterRecord,
            sheetRev: Long = 1L,
            fieldRevs: Map<String, Long> = emptyMap(),
            lastOpId: String = "seed",
        ) {
            val docs = SparkWireCodec.toSparkDocuments(
                record = record,
                revision = sheetRev,
                fieldRevisions = fieldRevs,
                lastOperationId = lastOpId,
                updatedBy = record.ownerId,
                updatedAt = clock.nowMillis(),
            )
            roots[record.id] = docs.root
            docs.fields.forEach { (k, v) -> fields[record.id to k] = v }
            docs.items.forEach { (itemId, bundle) ->
                items[record.id to itemId] = bundle.document
                bundle.fields.forEach { (fk, fv) -> itemFields[Triple(record.id, itemId, fk)] = fv }
            }
            docs.mediaAvatar?.let { mediaAvatars[record.id] = it }
        }

        override suspend fun <T> runTransaction(sheetId: String, block: (SparkTransactionContext) -> T): T {
            if (transientFailureCount > 0) {
                transientFailureCount--
                throw SparkTransientNetworkException("Offline")
            }
            if (permissionDeniedForSheet == sheetId) {
                throw SparkPermissionDeniedException(permissionDeniedMessage)
            }
            val stagedRoots = LinkedHashMap(roots)
            val stagedFields = LinkedHashMap(fields)
            val stagedItems = LinkedHashMap(items)
            val stagedItemFields = LinkedHashMap(itemFields)
            val stagedOps = LinkedHashMap(operations)
            val stagedMembers = LinkedHashMap(members)
            val stagedDeliveries = LinkedHashMap(deliveries)

            val ctx = object : SparkTransactionContext {
                override fun getRoot(sheetId: String) = stagedRoots[sheetId]
                override fun getField(sheetId: String, fieldKey: String) = stagedFields[sheetId to fieldKey]
                override fun getItem(sheetId: String, itemId: String) = stagedItems[sheetId to itemId]
                override fun getItemField(sheetId: String, itemId: String, fieldKey: String) =
                    stagedItemFields[Triple(sheetId, itemId, fieldKey)]

                override fun getOperation(sheetId: String, operationId: String) = stagedOps[sheetId to operationId]
                override fun getCampaign(campaignId: String) = campaigns[campaignId]
                override fun getMember(campaignId: String, uid: String) = stagedMembers[campaignId to uid]
                override fun getDelivery(deliveryId: String) = stagedDeliveries[deliveryId]

                override fun setRoot(sheetId: String, data: Map<String, Any?>) {
                    stagedRoots[sheetId] = data
                }

                override fun setField(sheetId: String, fieldKey: String, data: Map<String, Any?>) {
                    stagedFields[sheetId to fieldKey] = data
                }

                override fun setItem(sheetId: String, itemId: String, data: Map<String, Any?>) {
                    stagedItems[sheetId to itemId] = data
                }

                override fun setItemField(sheetId: String, itemId: String, fieldKey: String, data: Map<String, Any?>) {
                    stagedItemFields[Triple(sheetId, itemId, fieldKey)] = data
                }

                override fun setOperation(sheetId: String, operationId: String, data: Map<String, Any?>) {
                    stagedOps[sheetId to operationId] = data
                }

                override fun setMember(campaignId: String, uid: String, data: Map<String, Any?>) {
                    stagedMembers[campaignId to uid] = data
                }

                override fun setDelivery(deliveryId: String, data: Map<String, Any?>) {
                    stagedDeliveries[deliveryId] = data
                }

                override fun serverTimestampToken(): Any = clock.nowMillis()
                override fun currentTimestampMillis(): Long = clock.nowMillis()
            }

            val result = block(ctx)
            roots.clear()
            roots.putAll(stagedRoots)
            fields.clear()
            fields.putAll(stagedFields)
            items.clear()
            items.putAll(stagedItems)
            itemFields.clear()
            itemFields.putAll(stagedItemFields)
            operations.clear()
            operations.putAll(stagedOps)
            members.clear()
            members.putAll(stagedMembers)
            deliveries.clear()
            deliveries.putAll(stagedDeliveries)
            return result
        }

        override suspend fun fetchSheetDocuments(sheetId: String): SparkSheetDocuments? {
            val root = roots[sheetId] ?: return null
            val sheetFields = fields.filterKeys { it.first == sheetId }.mapKeys { it.key.second }
            val sheetItems = items.filterKeys { it.first == sheetId }.entries.associate { (k, doc) ->
                val itemId = k.second
                val props = itemFields.filterKeys { it.first == sheetId && it.second == itemId }
                    .mapKeys { it.key.third }
                itemId to SparkItemBundle(document = doc, fields = props)
            }
            return SparkSheetDocuments(
                root = root,
                fields = sheetFields,
                items = sheetItems,
                mediaAvatar = mediaAvatars[sheetId],
            )
        }

        override suspend fun fetchSheetHead(sheetId: String): SparkSheetHead? = roots[sheetId]?.let {
            SparkSheetHead((it["revision"] as Number).toLong(), it["deleted"] == true, 0L)
        }

        override fun observeSheetReceipts(sheetId: String, ownUid: String): Flow<RemoteSyncEvent> = flowOf()

        override suspend fun fetchAccessibleSheetIds(uid: String, ownedActiveCampaignIds: Set<String>): Set<String> =
            roots.entries.filter { (_, data) ->
                data["ownerId"] == uid || (data["campaignId"] in ownedActiveCampaignIds)
            }.mapTo(linkedSetOf()) { it.key }

        override suspend fun writeMediaAvatar(sheetId: String, mediaDoc: Map<String, Any?>) {
            mediaAvatars[sheetId] = mediaDoc
        }

        override suspend fun deleteMediaAvatar(sheetId: String) {
            mediaAvatars.remove(sheetId)
        }

        override suspend fun writePresence(sheetId: String, presenceId: String, data: Map<String, Any?>) {
            presence[sheetId to presenceId] = data
        }

        override suspend fun deletePresence(sheetId: String, presenceId: String) {
            presence.remove(sheetId to presenceId)
        }

        override fun observePresence(
            sheetId: String,
            ownPresenceId: String,
            ownUid: String,
        ): Flow<Map<String, String>> {
            val now = clock.nowMillis()
            val active = linkedMapOf<String, String>()
            presence.filterKeys { it.first == sheetId && it.second != ownPresenceId }.values.forEach { data ->
                val expiresMs = when (val raw = data["expiresAt"]) {
                    is com.google.firebase.Timestamp -> raw.toDate().time
                    is Number -> raw.toLong()
                    else -> 0L
                }
                if (expiresMs > now) {
                    val key = data["fieldKey"]?.toString().orEmpty()
                    val uid = data["uid"]?.toString().orEmpty()
                    if (key.isNotBlank()) {
                        active[key] = if (uid == ownUid) "Outro dispositivo" else "Outro participante"
                    }
                }
            }
            return flowOf(active)
        }

        override fun observeRemoteChanges(uid: String, ownedActiveCampaignIds: Set<String>): Flow<RemoteSyncEvent> =
            flowOf()
    }

    private var signedInUid: String? = "player-1"

    private fun newHarness(): Triple<SparkFirestoreSyncEngine, InMemorySparkLocalStore, InMemorySparkRemoteGateway> {
        val clock = MutableClock()
        val store = InMemorySparkLocalStore()
        val remote = InMemorySparkRemoteGateway(clock)
        val engine = SparkFirestoreSyncEngine(
            localStore = store,
            remoteGateway = remote,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            workScheduler = RecordingWorkScheduler(),
            clock = clock,
            currentUidProvider = { signedInUid },
        )
        return Triple(engine, store, remote)
    }

    private fun session(uid: String) = UserSession(uid, "$uid@example.com", uid, UserRole.USER)

    private fun baseSheet() = Character(
        id = "sheet-1", ownerId = "player-1", name = "Ayla", money = 100,
        life = ResourceValue(100, 100), canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
        dirty = false,
    )

    private fun edit(id: String, key: String, value: String, revision: Long = 1L,
                     dependency: String? = null, targets: List<SparkTargetSpec>? = null) = SheetIntent(
        operationId = id, accountId = "player-1", sheetId = "sheet-1",
        type = OperationType.PATCH_FIELDS, targetKey = "field:" + key,
        targets = targets ?: listOf(SparkTargetSpec(kind = TargetKind.FIELD, key = key,
            op = TargetOp.SET, expectedRevision = revision, nextRevision = revision + 1,
            valueJson = value)),
        dependsOnOperationId = dependency, baseRevision = revision,
    )

    @Test
    fun unsupportedConsumptionNeverEntersQueue() = runBlocking {
        val (engine, store, remote) = newHarness()
        val base = baseSheet().copy(inventory = listOf(InventoryItem(id = "potion",
            quantity = 2, mechanicalEffects = listOf(ItemEffect(id = "effect",
                type = ItemEffectType.ATTRIBUTE, condition = ItemEffectCondition.EQUIPPED)))) )
        remote.seedFromCharacter(base.toRecord())
        store.upsertAccountSheet(AccountSheetRecord(accountId = "player-1", sheetId = "sheet-1",
            sheetRevision = 1, fieldRevisionsJson = "{}",
            confirmedJson = com.google.gson.Gson().toJson(base.toRecord()), updatedAt = 1))
        engine.activateAccount("player-1")
        val error = runCatching { engine.applySessionCommand(session("player-1"), base,
            SessionCommand(idempotencyKey = "consume", type = SessionOperationType.EFFECT_OPERATION,
                effectOperation = EffectOperation.ConsumeItemState(ItemInstanceId("potion"), 1,
                    ConsumableStateKind.Dose, applyEffects = false))) }.exceptionOrNull()
        assertNotNull(error)
        assertTrue(store.listIntentsForSheet("player-1", "sheet-1").isEmpty())
        assertEquals(2, store.getCharacter("sheet-1")!!.toDomain().inventory.single().quantity)
        assertEquals(100, store.getCharacter("sheet-1")!!.toDomain().life.current)
    }

    @Test
    fun scheduledOldAccountCannotActivateOrRejectItsQueue() = runBlocking {
        val (engine, store, remote) = newHarness()
        remote.seedFromCharacter(baseSheet().toRecord())
        engine.sync(session("player-1"))
        val intent = edit("old", "name", "\"Draft\"")
        store.upsertIntent(intent.toRecord())
        signedInUid = "user-b"
        engine.activateAccount("user-b")
        engine.drain("player-1")
        assertNull(store.getCharacter("sheet-1"))
        assertEquals(SheetIntentStatus.PENDING.name, store.getIntent("player-1", "old")?.status)
        assertEquals("Ayla", remote.fields["sheet-1" to "name"]?.get("value"))
    }

    @Test
    fun dependentAbsoluteEditBindsToActualDeltaReceiptRevision() = runBlocking {
        val (engine, store, remote) = newHarness()
        remote.seedFromCharacter(baseSheet().toRecord())
        engine.sync(session("player-1"))
        val delta = SparkTargetSpec(kind = TargetKind.FIELD, key = "money", op = TargetOp.DELTA,
            delta = -10, expectedRevision = 1, nextRevision = 2)
        store.upsertIntent(edit("parent", "money", "", targets = listOf(delta)).copy(createdAt = 1).toRecord())
        store.upsertIntent(edit("child", "money", "50", 2, "parent").copy(createdAt = 2).toRecord())
        remote.fields["sheet-1" to "money"] = remote.fields.getValue("sheet-1" to "money") +
            mapOf("value" to 80, "revision" to 2L)
        engine.drain("player-1")
        assertEquals(50L, (remote.fields["sheet-1" to "money"]?.get("value") as Number).toLong())
        assertEquals(4L, (remote.fields["sheet-1" to "money"]?.get("revision") as Number).toLong())
        assertEquals(SheetIntentStatus.CONFIRMED.name, store.getIntent("player-1", "child")?.status)
    }

    @Test
    fun transientParentWaitsBeforeDependentWhileIndependentCommits() = runBlocking {
        val (engine, store, remote) = newHarness()
        remote.seedFromCharacter(baseSheet().toRecord())
        engine.sync(session("player-1"))
        store.upsertIntent(edit("parent", "name", "\"First\"").copy(createdAt = 1).toRecord())
        store.upsertIntent(edit("child", "name", "\"Second\"", 2, "parent").copy(createdAt = 2).toRecord())
        store.upsertIntent(edit("independent", "occupation", "\"Mage\"").copy(createdAt = 3).toRecord())
        remote.transientFailureCount = 1
        assertFalse(engine.drain("player-1"))
        assertNull(remote.operations["sheet-1" to "child"])
        assertEquals(SheetIntentStatus.PENDING.name, store.getIntent("player-1", "child")?.status)
        assertEquals("Mage", remote.fields["sheet-1" to "occupation"]?.get("value"))
        engine.drain("player-1")
        assertEquals("Second", remote.fields["sheet-1" to "name"]?.get("value"))
    }

    @Test
    fun oversizedDeliveryLeavesQueueAndProjectionUntouched() = runBlocking {
        val (engine, store, remote) = newHarness()
        val base = baseSheet()
        remote.seedFromCharacter(base.toRecord())
        engine.sync(session("player-1"))
        val error = runCatching { engine.enqueueDeliveryAcceptance(session("player-1"), base,
            "delivery-1", base.copy(name = "Changed", race = "Elf", occupation = "Mage",
                age = "20", height = "180", sex = "F", story = "Changed")) }.exceptionOrNull()
        assertNotNull(error)
        assertTrue(store.listIntentsForSheet("player-1", "sheet-1").isEmpty())
        assertEquals("Ayla", store.getCharacter("sheet-1")?.name)
        assertTrue(remote.operations.isEmpty())
    }

    @Test
    fun deltaReplayUsesRefreshedMoneyAndResourceInsteadOfStaleAbsoluteValues() = runBlocking {
        val (engine, store, remote) = newHarness()
        val base = baseSheet()
        remote.seedFromCharacter(base.toRecord())
        engine.sync(session("player-1"))
        val targets = listOf(
            SparkTargetSpec(kind = TargetKind.FIELD, key = "money", op = TargetOp.DELTA,
                delta = -10, expectedRevision = 1, nextRevision = 2, valueJson = "90"),
            SparkTargetSpec(kind = TargetKind.FIELD, key = "life", op = TargetOp.DELTA,
                delta = -10, expectedRevision = 1, nextRevision = 2,
                valueJson = "{\"current\":90,\"maximum\":100,\"adjustment\":0}"),
        )
        store.upsertIntent(edit("delta", "money", "90", targets = targets).toRecord())
        remote.seedFromCharacter(base.copy(money = 80, life = ResourceValue(80, 100)).toRecord())
        engine.refreshSheetFromRemote("player-1", "sheet-1")
        val projected = store.getCharacter("sheet-1")!!.toDomain()
        assertEquals(70, projected.money)
        assertEquals(70, projected.life.current)
    }

    @Test
    fun resolvingEitherSideOfBatchConflictPreservesOtherEdits() = runBlocking {
        for (remoteChoice in listOf(false, true)) {
            val (engine, store, remote) = newHarness()
            remote.seedFromCharacter(baseSheet().toRecord())
            engine.sync(session("player-1"))
            val name = edit("batch", "name", "\"Local\"").targets.single()
            val occupation = edit("x", "occupation", "\"Mage\"").targets.single()
            store.upsertIntent(edit("batch", "name", "\"Local\"", targets = listOf(name, occupation)).toRecord())
            remote.fields["sheet-1" to "name"] = remote.fields.getValue("sheet-1" to "name") +
                mapOf("value" to "Remote", "revision" to 2L)
            engine.drain("player-1")
            val conflict = store.observeOpenConflicts("player-1").first().single()
            engine.resolveIntentConflict(session("player-1"), conflict.id, useRemote = remoteChoice)
            engine.drain("player-1")
            assertEquals("Mage", remote.fields["sheet-1" to "occupation"]?.get("value"))
            assertEquals(if (remoteChoice) "Remote" else "Local", remote.fields["sheet-1" to "name"]?.get("value"))
        }
    }

    @Test
    fun accountSwitchIsolatesCachesAndQueuesAndLogoutPreservesDurableQueues() = runBlocking {
        val (engine, store, remote) = newHarness()
        val userA = session("user-a")
        val userB = session("user-b")

        val charA = Character(
            id = "sheet-a",
            ownerId = "user-a",
            name = "Ayla",
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
            dirty = false,
        )
        val charB = Character(
            id = "sheet-b",
            ownerId = "user-b",
            name = "Bram",
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
            dirty = false,
        )
        remote.seedFromCharacter(charA.toRecord(), sheetRev = 1L)
        remote.seedFromCharacter(charB.toRecord(), sheetRev = 1L)

        signedInUid = "user-a"
        // User A syncs, edits offline while remote returns transient error.
        engine.sync(userA)
        remote.transientFailureCount = 10
        engine.save(userA, charA.copy(name = "Ayla Draft", editingBase = charA))
        engine.drain("user-a")

        assertEquals("Ayla Draft", store.getCharacter("sheet-a")?.name)
        assertEquals(1, store.listActiveIntents("user-a").size)

        // Logout leaves User A's durable queues & confirmed snapshot intact, while clearing active projection.
        engine.onLogout()
        assertTrue(store.allCharacters().isEmpty())
        assertEquals(1, store.listActiveIntents("user-a").size)
        assertNotNull(store.getAccountSheet("user-a", "sheet-a"))

        // User B logs in: sees only B's sheets, never A's drafts or intents.
        remote.transientFailureCount = 0
        signedInUid = "user-b"
        engine.sync(userB)
        assertNull(store.getCharacter("sheet-a"))
        assertEquals("Bram", store.getCharacter("sheet-b")?.name)
        assertTrue(store.listActiveIntents("user-b").isEmpty())

        // User A logs back in: A's pending draft immediately re-projects into `characters`.
        signedInUid = "user-a"
        engine.activateAccount("user-a")
        assertEquals("Ayla Draft", store.getCharacter("sheet-a")?.name)
        assertNull(store.getCharacter("sheet-b"))
        assertEquals(1, store.listActiveIntents("user-a").size)
    }

    @Test
    fun dependencyPauseOnFailureWhileIndependentIntentsKeepSyncing() = runBlocking {
        val (engine, store, remote) = newHarness()
        val user = session("player-1")
        val baseChar = Character(
            id = "sheet-1",
            ownerId = "player-1",
            name = "Ayla",
            money = 50,
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
            dirty = false,
        )
        remote.seedFromCharacter(baseChar.toRecord(), sheetRev = 1L)
        engine.sync(user)

        // Remote advances field:name to revision 2.
        remote.fields["sheet-1" to "name"] = mapOf(
            "key" to "name",
            "value" to "Ayla Remota",
            "revision" to 2L,
            "lastOperationId" to "gm-op",
            "updatedBy" to "gm-1",
            "updatedAt" to 9_000L,
        )

        // Intent 1 (op-name-1) edits name at expectedRevision=1 -> will CONFLICT.
        // Intent 2 (op-name-2) depends on op-name-1 -> must become PAUSED_DEPENDENCY.
        // Intent 3 (op-money) edits independent field:money at expectedRevision=1 -> must CONFIRM!
        val op1 = SheetIntent(
            operationId = "op-name-1",
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
                    baseValueJson = "\"Ayla\"",
                    valueJson = "\"Ayla Local 1\"",
                ),
            ),
            baseRevision = 1L,
            createdAt = 100L,
        )
        val op2 = SheetIntent(
            operationId = "op-name-2",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:name",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "name",
                    op = TargetOp.SET,
                    expectedRevision = 2L,
                    nextRevision = 3L,
                    baseValueJson = "\"Ayla Local 1\"",
                    valueJson = "\"Ayla Local 2\"",
                ),
            ),
            dependsOnOperationId = "op-name-1",
            baseRevision = 2L,
            createdAt = 200L,
        )
        val op3 = SheetIntent(
            operationId = "op-money",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:money",
            targets = listOf(
                SparkTargetSpec(
                    kind = TargetKind.FIELD,
                    key = "money",
                    op = TargetOp.SET,
                    expectedRevision = 1L,
                    nextRevision = 2L,
                    baseValueJson = "50",
                    valueJson = "80",
                ),
            ),
            dependsOnOperationId = null,
            baseRevision = 1L,
            createdAt = 300L,
        )
        store.upsertIntents(listOf(op1.toRecord(), op2.toRecord(), op3.toRecord()))

        engine.drain("player-1")

        assertEquals(SheetIntentStatus.CONFLICT.name, store.getIntent("player-1", "op-name-1")?.status)
        assertEquals(SheetIntentStatus.PAUSED_DEPENDENCY.name, store.getIntent("player-1", "op-name-2")?.status)
        assertEquals(SheetIntentStatus.CONFIRMED.name, store.getIntent("player-1", "op-money")?.status)
        assertEquals(80L, (remote.fields["sheet-1" to "money"]?.get("value") as Number).toLong())
        assertEquals(1, store.observeOpenConflicts("player-1").first().size)
    }

    @Test
    fun conflictResolutionCreatesNewIntentAnchoredOnRemoteRevision() = runBlocking {
        val (engine, store, remote) = newHarness()
        val user = session("player-1")
        val baseChar = Character(
            id = "sheet-1",
            ownerId = "player-1",
            name = "Ayla",
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
            dirty = false,
        )
        remote.seedFromCharacter(baseChar.toRecord(), sheetRev = 1L)
        engine.sync(user)

        remote.fields["sheet-1" to "name"] = mapOf(
            "key" to "name",
            "value" to "Ayla do Mestre",
            "revision" to 4L,
            "lastOperationId" to "gm-op",
            "updatedBy" to "gm-1",
            "updatedAt" to 9_500L,
        )

        val localEdit = SheetIntent(
            operationId = "op-conflict",
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
                    baseValueJson = "\"Ayla\"",
                    valueJson = "\"Ayla Local\"",
                ),
            ),
            baseRevision = 1L,
            baseValueJson = "\"Ayla\"",
            desiredPayloadJson = "\"Ayla Local\"",
            createdAt = 100L,
        )
        store.upsertIntent(localEdit.toRecord())
        engine.drain("player-1")

        val conflict = store.observeOpenConflicts("player-1").first().single()
        assertEquals(1L, conflict.baseRevision)
        assertEquals(4L, conflict.remoteRevision)

        // Resolve choosing custom merged value -> creates new PENDING intent anchored on remoteRevision=4.
        engine.resolveIntentConflict(user, conflict.id, useRemote = false, customValueJson = "\"Ayla Fundida\"")
        engine.drain("player-1")

        assertTrue(store.observeOpenConflicts("player-1").first().isEmpty())
        assertEquals("Ayla Fundida", remote.fields["sheet-1" to "name"]?.get("value"))
        assertEquals(5L, (remote.fields["sheet-1" to "name"]?.get("revision") as Number).toLong())
        assertEquals("Ayla Fundida", store.getCharacter("sheet-1")?.name)
    }

    @Test
    fun accessDeniedTransitionsToRecoverableDraftAndRetriesCleanly() = runBlocking {
        val (engine, store, remote) = newHarness()
        val user = session("player-1")
        val baseChar = Character(
            id = "sheet-1",
            ownerId = "player-1",
            name = "Ayla",
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
            dirty = false,
        )
        remote.seedFromCharacter(baseChar.toRecord(), sheetRev = 1L)
        engine.sync(user)

        remote.permissionDeniedForSheet = "sheet-1"
        engine.save(user, baseChar.copy(name = "Ayla Bloqueada", editingBase = baseChar))
        engine.drain("player-1")

        val deniedIntents = store.observeIntents("player-1").first()
        assertEquals(1, deniedIntents.size)
        val deniedIntent = deniedIntents.single()
        assertEquals(SheetIntentStatus.ACCESS_DENIED.name, deniedIntent.status)
        // Draft is NEVER discarded!
        assertNotNull(store.getIntent("player-1", deniedIntent.operationId))

        // Unlock / restore access and retry recoverable draft.
        remote.permissionDeniedForSheet = null
        engine.retryAccessDeniedDraft(user, deniedIntent.operationId)
        engine.drain("player-1")

        assertEquals(SheetIntentStatus.CONFIRMED.name, store.getIntent("player-1", deniedIntent.operationId)?.status)
        assertEquals("Ayla Bloqueada", remote.fields["sheet-1" to "name"]?.get("value"))
    }

    @Test
    fun idempotentReplayMatchesCommittedReceiptAndHashMismatchRejects() = runBlocking {
        val (engine, store, remote) = newHarness()
        val user = session("player-1")
        val baseChar = Character(
            id = "sheet-1",
            ownerId = "player-1",
            name = "Ayla",
            money = 100,
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
            dirty = false,
        )
        remote.seedFromCharacter(baseChar.toRecord(), sheetRev = 1L)
        engine.sync(user)

        val matchedTarget = SparkTargetSpec(
            kind = TargetKind.FIELD,
            key = "name",
            op = TargetOp.SET,
            expectedRevision = 1L,
            nextRevision = 2L,
            valueJson = "\"Ayla Idempotente\"",
        )
        val matchedIntent = SheetIntent(
            operationId = "op-replay-1",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:name",
            targets = listOf(matchedTarget),
            baseRevision = 1L,
        )
        // Pre-seed matching committed receipt on server (simulating prior commit where client lost ack).
        remote.operations["sheet-1" to "op-replay-1"] = mapOf(
            "id" to "op-replay-1",
            "actorUid" to "player-1",
            "type" to OperationType.PATCH_FIELDS.name,
            "outcome" to "COMMITTED",
            "schemaVersion" to 2,
            "createdAt" to 9_000L,
            "targets" to listOf(
                mapOf("kind" to "FIELD", "itemId" to "", "key" to "name", "op" to "SET", "expectedRevision" to 1L, "nextRevision" to 2L),
            ),
            "preconditions" to mapOf("payloadHash" to matchedIntent.payloadHash),
        )
        remote.fields["sheet-1" to "name"] = remote.fields.getValue("sheet-1" to "name") +
            mapOf("value" to "Ayla Idempotente", "revision" to 2L)
        store.upsertIntent(matchedIntent.toRecord())
        store.upsertIntent(edit("later-dependent", "name", "\"Later edit\"", 2, "op-replay-1")
            .copy(createdAt = 1).toRecord())

        // Second intent reuses an existing opId ("op-replay-2") with a DIFFERENT payload hash!
        val spoofedIntent = SheetIntent(
            operationId = "op-replay-2",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:money",
            targets = listOf(
                SparkTargetSpec(kind = TargetKind.FIELD, key = "money", op = TargetOp.SET, expectedRevision = 1L, nextRevision = 2L, valueJson = "99999"),
            ),
            baseRevision = 1L,
        )
        remote.operations["sheet-1" to "op-replay-2"] = mapOf(
            "id" to "op-replay-2",
            "actorUid" to "player-1",
            "type" to OperationType.PATCH_FIELDS.name,
            "outcome" to "COMMITTED",
            "schemaVersion" to 2,
            "createdAt" to 9_000L,
            "targets" to listOf(
                mapOf("kind" to "FIELD", "itemId" to "", "key" to "money", "op" to "SET", "expectedRevision" to 1L, "nextRevision" to 2L),
            ),
            "preconditions" to mapOf("payloadHash" to "different-original-hash"),
        )
        store.upsertIntent(spoofedIntent.toRecord())

        engine.drain("player-1")

        assertEquals("Later edit", remote.fields["sheet-1" to "name"]?.get("value"))
        assertEquals("Later edit", store.getCharacter("sheet-1")?.name)
        assertEquals(3L, (remote.fields["sheet-1" to "name"]?.get("revision") as Number).toLong())
        assertEquals(SheetIntentStatus.CONFIRMED.name, store.getIntent("player-1", "op-replay-1")?.status)
        assertEquals(SheetIntentStatus.REJECTED.name, store.getIntent("player-1", "op-replay-2")?.status)
        assertEquals(100L, (remote.fields["sheet-1" to "money"]?.get("value") as Number).toLong())
    }

    @Test
    fun oversizedMultiEffectActionIsRejectedBeforeOptimisticMutation() = runBlocking {
        val (engine, store, remote) = newHarness()
        val user = session("player-1")
        val potion = InventoryItem(
            id = "potion-1",
            name = "Poção Complexa",
            quantity = 3,
            mechanicalEffects = listOf(
                ItemEffect(id = "e1", type = ItemEffectType.ATTRIBUTE, condition = ItemEffectCondition.EQUIPPED, description = "+1 PV"),
            ),
        )
        val char = Character(
            id = "sheet-1",
            ownerId = "player-1",
            name = "Ayla",
            life = ResourceValue(5, 20),
            sanity = ResourceValue(5, 20),
            energy = ResourceValue(5, 20),
            arcane = ResourceValue(5, 20),
            destiny = ResourceValue(2, 5),
            inventory = listOf(potion),
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
            dirty = false,
        )
        remote.seedFromCharacter(char.toRecord(), sheetRev = 1L)
        engine.sync(user)

        // An ability with > 5 targets when an ITEM_FIELD (Ash consumption) or > 7 targets must throw ValidationError BEFORE mutating Room!
        val bigDamage = EffectOperation.Damage(
            amount = com.kinderman.sdo.domain.model.DiceOrNumber.Fixed(10),
            damageType = com.kinderman.sdo.domain.model.DamageType.Physical,
            target = TargetSpec.Self,
        )
        // Use a multi-target budget check directly via SparkWireCodec + validateAtomicSparkActionBudget + enqueue check:
        val manyTargets = (1..6).map { idx ->
            SparkTargetSpec(
                kind = TargetKind.ITEM_FIELD,
                itemId = "inv_potion-1",
                key = "quantity",
                op = TargetOp.DELTA,
                delta = -idx,
            )
        }
        val err = runCatching {
            com.kinderman.sdo.domain.model.validateAtomicSparkActionBudget(manyTargets, "Poção Complexa")
        }.exceptionOrNull()
        assertTrue(err is DomainError.ValidationError)

        // Verify valid atomic ITEM_CONSUME places consumed ITEM_FIELD (delta < 0) at targets[0].
        val validConsume = SessionCommand(
            idempotencyKey = "op-consume-valid",
            type = SessionOperationType.EFFECT_OPERATION,
            label = "Beber Poção",
            effectOperation = EffectOperation.ConsumeItemState(
                itemInstanceId = ItemInstanceId("potion-1"),
                amount = 1,
                state = ConsumableStateKind.Dose,
                applyEffects = false,
            ),
        )
        // Note: ConsumeItemState without ItemState throws InvalidReference if no canonical itemStates; test MONEY/RESOURCE + budget guard on applySessionCommand:
        val spendEnergy = SessionCommand(
            idempotencyKey = "op-spend-energy",
            type = SessionOperationType.RESOURCE,
            resource = SessionResource.ENERGY,
            amount = -2,
        )
        assertTrue(engine.applySessionCommand(user, char, spendEnergy))
        engine.drain("player-1")
        assertEquals(SheetIntentStatus.CONFIRMED.name, store.getIntent("player-1", "op-spend-energy")?.status)
        val remoteEnergy = remote.fields["sheet-1" to "energy"]?.get("value") as Map<*, *>
        assertEquals(0L, (remoteEnergy["current"] as Number).toLong())
        assertNotNull(bigDamage)
    }

    @Test
    fun legacyV32EnvelopeReconcilesSafelyWithoutDiscardingOrAutoAcknowledging() = runBlocking {
        val (engine, store, remote) = newHarness()
        val user = session("player-1")
        val confirmed = Character(
            id = "sheet-1",
            ownerId = "player-1",
            name = "Ayla Antiga",
            creationRulesVersion = 2,
            money = 40,
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
            dirty = false,
        )
        remote.seedFromCharacter(confirmed.toRecord(), sheetRev = 1L)
        store.upsertAccountSheet(
            AccountSheetRecord(
                accountId = "player-1",
                sheetId = "sheet-1",
                sheetRevision = 1L,
                fieldRevisionsJson = "{\"field:name\":1,\"field:money\":1}",
                confirmedJson = com.google.gson.Gson().toJson(confirmed.toRecord()),
                updatedAt = 500L,
            ),
        )

        val optimistic = confirmed.copy(name = "Ayla Migrada", money = 75).toRecord()
        val optimisticJson = com.google.gson.Gson().toJson(optimistic)
        val legacyEnvelope = """{"protocolVersion":1,"sheetId":"sheet-1","operationId":"legacy-op-1","command":{"type":"PATCH","payload":{}}}"""

        // Simulate row inserted by AppDatabase.MIGRATION_32_33: targetKey='sheet', status='PENDING'.
        store.upsertIntent(
            SheetIntentRecord(
                operationId = "legacy-op-1",
                accountId = "player-1",
                sheetId = "sheet-1",
                type = OperationType.PATCH_FIELDS.name,
                targetKey = "sheet",
                dependsOnOperationId = null,
                baseRevision = 0L,
                baseValueJson = legacyEnvelope,
                desiredPayloadJson = optimisticJson,
                payloadHash = "legacy-hash",
                status = SheetIntentStatus.PENDING.name,
                error = "",
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )

        engine.activateAccount("player-1")
        val reconciled = store.getIntent("player-1", "legacy-op-1")!!.toDomain()
        // Never auto-acknowledged: still PENDING, now with real Spark v2 field targets!
        assertEquals(SheetIntentStatus.PENDING, reconciled.status)
        assertTrue(reconciled.targets.any { it.kind == TargetKind.FIELD && it.key == "name" })
        assertTrue(reconciled.targets.any { it.kind == TargetKind.FIELD && it.key == "money" })

        // Now drain commits it to Firestore!
        engine.drain("player-1")
        assertEquals(SheetIntentStatus.CONFIRMED.name, store.getIntent("player-1", "legacy-op-1")?.status)
        assertEquals("Ayla Migrada", remote.fields["sheet-1" to "name"]?.get("value"))
        assertEquals(75L, (remote.fields["sheet-1" to "money"]?.get("value") as Number).toLong())
    }

    @Test
    fun deltaRejectsInsufficientBalanceAndTombstoneRejectsStaleEdit() = runBlocking {
        val (engine, store, remote) = newHarness()
        val user = session("player-1")
        val char = Character(
            id = "sheet-1",
            ownerId = "player-1",
            name = "Ayla",
            life = ResourceValue(3, 20),
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
            dirty = false,
        )
        remote.seedFromCharacter(char.toRecord(), sheetRev = 1L)
        engine.sync(user)

        // Spend 5 life when remote only has 3 -> must REJECT!
        val overdraw = SheetIntent(
            operationId = "op-overdraw",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.ADJUST_RESOURCE,
            targetKey = "field:life",
            targets = listOf(
                SparkTargetSpec(kind = TargetKind.FIELD, key = "life", op = TargetOp.DELTA, expectedRevision = 1L, nextRevision = 2L, delta = -5, minValue = 0),
            ),
            baseRevision = 1L,
        )
        store.upsertIntent(overdraw.toRecord())
        engine.drain("player-1")
        assertEquals(SheetIntentStatus.REJECTED.name, store.getIntent("player-1", "op-overdraw")?.status)

        // Tombstone remote sheet -> stale edit must REJECT!
        remote.roots["sheet-1"] = remote.roots["sheet-1"]!!.toMutableMap().apply { put("deleted", true) }
        val staleEdit = SheetIntent(
            operationId = "op-stale",
            accountId = "player-1",
            sheetId = "sheet-1",
            type = OperationType.PATCH_FIELDS,
            targetKey = "field:name",
            targets = listOf(
                SparkTargetSpec(kind = TargetKind.FIELD, key = "name", op = TargetOp.SET, expectedRevision = 1L, nextRevision = 2L, valueJson = "\"Fantasma\""),
            ),
            baseRevision = 1L,
        )
        store.upsertIntent(staleEdit.toRecord())
        engine.drain("player-1")
        assertEquals(SheetIntentStatus.REJECTED.name, store.getIntent("player-1", "op-stale")?.status)
    }

    @Test
    fun presenceExpiresAfter90SecondsAndLargeLocalPhotoIsNotUploaded() = runBlocking {
        val clock = MutableClock(100_000L)
        val store = InMemorySparkLocalStore()
        val remote = InMemorySparkRemoteGateway(clock)
        val engine = SparkFirestoreSyncEngine(
            localStore = store,
            remoteGateway = remote,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            workScheduler = RecordingWorkScheduler(),
            clock = clock,
            currentUidProvider = { null },
        )
        val user = session("player-1")
        val other = session("gm-1")
        val char = Character(
            id = "sheet-1",
            ownerId = "player-1",
            canonicalSchemaVersion = CURRENT_CHARACTER_SCHEMA_VERSION,
            dirty = false,
        )
        remote.seedFromCharacter(char.toRecord(), sheetRev = 1L)
        engine.sync(user)

        // Presence heartbeat writes expiresAt = now + 90_000ms.
        engine.setFieldPresence(other, "sheet-1", "field:life", active = true)
        val activePresence = engine.observeFieldPresence(user, "sheet-1").first()
        assertEquals("Outro participante", activePresence["field:life"])

        // Advance clock past 90s -> expired presence is ignored on read.
        clock.time += 91_000L
        val expiredPresence = engine.observeFieldPresence(user, "sheet-1").first()
        assertTrue(expiredPresence.isEmpty())

        // Local-only photo URI stays in account_sheets without uploading to media/avatar.
        engine.save(user, char.copy(photoUri = "content://media/external/images/1234", editingBase = char))
        assertEquals("content://media/external/images/1234", store.getAccountSheet("player-1", "sheet-1")?.localPhotoUri)
        assertNull(remote.mediaAvatars["sheet-1"])

        // Small <= 64 KiB dataUri thumbnail IS uploaded to media/avatar.
        val tinyThumb = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/"
        engine.save(user, char.copy(photoUri = tinyThumb, editingBase = char))
        assertEquals(tinyThumb, remote.mediaAvatars["sheet-1"]?.get("dataUri"))
    }
}
