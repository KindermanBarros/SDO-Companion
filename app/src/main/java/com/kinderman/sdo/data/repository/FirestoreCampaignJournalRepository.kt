package com.kinderman.sdo.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignJournalDraft
import com.kinderman.sdo.domain.model.CampaignJournalEntry
import com.kinderman.sdo.domain.model.CampaignJournalPolicy
import com.kinderman.sdo.domain.model.CampaignState
import com.kinderman.sdo.domain.model.JournalSaveResult
import com.kinderman.sdo.domain.model.JournalSnapshot
import com.kinderman.sdo.domain.model.JournalSyncState
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.repository.CampaignJournalRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreCampaignJournalRepository : CampaignJournalRepository {
    private val cache = ConcurrentHashMap<String, List<CampaignJournalEntry>>()

    override fun observe(session: UserSession, campaign: Campaign): Flow<JournalSnapshot> = callbackFlow {
        if (campaign.isDeleted) {
            cache.remove(cacheKey(session.uid, campaign.id))
            trySend(JournalSnapshot(emptyList(), JournalSyncState.ACCESS_REVOKED))
            close()
            return@callbackFlow
        }
        val key = cacheKey(session.uid, campaign.id)
        trySend(JournalSnapshot(cache[key].orEmpty(), JournalSyncState.LOADING))
        val registration = runCatching {
            Firebase.firestore
                .collection(FirestoreCollections.CAMPAIGN_JOURNAL)
                .whereEqualTo("campaignId", campaign.id)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Journal listener error code=${error.code}")
                        if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            cache.remove(key)
                            trySend(JournalSnapshot(emptyList(), JournalSyncState.ACCESS_REVOKED))
                        } else {
                            val cached = cache[key].orEmpty()
                            val state = if (cached.isNotEmpty()) {
                                JournalSyncState.CACHED_OFFLINE
                            } else if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                                JournalSyncState.UNAVAILABLE_OFFLINE
                            } else {
                                JournalSyncState.ERROR
                            }
                            trySend(JournalSnapshot(cached, state, error.message))
                        }
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener

                    val entries = snapshot.documents
                        .mapNotNull(::toJournalEntryOrNull)
                        .filterNot(CampaignJournalEntry::deleted)
                        .sortedWith(compareByDescending<CampaignJournalEntry> { it.createdAt }.thenByDescending { it.id })

                    if (!snapshot.metadata.isFromCache) {
                        cache[key] = entries
                        val state = if (campaign.isArchived) JournalSyncState.ARCHIVED else JournalSyncState.LIVE
                        trySend(JournalSnapshot(entries, state))
                    } else {
                        val cached = entries.ifEmpty { cache[key].orEmpty() }
                        val state = when {
                            campaign.isArchived && cached.isNotEmpty() -> JournalSyncState.ARCHIVED
                            cached.isNotEmpty() -> JournalSyncState.CACHED_OFFLINE
                            else -> JournalSyncState.UNAVAILABLE_OFFLINE
                        }
                        trySend(JournalSnapshot(cached, state))
                    }
                }
        }.getOrElse { failure ->
            Log.w(TAG, "Journal listener setup failed type=${failure::class.simpleName}")
            trySend(JournalSnapshot(cache[key].orEmpty(), JournalSyncState.UNAVAILABLE_OFFLINE))
            null
        }
        awaitClose { registration?.remove() }
    }

    override suspend fun save(
        session: UserSession,
        campaign: Campaign,
        draft: CampaignJournalDraft,
        expectedRevision: Long?,
    ): JournalSaveResult {
        val trimmedTitle = draft.title.trim()
        val trimmedBody = draft.body.trim()
        CampaignJournalPolicy.validateDraft(trimmedTitle, trimmedBody)?.let { message ->
            return JournalSaveResult.ValidationError(message)
        }
        if (campaign.state != CampaignState.ACTIVE) {
            return JournalSaveResult.PermissionDenied("Campanha arquivada ou removida.")
        }
        val db = Firebase.firestore
        val campaignRef = db.collection(FirestoreCollections.CAMPAIGNS).document(campaign.id)
        val memberRef = db.collection(FirestoreCollections.CAMPAIGN_MEMBERS).document("${campaign.id}::${session.uid}")
        val entryRef = db.collection(FirestoreCollections.CAMPAIGN_JOURNAL).document(draft.entryId)
        val actorName = session.displayName.trim().ifBlank { session.email.substringBefore('@').ifBlank { "Participante" } }

        return try {
            val saved = db.runTransaction { tx ->
                val campaignDoc = tx.get(campaignRef)
                val campaignState = campaignDoc.getString("state")
                if (!campaignDoc.exists() || campaignState != CampaignState.ACTIVE.name) {
                    throw JournalPermissionException("Campanha indisponível para edição.")
                }
                val ownerId = campaignDoc.getString("ownerId").orEmpty()
                val isModerator = session.isAdmin || ownerId == session.uid
                if (!isModerator) {
                    val memberDoc = tx.get(memberRef)
                    if (!memberDoc.exists() || memberDoc.getString("state") != "ACTIVE") {
                        throw JournalPermissionException("Você não participa mais desta campanha.")
                    }
                }
                val existingDoc = tx.get(entryRef)
                val now = System.currentTimeMillis()
                if (!existingDoc.exists()) {
                    val payload = mapOf(
                        "id" to draft.entryId,
                        "campaignId" to campaign.id,
                        "authorId" to session.uid,
                        "authorName" to actorName,
                        "title" to trimmedTitle,
                        "body" to trimmedBody,
                        "createdAt" to now,
                        "updatedAt" to now,
                        "revision" to 1L,
                        "lastEditorId" to session.uid,
                        "lastEditorName" to actorName,
                        "operationId" to draft.operationId,
                        "deleted" to false,
                        "deletedAt" to null,
                    )
                    tx.set(entryRef, payload)
                    CampaignJournalEntry(
                        id = draft.entryId,
                        campaignId = campaign.id,
                        authorId = session.uid,
                        authorName = actorName,
                        title = trimmedTitle,
                        body = trimmedBody,
                        createdAt = now,
                        updatedAt = now,
                        revision = 1L,
                        lastEditorId = session.uid,
                        lastEditorName = actorName,
                        operationId = draft.operationId,
                        deleted = false,
                        deletedAt = null,
                    )
                } else {
                    val existing = toJournalEntryOrNull(existingDoc)
                        ?: throw JournalPermissionException("Registro inválido.")
                    if (existing.operationId == draft.operationId && !existing.deleted) {
                        return@runTransaction existing
                    }
                    if (existing.campaignId != campaign.id) {
                        throw JournalPermissionException("Registro pertence a outra campanha.")
                    }
                    if (!isModerator && existing.authorId != session.uid) {
                        throw JournalPermissionException("Sem permissão para editar este registro.")
                    }
                    if (expectedRevision != null &&
                        existing.revision != expectedRevision &&
                        existing.lastEditorId != session.uid
                    ) {
                        throw JournalConflictException(existing)
                    }
                    val nextRevision = existing.revision + 1L
                    val payload = mapOf(
                        "title" to trimmedTitle,
                        "body" to trimmedBody,
                        "updatedAt" to now,
                        "revision" to nextRevision,
                        "lastEditorId" to session.uid,
                        "lastEditorName" to actorName,
                        "operationId" to draft.operationId,
                        "deleted" to false,
                        "deletedAt" to null,
                    )
                    tx.update(entryRef, payload)
                    existing.copy(
                        title = trimmedTitle,
                        body = trimmedBody,
                        updatedAt = now,
                        revision = nextRevision,
                        lastEditorId = session.uid,
                        lastEditorName = actorName,
                        operationId = draft.operationId,
                        deleted = false,
                        deletedAt = null,
                    )
                }
            }.await()
            JournalSaveResult.Confirmed(saved)
        } catch (conflict: JournalConflictException) {
            JournalSaveResult.Conflict(conflict.serverEntry)
        } catch (denied: JournalPermissionException) {
            JournalSaveResult.PermissionDenied(denied.message ?: "Sem permissão.")
        } catch (fs: FirebaseFirestoreException) {
            Log.w(TAG, "Journal save failed code=${fs.code}")
            when (fs.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    JournalSaveResult.PermissionDenied("Você não tem mais acesso a este diário.")
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    JournalSaveResult.Offline("Sem conexão. Seu rascunho está preservado. Reconecte-se para salvar.")
                else ->
                    JournalSaveResult.Uncertain(
                        draft.entryId,
                        draft.operationId,
                        "Não foi possível confirmar o salvamento. Seu rascunho está preservado.",
                    )
            }
        } catch (other: Exception) {
            Log.w(TAG, "Journal save uncertain type=${other::class.simpleName}")
            JournalSaveResult.Uncertain(
                draft.entryId,
                draft.operationId,
                "Não foi possível confirmar o salvamento. Seu rascunho está preservado.",
            )
        }
    }

    override suspend fun delete(
        session: UserSession,
        campaign: Campaign,
        entry: CampaignJournalEntry,
        operationId: String,
    ): JournalSaveResult {
        if (campaign.state != CampaignState.ACTIVE) {
            return JournalSaveResult.PermissionDenied("Campanha arquivada ou removida.")
        }
        val db = Firebase.firestore
        val campaignRef = db.collection(FirestoreCollections.CAMPAIGNS).document(campaign.id)
        val memberRef = db.collection(FirestoreCollections.CAMPAIGN_MEMBERS).document("${campaign.id}::${session.uid}")
        val entryRef = db.collection(FirestoreCollections.CAMPAIGN_JOURNAL).document(entry.id)
        val actorName = session.displayName.trim().ifBlank { session.email.substringBefore('@').ifBlank { "Participante" } }

        return try {
            val deleted = db.runTransaction { tx ->
                val campaignDoc = tx.get(campaignRef)
                if (!campaignDoc.exists() || campaignDoc.getString("state") != CampaignState.ACTIVE.name) {
                    throw JournalPermissionException("Campanha indisponível.")
                }
                val ownerId = campaignDoc.getString("ownerId").orEmpty()
                val isModerator = session.isAdmin || ownerId == session.uid
                if (!isModerator) {
                    val memberDoc = tx.get(memberRef)
                    if (!memberDoc.exists() || memberDoc.getString("state") != "ACTIVE") {
                        throw JournalPermissionException("Você não participa mais desta campanha.")
                    }
                }
                val existingDoc = tx.get(entryRef)
                val existing = toJournalEntryOrNull(existingDoc)
                    ?: throw JournalPermissionException("Registro não encontrado.")
                if (existing.deleted) return@runTransaction existing
                if (!isModerator && existing.authorId != session.uid) {
                    throw JournalPermissionException("Sem permissão para excluir este registro.")
                }
                val now = System.currentTimeMillis()
                val nextRevision = existing.revision + 1L
                tx.update(
                    entryRef,
                    mapOf(
                        "deleted" to true,
                        "deletedAt" to now,
                        "updatedAt" to now,
                        "revision" to nextRevision,
                        "lastEditorId" to session.uid,
                        "lastEditorName" to actorName,
                        "operationId" to operationId,
                    ),
                )
                existing.copy(
                    deleted = true,
                    deletedAt = now,
                    updatedAt = now,
                    revision = nextRevision,
                    lastEditorId = session.uid,
                    lastEditorName = actorName,
                    operationId = operationId,
                )
            }.await()
            JournalSaveResult.Confirmed(deleted)
        } catch (denied: JournalPermissionException) {
            JournalSaveResult.PermissionDenied(denied.message ?: "Sem permissão.")
        } catch (fs: FirebaseFirestoreException) {
            Log.w(TAG, "Journal delete failed code=${fs.code}")
            when (fs.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    JournalSaveResult.PermissionDenied("Você não tem mais acesso a este diário.")
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    JournalSaveResult.Offline("Sem conexão. Reconecte-se para excluir.")
                else ->
                    JournalSaveResult.Uncertain(entry.id, operationId, "Não foi possível confirmar a exclusão.")
            }
        } catch (other: Exception) {
            Log.w(TAG, "Journal delete uncertain type=${other::class.simpleName}")
            JournalSaveResult.Uncertain(entry.id, operationId, "Não foi possível confirmar a exclusão.")
        }
    }

    override suspend fun verifyOperation(
        session: UserSession,
        campaignId: String,
        entryId: String,
        operationId: String,
    ): CampaignJournalEntry? {
        val doc = runCatching {
            Firebase.firestore
                .collection(FirestoreCollections.CAMPAIGN_JOURNAL)
                .document(entryId)
                .get(Source.SERVER)
                .await()
        }.getOrNull() ?: return null
        val entry = toJournalEntryOrNull(doc) ?: return null
        return if (entry.campaignId == campaignId && entry.operationId == operationId) entry else null
    }

    override fun clearAccountCache(uid: String) {
        val prefix = "$uid::"
        cache.keys.filter { it.startsWith(prefix) }.forEach(cache::remove)
    }

    private fun cacheKey(uid: String, campaignId: String): String = "$uid::$campaignId"

    private fun toJournalEntryOrNull(doc: DocumentSnapshot): CampaignJournalEntry? {
        if (!doc.exists()) return null
        val campaignId = doc.getString("campaignId")?.trim().orEmpty()
        val authorId = doc.getString("authorId")?.trim().orEmpty()
        if (campaignId.isEmpty() || authorId.isEmpty()) return null
        val rawAuthorName = doc.getString("authorName").orEmpty()
        val rawLastEditor = doc.getString("lastEditorName").orEmpty()
        return CampaignJournalEntry(
            id = doc.getString("id")?.takeIf { it.isNotBlank() } ?: doc.id,
            campaignId = campaignId,
            authorId = authorId,
            authorName = CampaignJournalPolicy.displayAuthor(rawAuthorName, isActiveMemberOrOwner = true),
            title = doc.getString("title").orEmpty(),
            body = doc.getString("body").orEmpty(),
            createdAt = doc.getLong("createdAt") ?: 0L,
            updatedAt = doc.getLong("updatedAt") ?: 0L,
            revision = (doc.getLong("revision") ?: 1L).coerceAtLeast(1L),
            lastEditorId = doc.getString("lastEditorId").orEmpty(),
            lastEditorName = CampaignJournalPolicy.displayAuthor(rawLastEditor.ifBlank { rawAuthorName }, isActiveMemberOrOwner = true),
            operationId = doc.getString("operationId").orEmpty(),
            deleted = doc.getBoolean("deleted") ?: false,
            deletedAt = doc.getLong("deletedAt"),
        )
    }

    private class JournalPermissionException(message: String) : RuntimeException(message)
    private class JournalConflictException(val serverEntry: CampaignJournalEntry) : RuntimeException()

    private companion object {
        const val TAG = "SDO_JOURNAL"
    }
}
