package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.CampaignAlertSettingsRecord
import com.kinderman.sdo.data.local.CampaignDao
import com.kinderman.sdo.data.local.CampaignDeliveryRecord
import com.kinderman.sdo.data.local.CampaignLibraryRecord
import com.kinderman.sdo.data.local.CampaignPayloadCodec
import com.kinderman.sdo.data.local.CharacterMigrationManager
import com.kinderman.sdo.data.local.OperationsDao
import com.kinderman.sdo.data.local.SessionOperationRecord
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.data.sync.SparkFirestoreSyncEngine
import com.kinderman.sdo.domain.model.CampaignAlertSettings
import com.kinderman.sdo.domain.model.CampaignContentKind
import com.kinderman.sdo.domain.model.CampaignDelivery
import com.kinderman.sdo.domain.model.CampaignDeliveryState
import com.kinderman.sdo.domain.model.CampaignLibraryEntry
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.ConditionEffect
import com.kinderman.sdo.domain.model.InventoryItem
import com.kinderman.sdo.domain.model.PersonalNote
import com.kinderman.sdo.domain.model.Power
import com.kinderman.sdo.domain.model.SessionCommand
import com.kinderman.sdo.domain.model.SessionOperation
import com.kinderman.sdo.domain.model.SpecialKnowledge
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.OperationsRepository
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val AUDIT_SYNC_LIMIT = 200L

class SyncedOperationsRepository(
    private val dao: OperationsDao,
    private val campaignDao: CampaignDao,
    private val migrationManager: CharacterMigrationManager = CharacterMigrationManager(),
    private val syncEngine: SparkFirestoreSyncEngine? = null,
) : OperationsRepository {
    override fun observeAudit(campaignIds: Set<String>): Flow<List<SessionOperation>> =
        dao.observeAudit(campaignIds.ifEmpty { setOf("") }).map { it.map(SessionOperationRecord::toDomain) }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun observeLibrary(campaignIds: Set<String>): Flow<List<CampaignLibraryEntry>> =
        dao.observeLibrary(campaignIds).map { it.map(CampaignLibraryRecord::toDomain) }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun observeDeliveries(session: UserSession, campaignIds: Set<String>): Flow<List<CampaignDelivery>> =
        dao.observeDeliveries(session.uid, campaignIds).map { it.map(CampaignDeliveryRecord::toDomain) }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun observeAlertSettings(campaignIds: Set<String>): Flow<List<CampaignAlertSettings>> =
        dao.observeAlertSettings(campaignIds).map { it.map(CampaignAlertSettingsRecord::toDomain) }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override suspend fun apply(session: UserSession, character: Character, command: SessionCommand): Boolean {
        val engine = requireNotNull(syncEngine) { "SparkFirestoreSyncEngine indisponível." }
        return engine.applySessionCommand(session, character, command)
    }

    override suspend fun saveLibrary(session: UserSession, entry: CampaignLibraryEntry) {
        requireActiveCampaign(entry.campaignId)
        val existing = dao.library(entry.id)
        require(existing == null || existing.campaignId == entry.campaignId) {
            "A campanha de um modelo salvo não pode ser alterada. Duplique o modelo para transferi-lo."
        }
        val now = System.currentTimeMillis()
        dao.upsertLibrary(
            entry.copy(createdBy = entry.createdBy.ifBlank { session.uid }, updatedAt = now, dirty = true).toRecord(),
        )
    }

    override suspend fun duplicateLibrary(session: UserSession, entry: CampaignLibraryEntry) {
        requireActiveCampaign(entry.campaignId)
        saveLibrary(
            session,
            entry.copy(
                id = UUID.randomUUID().toString(),
                name = "${entry.name} — cópia",
                version = 1,
                createdBy = session.uid,
                createdAt = System.currentTimeMillis(),
                archived = false,
            ),
        )
    }

    override suspend fun archiveLibrary(session: UserSession, entry: CampaignLibraryEntry, archived: Boolean) =
        saveLibrary(session, entry.copy(archived = archived, version = entry.version + 1))

    override suspend fun deliver(
        session: UserSession,
        entry: CampaignLibraryEntry,
        recipients: List<Character>,
        knowledgeMappings: Map<String, String>,
    ) {
        requireActiveCampaign(entry.campaignId)
        require(recipients.isNotEmpty()) { "Selecione ao menos uma ficha." }
        if (entry.knowledgeBonus != 0) require(recipients.all { !knowledgeMappings[it.id].isNullOrBlank() }) {
            "Mapeie o Conhecimento Adquirido de cada destinatário."
        }
        val now = System.currentTimeMillis()
        recipients.forEach { character ->
            dao.upsertDelivery(
                CampaignDelivery(
                    campaignId = entry.campaignId,
                    libraryEntryId = entry.id,
                    recipientId = character.ownerId,
                    recipientCharacterId = character.id,
                    snapshotKind = entry.kind,
                    snapshotName = entry.name,
                    snapshotSummary = entry.summary,
                    snapshotPayload = CampaignPayloadCodec.encode(entry),
                    knowledgeMapping = knowledgeMappings[character.id].orEmpty(),
                    knowledgeBonus = entry.knowledgeBonus,
                    createdBy = session.uid,
                    createdAt = now,
                    updatedAt = now,
                ).toRecord(),
            )
        }
    }

    override suspend fun respondToDelivery(session: UserSession, delivery: CampaignDelivery, accept: Boolean) {
        require(delivery.recipientId == session.uid) { "Somente o destinatário pode responder." }
        require(delivery.state == CampaignDeliveryState.PENDING) { "Esta entrega já foi respondida." }
        val now = System.currentTimeMillis()
        val responded = delivery.copy(
            state = if (accept) CampaignDeliveryState.ACCEPTED else CampaignDeliveryState.DECLINED,
            updatedAt = now,
            dirty = true,
        )
        if (!accept) {
            dao.upsertDelivery(responded.toRecord())
            return
        }
        val record = dao.character(delivery.recipientCharacterId)?.let { migrationManager.migrate(it, markDirty = false) }
            ?: error("Ficha destinatária não encontrada.")
        val baseCharacter = record.toDomain()
        val appliedCharacter = applyDelivery(baseCharacter, delivery).copy(
            updatedAt = now,
            lastEditedBy = session.uid,
            dirty = false,
        )
        requireNotNull(syncEngine).enqueueDeliveryAcceptance(session, baseCharacter, delivery.id, appliedCharacter)
        dao.upsertDelivery(responded.toRecord())
    }

    override suspend fun saveAlertSettings(settings: CampaignAlertSettings) = dao.upsertAlertSettings(settings.toRecord())

    override suspend fun sync(
        session: UserSession,
        readableCampaignIds: Set<String>,
        writableCampaignIds: Set<String>,
    ) {
        val store = Firebase.firestore
        val operations = store.collection(FirestoreCollections.CAMPAIGN_AUDIT)
        val library = store.collection(FirestoreCollections.CAMPAIGN_LIBRARY)
        val deliveries = store.collection(FirestoreCollections.CAMPAIGN_DELIVERIES)

        dao.dirtyOperations().filter { it.actorId == session.uid }.forEach { local ->
            val reference = operations.document(local.id)
            try {
                reference.set(local.copy(dirty = false)).await()
            } catch (error: FirebaseFirestoreException) {
                if (error.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw error
                val existing = reference.get().await()
                val sameOperation = existing.exists() &&
                    existing.getString("actorId") == local.actorId &&
                    existing.getString("idempotencyKey") == local.idempotencyKey
                if (!sameOperation) throw error
            }
            dao.markOperationSynced(local.id, local.createdAt)
        }

        dao.dirtyLibrary().filter { it.campaignId in writableCampaignIds }.forEach { original ->
            val reference = library.document(original.id)
            val remote = reference.get().await().toObject(CampaignLibraryRecord::class.java)
            val local = if (remote != null && remote.campaignId != original.campaignId) {
                original.copy(campaignId = remote.campaignId).also { dao.upsertLibrary(it) }
            } else {
                original
            }
            reference.set(local.copy(dirty = false), SetOptions.merge()).await()
            dao.markLibrarySynced(local.id, local.updatedAt)
        }

        // Accepted deliveries are committed atomically inside SparkFirestoreSyncEngine (ACCEPT_DELIVERY).
        // Drain pending Spark intents before checking delivery state so accepted deliveries settle cleanly.
        syncEngine?.drain(session.uid)
        dao.dirtyDeliveries().filter { it.createdBy == session.uid || it.recipientId == session.uid }.forEach { local ->
            if (local.state == CampaignDeliveryState.ACCEPTED.name && local.recipientId == session.uid) {
                val remoteState = runCatching {
                    deliveries.document(local.id).get().await().getString("state")
                }.getOrNull()
                if (remoteState == CampaignDeliveryState.ACCEPTED.name) {
                    dao.markDeliverySynced(local.id, local.updatedAt)
                }
            } else {
                deliveries.document(local.id).set(local.copy(dirty = false), SetOptions.merge()).await()
                dao.markDeliverySynced(local.id, local.updatedAt)
            }
        }

        val readableSnapshots = coroutineScope {
            readableCampaignIds.map { campaignId ->
                async {
                    val lastAuditSync = dao.maxSyncedAuditAt(campaignId)
                    val lastLibrarySync = dao.maxSyncedLibraryAt(campaignId)
                    val auditQuery = operations.whereEqualTo("campaignId", campaignId)
                        .let { if (lastAuditSync > 0L) it.whereGreaterThan("createdAt", lastAuditSync) else it }
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(AUDIT_SYNC_LIMIT)
                    val libraryQuery = library.whereEqualTo("campaignId", campaignId)
                        .let { if (lastLibrarySync > 0L) it.whereGreaterThan("updatedAt", lastLibrarySync) else it }
                        .orderBy("updatedAt", Query.Direction.DESCENDING)
                    val remoteAudit = auditQuery.get().await().documents.mapNotNull { it.toObject(SessionOperationRecord::class.java) }
                    val remoteLibrary = libraryQuery.get().await().documents.mapNotNull { it.toObject(CampaignLibraryRecord::class.java) }
                    remoteAudit to remoteLibrary
                }
            }.awaitAll()
        }
        readableSnapshots.forEach { (remoteAudit, remoteLibrary) ->
            remoteAudit.forEach { dao.upsertOperation(it.copy(dirty = false, lastSyncedAt = it.createdAt)) }
            remoteLibrary.forEach { dao.upsertLibrary(it.copy(dirty = false, lastSyncedAt = it.updatedAt)) }
        }

        val remoteDeliveries = coroutineScope {
            val campaignJobs = writableCampaignIds.map { campaignId ->
                async {
                    deliveries.whereEqualTo("campaignId", campaignId)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get().await().documents.mapNotNull { it.toObject(CampaignDeliveryRecord::class.java) }
                }
            }
            val recipientJob = async {
                deliveries.whereEqualTo("recipientId", session.uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().await().documents.mapNotNull { it.toObject(CampaignDeliveryRecord::class.java) }
            }
            (campaignJobs + recipientJob).awaitAll().flatten()
        }
        remoteDeliveries.distinctBy(CampaignDeliveryRecord::id)
            .forEach { dao.upsertDelivery(it.copy(dirty = false, lastSyncedAt = it.updatedAt)) }
    }

    private suspend fun requireActiveCampaign(campaignId: String) {
        val campaign = campaignDao.campaign(campaignId) ?: error("Campanha não encontrada.")
        require(campaign.state == "ACTIVE") { "Campanhas arquivadas são somente leitura." }
    }
}

internal fun applyDelivery(character: Character, delivery: CampaignDelivery): Character {
    if (delivery.id in character.appliedDeliveryIds) return character
    val decoded = CampaignPayloadCodec.decode(delivery.snapshotKind, delivery.snapshotPayload)
    var changed = when (delivery.snapshotKind) {
        CampaignContentKind.ITEM -> character.copy(
            inventory = character.inventory + (
                (decoded?.item ?: InventoryItem(name = delivery.snapshotName, effect = delivery.snapshotSummary))
                    .copy(id = "delivery:${delivery.id}")
                ),
        )
        CampaignContentKind.POWER -> character.copy(
            powers = character.powers + (
                (decoded?.power ?: Power(name = delivery.snapshotName, effect = delivery.snapshotSummary))
                    .copy(id = "delivery:${delivery.id}")
                ),
        )
        CampaignContentKind.CONDITION -> character.copy(
            conditions = character.conditions + (
                (decoded?.condition ?: ConditionEffect(name = delivery.snapshotName, summary = delivery.snapshotSummary, origin = "Campanha"))
                    .copy(id = "delivery:${delivery.id}")
                ),
        )
        CampaignContentKind.NOTE, CampaignContentKind.REWARD, CampaignContentKind.TEMPLATE -> character.copy(
            personalNotes = character.personalNotes + PersonalNote(
                id = "delivery:${delivery.id}",
                title = delivery.snapshotName,
                text = delivery.snapshotSummary + "\n" + (decoded?.text ?: delivery.snapshotPayload),
            ),
        )
    }
    if (delivery.knowledgeBonus != 0) {
        val index = changed.learnedKnowledges.indexOfFirst {
            it.id == delivery.knowledgeMapping || it.name.equals(delivery.knowledgeMapping, true)
        }
        require(index >= 0) { "O Conhecimento Adquirido mapeado não existe mais." }
        val learned = changed.learnedKnowledges.toMutableList()
        learned[index] = learned[index].copy(adjustment = learned[index].adjustment + delivery.knowledgeBonus)
        changed = changed.copy(learnedKnowledges = learned)
    }
    return changed.copy(appliedDeliveryIds = (changed.appliedDeliveryIds + delivery.id).distinct())
}
