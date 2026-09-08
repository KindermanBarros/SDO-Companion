package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.CampaignAlertSettingsRecord
import com.kinderman.sdo.data.local.CampaignDeliveryRecord
import com.kinderman.sdo.data.local.CampaignLibraryRecord
import com.kinderman.sdo.data.local.OperationsDao
import com.kinderman.sdo.data.local.SessionOperationRecord
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
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
import com.kinderman.sdo.domain.model.applySessionCommand
import com.kinderman.sdo.domain.repository.OperationsRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OfflineFirstOperationsRepository(private val dao: OperationsDao) : OperationsRepository {
    override fun observeAudit(campaignIds: Set<String>): Flow<List<SessionOperation>> =
        dao.observeAudit(campaignIds.ifEmpty { setOf("") }).map { it.map(SessionOperationRecord::toDomain) }

    override fun observeLibrary(campaignIds: Set<String>): Flow<List<CampaignLibraryEntry>> =
        dao.observeLibrary(campaignIds).map { it.map(CampaignLibraryRecord::toDomain) }

    override fun observeDeliveries(session: UserSession, campaignIds: Set<String>): Flow<List<CampaignDelivery>> =
        dao.observeDeliveries(session.uid, campaignIds).map { it.map(CampaignDeliveryRecord::toDomain) }

    override fun observeAlertSettings(campaignIds: Set<String>): Flow<List<CampaignAlertSettings>> =
        dao.observeAlertSettings(campaignIds).map { it.map(CampaignAlertSettingsRecord::toDomain) }

    override suspend fun apply(session: UserSession, character: Character, command: SessionCommand): Boolean {
        val mutation = character.applySessionCommand(command, session.uid)
        return dao.applyOnce(mutation.operation.toRecord(), mutation.character.toRecord())
    }

    override suspend fun saveLibrary(session: UserSession, entry: CampaignLibraryEntry) {
        val now = System.currentTimeMillis()
        dao.upsertLibrary(entry.copy(createdBy = entry.createdBy.ifBlank { session.uid }, updatedAt = now, dirty = true).toRecord())
    }

    override suspend fun duplicateLibrary(session: UserSession, entry: CampaignLibraryEntry) {
        saveLibrary(session, entry.copy(id = UUID.randomUUID().toString(), name = "${entry.name} — cópia", version = 1, createdBy = session.uid, createdAt = System.currentTimeMillis(), archived = false))
    }

    override suspend fun archiveLibrary(session: UserSession, entry: CampaignLibraryEntry, archived: Boolean) =
        saveLibrary(session, entry.copy(archived = archived, version = entry.version + 1))

    override suspend fun deliver(
        session: UserSession,
        entry: CampaignLibraryEntry,
        recipients: List<Character>,
        knowledgeMappings: Map<String, String>,
    ) {
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
                    snapshotPayload = entry.payload,
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
        if (!accept) return dao.upsertDelivery(responded.toRecord())
        val record = dao.character(delivery.recipientCharacterId) ?: error("Ficha destinatária não encontrada.")
        var character = record.toDomain()
        character = when (delivery.snapshotKind) {
            CampaignContentKind.ITEM -> character.copy(inventory = character.inventory + InventoryItem(name = delivery.snapshotName, effect = delivery.snapshotSummary))
            CampaignContentKind.POWER -> character.copy(powers = character.powers + Power(name = delivery.snapshotName, effect = delivery.snapshotSummary))
            CampaignContentKind.CONDITION -> character.copy(conditions = character.conditions + ConditionEffect(name = delivery.snapshotName, summary = delivery.snapshotSummary, origin = "Campanha"))
            CampaignContentKind.NOTE, CampaignContentKind.REWARD, CampaignContentKind.TEMPLATE -> character.copy(personalNotes = character.personalNotes + PersonalNote(title = delivery.snapshotName, text = delivery.snapshotSummary + "\n" + delivery.snapshotPayload))
        }
        if (delivery.knowledgeBonus != 0) {
            val index = character.learnedKnowledges.indexOfFirst { it.name.equals(delivery.knowledgeMapping, true) }
            val learned = character.learnedKnowledges.toMutableList()
            if (index >= 0) learned[index] = learned[index].copy(adjustment = learned[index].adjustment + delivery.knowledgeBonus)
            else learned += SpecialKnowledge(name = delivery.knowledgeMapping, adjustment = delivery.knowledgeBonus, source = "Campanha")
            character = character.copy(learnedKnowledges = learned)
        }
        dao.acceptDelivery(responded.toRecord(), character.copy(updatedAt = now, dirty = true).toRecord())
    }

    override suspend fun saveAlertSettings(settings: CampaignAlertSettings) = dao.upsertAlertSettings(settings.toRecord())

    override suspend fun sync(session: UserSession, manageableCampaignIds: Set<String>) {
        val store = runCatching { Firebase.firestore }.getOrNull() ?: return
        val operations = store.collection(OPERATIONS)
        val library = store.collection(LIBRARY)
        val deliveries = store.collection(DELIVERIES)
        dao.dirtyOperations().filter { it.actorId == session.uid }.forEach { local ->
            // Audit entries are immutable. Reading a missing document first cannot be authorized
            // by rules that inspect resource.data, so create directly and treat an existing
            // idempotency key as an already completed retry.
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
        dao.dirtyLibrary().filter { it.campaignId in manageableCampaignIds }.forEach { local ->
            library.document(local.id).set(local.copy(dirty = false), SetOptions.merge()).await()
            dao.markLibrarySynced(local.id, local.updatedAt)
        }
        dao.dirtyDeliveries().filter { it.createdBy == session.uid || it.recipientId == session.uid }.forEach { local ->
            deliveries.document(local.id).set(local.copy(dirty = false), SetOptions.merge()).await()
            dao.markDeliverySynced(local.id, local.updatedAt)
        }
        manageableCampaignIds.forEach { campaignId ->
            operations.whereEqualTo("campaignId", campaignId).get().await().documents.mapNotNull { it.toObject(SessionOperationRecord::class.java) }
                .forEach { dao.upsertOperation(it.copy(dirty = false, lastSyncedAt = it.createdAt)) }
            library.whereEqualTo("campaignId", campaignId).get().await().documents.mapNotNull { it.toObject(CampaignLibraryRecord::class.java) }
                .forEach { dao.upsertLibrary(it.copy(dirty = false, lastSyncedAt = it.updatedAt)) }
            deliveries.whereEqualTo("campaignId", campaignId).get().await().documents.mapNotNull { it.toObject(CampaignDeliveryRecord::class.java) }
                .forEach { dao.upsertDelivery(it.copy(dirty = false, lastSyncedAt = it.updatedAt)) }
        }
        deliveries.whereEqualTo("recipientId", session.uid).get().await().documents.mapNotNull { it.toObject(CampaignDeliveryRecord::class.java) }
            .forEach { dao.upsertDelivery(it.copy(dirty = false, lastSyncedAt = it.updatedAt)) }
    }

    private companion object {
        const val OPERATIONS = "campaignAudit"
        const val LIBRARY = "campaignLibrary"
        const val DELIVERIES = "campaignDeliveries"
    }
}
