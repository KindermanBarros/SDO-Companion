package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.CampaignDao
import com.kinderman.sdo.data.local.CampaignInviteRecord
import com.kinderman.sdo.data.local.CampaignMemberRecord
import com.kinderman.sdo.data.local.CampaignRecord
import com.kinderman.sdo.data.local.CharacterDao
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.data.sync.SparkFirestoreSyncEngine
import com.kinderman.sdo.data.sync.SparkWireCodec
import com.kinderman.sdo.data.sync.normalizeFirestoreMap
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignInvite
import com.kinderman.sdo.domain.model.CampaignInvitePreview
import com.kinderman.sdo.domain.model.CampaignMember
import com.kinderman.sdo.domain.model.CampaignMemberState
import com.kinderman.sdo.domain.model.CampaignRole
import com.kinderman.sdo.domain.model.CampaignState
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.OperationType
import com.kinderman.sdo.domain.model.SparkTargetSpec
import com.kinderman.sdo.domain.model.TargetKind
import com.kinderman.sdo.domain.model.TargetOp
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.computeCanonicalPayloadHash
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.domain.repository.CampaignRepository
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class SyncedCampaignRepository(
    private val dao: CampaignDao,
    private val characterDao: CharacterDao,
    private val syncEngine: SparkFirestoreSyncEngine? = null,
) : CampaignRepository {
    private val syncMutex = Mutex()
    private val random = SecureRandom()

    override fun observe(session: UserSession): Flow<List<Campaign>> =
        (if (session.isAdmin) dao.observeAll() else dao.observeForUser(session.uid))
            .map { values -> values.map(CampaignRecord::toDomain).filterNot(Campaign::isDeleted) }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun observeMembers(campaignId: String): Flow<List<CampaignMember>> =
        dao.observeMembers(campaignId).map { values -> values.map(CampaignMemberRecord::toDomain) }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun observeMemberships(session: UserSession): Flow<List<CampaignMember>> =
        dao.observeMemberships(session.uid).map { values -> values.map(CampaignMemberRecord::toDomain) }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun observeInvites(campaignId: String): Flow<List<CampaignInvite>> =
        dao.observeInvites(campaignId).map { values -> values.map(CampaignInviteRecord::toDomain) }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override fun observeAllInvites(): Flow<List<CampaignInvite>> =
        dao.observeAllInvites().map { values -> values.map(CampaignInviteRecord::toDomain) }
            .distinctUntilChanged().flowOn(Dispatchers.Default)

    override suspend fun create(session: UserSession, name: String, description: String): Campaign {
        val now = System.currentTimeMillis()
        val campaign = Campaign(
            name = name.trim().ifBlank { "Nova campanha" },
            description = description.trim(),
            ownerId = session.uid,
            createdAt = now,
            updatedAt = now,
            dirty = true,
        )
        dao.upsertCampaign(campaign.toRecord())
        dao.upsertMember(
            CampaignMember(
                campaignId = campaign.id,
                userId = session.uid,
                role = CampaignRole.PLAYER,
                state = CampaignMemberState.ACTIVE,
                joinedAt = now,
                updatedAt = now,
                dirty = true,
            ).toRecord(),
        )
        createStaticInvite(session, campaign, now)
        return campaign
    }

    override suspend fun update(session: UserSession, campaign: Campaign) {
        requireOwner(session, campaign)
        val existing = requireActiveCampaign(campaign.id)
        dao.upsertCampaign(
            campaign.copy(
                ownerId = existing.ownerId,
                state = existing.state,
                archivedAt = existing.archivedAt,
                createdAt = existing.createdAt,
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            ).toRecord(),
        )
    }

    override suspend fun archive(session: UserSession, campaign: Campaign, archived: Boolean) {
        requireOwner(session, campaign)
        val existing = requireCampaign(campaign.id)
        check(!existing.isDeleted) { "Campanhas excluídas não podem ser restauradas." }
        val now = System.currentTimeMillis()
        dao.upsertCampaign(
            existing.copy(
                state = if (archived) CampaignState.ARCHIVED else CampaignState.ACTIVE,
                archivedAt = if (archived) now else null,
                updatedAt = now,
                dirty = true,
            ).toRecord(),
        )
    }

    override suspend fun delete(session: UserSession, campaign: Campaign) = syncMutex.withLock {
        requireOwner(session, requireCampaign(campaign.id))
        val store = Firebase.firestore
        val reference = store.collection(FirestoreCollections.CAMPAIGNS).document(campaign.id)
        val remote = reference.get(Source.SERVER).await()
        check(remote.exists()) { "Sincronize a campanha antes de excluí-la." }
        check(session.isAdmin || remote.getString("ownerId") == session.uid) { "Sem permissão para excluir." }
        val characters = store.collection(FirestoreCollections.CHARACTERS)
            .whereEqualTo("campaignId", campaign.id)
            .get(Source.SERVER)
            .await()
            .documents
        check(characters.size <= 200) {
            "Desvincule algumas fichas antes de excluir: o limite seguro por operação é de 200 fichas."
        }
        val now = System.currentTimeMillis()
        val batch = store.batch()
        batch.update(reference, mapOf("state" to CampaignState.DELETED.name, "updatedAt" to now))
        characters.forEach { charSnap ->
            val data = normalizeFirestoreMap(charSnap.data.orEmpty())
            val prevRev = (data["revision"] as? Number)?.toLong() ?: 1L
            val nextRev = prevRev + 1L
            val opId = UUID.randomUUID().toString()
            val target = SparkTargetSpec(
                kind = TargetKind.SHEET,
                op = TargetOp.UNLINK,
                expectedRevision = prevRev,
                nextRevision = nextRev,
            )
            val payloadHash = computeCanonicalPayloadHash(
                sheetId = charSnap.id,
                type = OperationType.UNLINK_CAMPAIGN,
                targetKey = target.canonicalTargetKey(),
                targets = listOf(target),
            )
            val opRef = charSnap.reference.collection(FirestoreCollections.CHARACTER_OPERATIONS).document(opId)
            batch.set(
                opRef,
                linkedMapOf(
                    "id" to opId,
                    "actorUid" to session.uid,
                    "type" to OperationType.UNLINK_CAMPAIGN.name,
                    "outcome" to "COMMITTED",
                    "schemaVersion" to SparkWireCodec.SCHEMA_VERSION,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "targets" to listOf(
                        linkedMapOf(
                            "kind" to TargetKind.SHEET.name,
                            "itemId" to "",
                            "key" to "",
                            "op" to TargetOp.UNLINK.name,
                            "expectedRevision" to prevRev,
                            "nextRevision" to nextRev,
                        ),
                    ),
                    "preconditions" to mapOf("payloadHash" to payloadHash),
                ),
            )
            val nextRoot = LinkedHashMap<String, Any?>(charSnap.data.orEmpty()).apply {
                put("campaignId", "")
                put("authorizedCampaignId", "")
                put("revision", nextRev)
                put("lastOperationId", opId)
                put("updatedBy", session.uid)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            batch.set(charSnap.reference, nextRoot)
        }
        batch.commit().await()
        characterDao.all().filter { it.campaignId == campaign.id }.forEach { record ->
            if (record.ownerId == session.uid) {
                syncEngine?.recordConfirmedSheetLinkUpdate(session.uid, record.id, "", 1L)
            } else {
                syncEngine?.purgeSheetForNonOwner(session.uid, record.id)
            }
        }
        dao.upsertCampaign(
            requireCampaign(campaign.id).copy(
                state = CampaignState.DELETED,
                updatedAt = now,
                dirty = false,
                lastSyncedAt = now,
            ).toRecord(),
        )
    }

    override suspend fun leave(session: UserSession, campaign: Campaign) {
        val existing = requireActiveCampaign(campaign.id)
        check(existing.ownerId != session.uid) { "A Mestre que criou a campanha não pode sair dela." }
        val member = dao.member(existing.id, session.uid)?.toDomain()
            ?: error("Você não participa desta campanha.")
        val now = System.currentTimeMillis()
        detachMemberCharacters(session, member, existing.id)
        dao.upsertMember(
            member.copy(
                state = CampaignMemberState.LEFT,
                characterIds = emptyList(),
                updatedAt = now,
                dirty = true,
            ).toRecord(),
        )
    }

    override suspend fun removeMember(session: UserSession, campaign: Campaign, userId: String) {
        requireOwner(session, campaign)
        val existing = requireActiveCampaign(campaign.id)
        check(userId != existing.ownerId) { "A Mestre principal não pode ser removida." }
        val member = dao.member(existing.id, userId)?.toDomain() ?: return
        val now = System.currentTimeMillis()
        detachMemberCharacters(session, member, existing.id)
        dao.upsertMember(
            member.copy(
                state = CampaignMemberState.REMOVED,
                characterIds = emptyList(),
                updatedAt = now,
                dirty = true,
            ).toRecord(),
        )
    }

    override suspend fun createInvite(session: UserSession, campaign: Campaign): CampaignInvite {
        requireOwner(session, campaign)
        val existing = requireActiveCampaign(campaign.id)
        return dao.canonicalInvite(existing.id)?.toDomain()?.takeIf { it.isUsable() }
            ?: createStaticInvite(session, existing, System.currentTimeMillis())
    }

    override suspend fun previewInvite(session: UserSession, code: String): CampaignInvitePreview? {
        val normalized = normalizeCode(code)
        if (normalized.isBlank()) return null
        val local = dao.inviteByCode(normalized)?.toDomain()?.takeIf { it.isUsable() }
        val invite = local ?: run {
            val store = Firebase.firestore
            val snapshot = store.collectionGroup(FirestoreCollections.CAMPAIGN_INVITES_SUBCOLLECTION)
                .whereEqualTo("code", normalized)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.let(::snapshotToInvite)
        } ?: return null

        if (!invite.isUsable()) return null
        if (invite.recipientUid.isNotBlank() && invite.recipientUid != session.uid) return null

        val campaign = dao.campaign(invite.campaignId)?.toDomain() ?: run {
            val doc = runCatching {
                Firebase.firestore.collection(FirestoreCollections.CAMPAIGNS)
                    .document(invite.campaignId)
                    .get()
                    .await()
            }.getOrNull()
            doc?.toObject(CampaignRecord::class.java)?.copy(id = doc.id)?.toDomain()
        } ?: Campaign(
            id = invite.campaignId,
            name = invite.campaignName.ifBlank { "Campanha" },
            description = invite.campaignDescription,
            ownerId = invite.createdBy,
            state = CampaignState.ACTIVE,
            dirty = false,
        )
        if (campaign.isArchived || campaign.isDeleted) return null
        return CampaignInvitePreview(
            campaign = campaign,
            invite = invite,
            alreadyMember = dao.member(campaign.id, session.uid)?.toDomain()?.isActive == true,
        )
    }

    override suspend fun joinByCode(session: UserSession, code: String): Campaign = syncMutex.withLock {
        val preview = previewInvite(session, code) ?: error("Convite inválido, revogado ou expirado.")
        val store = Firebase.firestore
        val campaignRef = store.collection(FirestoreCollections.CAMPAIGNS).document(preview.campaign.id)
        val inviteRef = campaignRef.collection(FirestoreCollections.CAMPAIGN_INVITES_SUBCOLLECTION)
            .document(preview.invite.id)
        val memberRef = campaignRef.collection(FirestoreCollections.CAMPAIGN_MEMBERS_SUBCOLLECTION)
            .document(session.uid)

        val (membership, consumedInvite) = store.runTransaction { transaction ->
            val campaignSnap = transaction.get(campaignRef)
            check(campaignSnap.exists() && campaignSnap.getString("state") == CampaignState.ACTIVE.name) {
                "Esta campanha não está ativa."
            }
            val inviteSnap = transaction.get(inviteRef)
            val invite = snapshotToInvite(inviteSnap)
            check(
                invite != null &&
                    invite.isUsable() &&
                    invite.campaignId == preview.campaign.id &&
                    (invite.recipientUid.isBlank() || invite.recipientUid == session.uid),
            ) {
                "Convite inválido, revogado ou expirado."
            }
            val previousSnap = transaction.get(memberRef)
            val previous = if (previousSnap.exists()) {
                previousSnap.toObject(CampaignMemberRecord::class.java)?.toDomain()
            } else {
                null
            }
            if (previous?.isActive == true) {
                return@runTransaction previous to invite
            }
            check(previous?.state != CampaignMemberState.REMOVED) {
                "Você foi removido desta campanha. Peça à Mestre para vincular sua ficha novamente."
            }

            val now = System.currentTimeMillis()
            val joined = (previous ?: CampaignMember(
                campaignId = preview.campaign.id,
                userId = session.uid,
                joinedAt = now,
            )).copy(
                role = CampaignRole.PLAYER,
                state = CampaignMemberState.ACTIVE,
                joinedByInviteId = invite.id,
                updatedAt = now,
                dirty = false,
                lastSyncedAt = now,
            )

            // Atomic single-use PENDING -> CONSUMED coupled with member activation via Rules getAfter.
            transaction.update(
                inviteRef,
                mapOf(
                    "state" to "CONSUMED",
                    "consumedBy" to session.uid,
                    "consumedAt" to FieldValue.serverTimestamp(),
                ),
            )
            transaction.set(memberRef, memberToFirestoreMap(joined))
            joined to invite.copy(state = "CONSUMED", consumedBy = session.uid, consumedAt = now, dirty = false)
        }.await()

        dao.upsertCampaign(preview.campaign.copy(dirty = false).toRecord())
        dao.upsertInvite(consumedInvite.toRecord())
        dao.upsertMember(membership.toRecord())
        preview.campaign
    }

    /**
     * Owner pre-authorizes a GM link on [character] by setting `authorizedCampaignId = campaignId`
     * via an atomic Spark v2 `LINK_CAMPAIGN` transaction.
     */
    suspend fun authorizeCampaignLink(
        session: UserSession,
        character: Character,
        campaignId: String,
    ) = syncMutex.withLock {
        val targetCid = normalizeCampaignId(campaignId)
        check(targetCid.isNotBlank()) { "Campanha inválida." }
        check(character.ownerId == session.uid) { "Somente o dono pode autorizar a vinculação da ficha." }
        syncEngine?.drain(session.uid)
        val store = Firebase.firestore
        val sheetRef = store.collection(FirestoreCollections.CHARACTERS).document(character.id)
        store.runTransaction { transaction ->
            val sheetSnap = transaction.get(sheetRef)
            check(sheetSnap.exists()) { "Sincronize a ficha antes de autorizar o vínculo." }
            val rawRoot = sheetSnap.data.orEmpty()
            val prevRev = (rawRoot["revision"] as? Number)?.toLong() ?: 1L
            val nextRev = prevRev + 1L
            val opId = UUID.randomUUID().toString()
            val target = SparkTargetSpec(
                kind = TargetKind.SHEET,
                op = TargetOp.LINK,
                expectedRevision = prevRev,
                nextRevision = nextRev,
                valueJson = "{\"authorizedCampaignId\":\"$targetCid\"}",
            )
            val payloadHash = computeCanonicalPayloadHash(
                sheetId = character.id,
                type = OperationType.LINK_CAMPAIGN,
                targetKey = target.canonicalTargetKey(),
                targets = listOf(target),
            )
            val opRef = sheetRef.collection(FirestoreCollections.CHARACTER_OPERATIONS).document(opId)
            transaction.set(
                opRef,
                linkedMapOf(
                    "id" to opId,
                    "actorUid" to session.uid,
                    "type" to OperationType.LINK_CAMPAIGN.name,
                    "outcome" to "COMMITTED",
                    "schemaVersion" to SparkWireCodec.SCHEMA_VERSION,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "targets" to listOf(
                        linkedMapOf(
                            "kind" to TargetKind.SHEET.name,
                            "itemId" to "",
                            "key" to "",
                            "op" to TargetOp.LINK.name,
                            "expectedRevision" to prevRev,
                            "nextRevision" to nextRev,
                            "desiredValue" to mapOf("authorizedCampaignId" to targetCid),
                        ),
                    ),
                    "preconditions" to mapOf("payloadHash" to payloadHash, "campaignId" to targetCid),
                ),
            )
            val nextRoot = LinkedHashMap<String, Any?>(rawRoot).apply {
                put("campaignId", "")
                put("authorizedCampaignId", targetCid)
                put("revision", nextRev)
                put("lastOperationId", opId)
                put("updatedBy", session.uid)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            transaction.set(sheetRef, nextRoot)
        }.await()
    }

    override suspend fun linkCharacter(
        session: UserSession,
        character: Character,
        campaign: Campaign,
    ): Character = syncMutex.withLock {
        val existing = requireActiveCampaign(campaign.id)
        val isGm = existing.ownerId == session.uid
        val isOwner = character.ownerId == session.uid
        check(isGm || isOwner || session.isAdmin) {
            "Somente o dono ou a Mestre da campanha pode vincular esta ficha."
        }
        if (isOwner) {
            syncEngine?.drain(session.uid)
        }

        val store = Firebase.firestore
        val campaignRef = store.collection(FirestoreCollections.CAMPAIGNS).document(existing.id)
        val sheetRef = store.collection(FirestoreCollections.CHARACTERS).document(character.id)
        val memberRef = campaignRef.collection(FirestoreCollections.CAMPAIGN_MEMBERS_SUBCOLLECTION)
            .document(character.ownerId)

        val (nextRev, membership) = store.runTransaction { transaction ->
            val remoteCampaign = transaction.get(campaignRef).toObject(CampaignRecord::class.java)
            check(remoteCampaign != null && remoteCampaign.state == CampaignState.ACTIVE.name) {
                "Esta campanha não está ativa."
            }
            val sheetSnap = transaction.get(sheetRef)
            check(sheetSnap.exists()) { "Sincronize a ficha antes de vinculá-la à campanha." }
            val rawRoot = sheetSnap.data.orEmpty()
            val remoteOwnerId = rawRoot["ownerId"] as? String ?: ""
            check(remoteOwnerId == character.ownerId) {
                "O dono da ficha mudou. Sincronize antes de vinculá-la."
            }
            check(rawRoot["deleted"] as? Boolean != true) {
                "A ficha foi excluída online e não pode ser vinculada."
            }
            val prevCid = normalizeCampaignId(rawRoot["campaignId"] as? String ?: "")
            check(prevCid.isBlank() || prevCid == existing.id) {
                "A ficha já está vinculada a outra campanha ativa."
            }
            val authorizedCid = normalizeCampaignId(rawRoot["authorizedCampaignId"] as? String ?: "")
            if (!isOwner) {
                check(remoteCampaign.ownerId == session.uid) {
                    "Somente o dono ou a Mestre da campanha pode vincular esta ficha."
                }
                check(prevCid == existing.id || authorizedCid == existing.id) {
                    "O dono da ficha precisa autorizar o vínculo com esta campanha antes da Mestre vinculá-la."
                }
            }

            val memberSnap = transaction.get(memberRef)
            val ownerMembership = if (memberSnap.exists()) {
                memberSnap.toObject(CampaignMemberRecord::class.java)?.toDomain()
            } else {
                null
            }
            if (isOwner && remoteCampaign.ownerId != session.uid) {
                check(ownerMembership?.isActive == true) { "Você não participa desta campanha." }
            }

            val now = System.currentTimeMillis()
            val activatedMembership = (ownerMembership ?: CampaignMember(
                campaignId = existing.id,
                userId = character.ownerId,
                joinedAt = now,
            )).copy(
                role = CampaignRole.PLAYER,
                state = CampaignMemberState.ACTIVE,
                characterIds = (ownerMembership?.characterIds.orEmpty() + character.id).distinct(),
                updatedAt = now,
                dirty = false,
                lastSyncedAt = now,
            )

            val prevRev = (rawRoot["revision"] as? Number)?.toLong() ?: 1L
            val nextRevision = prevRev + 1L
            val opId = UUID.randomUUID().toString()
            val target = SparkTargetSpec(
                kind = TargetKind.SHEET,
                op = TargetOp.LINK,
                expectedRevision = prevRev,
                nextRevision = nextRevision,
                baseValueJson = "{\"campaignId\":\"$prevCid\"}",
                valueJson = "{\"campaignId\":\"${existing.id}\"}",
            )
            val payloadHash = computeCanonicalPayloadHash(
                sheetId = character.id,
                type = OperationType.LINK_CAMPAIGN,
                targetKey = target.canonicalTargetKey(),
                targets = listOf(target),
            )
            val opRef = sheetRef.collection(FirestoreCollections.CHARACTER_OPERATIONS).document(opId)

            transaction.set(
                opRef,
                linkedMapOf(
                    "id" to opId,
                    "actorUid" to session.uid,
                    "type" to OperationType.LINK_CAMPAIGN.name,
                    "outcome" to "COMMITTED",
                    "schemaVersion" to SparkWireCodec.SCHEMA_VERSION,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "targets" to listOf(
                        linkedMapOf(
                            "kind" to TargetKind.SHEET.name,
                            "itemId" to "",
                            "key" to "",
                            "op" to TargetOp.LINK.name,
                            "expectedRevision" to prevRev,
                            "nextRevision" to nextRevision,
                            "baseValue" to mapOf("campaignId" to prevCid),
                            "desiredValue" to mapOf("campaignId" to existing.id),
                        ),
                    ),
                    "preconditions" to mapOf("payloadHash" to payloadHash, "campaignId" to existing.id),
                ),
            )
            // Atomic member activation + sheet root update preserving ownerId and clearing authorizedCampaignId.
            transaction.set(memberRef, memberToFirestoreMap(activatedMembership))
            val nextRoot = LinkedHashMap<String, Any?>(rawRoot).apply {
                put("ownerId", remoteOwnerId)
                put("campaignId", existing.id)
                put("authorizedCampaignId", "")
                put("revision", nextRevision)
                put("lastOperationId", opId)
                put("updatedBy", session.uid)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            transaction.set(sheetRef, nextRoot)
            nextRevision to activatedMembership
        }.await()

        dao.upsertMember(membership.toRecord())
        val updatedChar = character.copy(campaignId = existing.id, dirty = false)
        syncEngine?.recordConfirmedSheetLinkUpdate(session.uid, character.id, existing.id, nextRev, updatedChar)
        val linked = characterDao.get(character.id)?.toDomain() ?: updatedChar
        linked.copy(editingBase = linked)
    }

    override suspend fun unlinkCharacter(session: UserSession, character: Character): Character = syncMutex.withLock {
        val campaignId = normalizeCampaignId(character.campaignId)
        if (campaignId.isBlank()) return@withLock character.copy(campaignId = "")
        val localCampaign = dao.campaign(campaignId)?.toDomain()
        val isOwner = character.ownerId == session.uid
        val isGm = localCampaign?.ownerId == session.uid
        check(session.isAdmin || isOwner || isGm) {
            "Somente o dono da ficha ou a Mestre da campanha pode desvincular esta ficha."
        }
        if (!isOwner && localCampaign?.isArchived == true) {
            error("Campanha arquivada: somente o dono pode desvincular a ficha.")
        }

        val store = Firebase.firestore
        val campaignRef = store.collection(FirestoreCollections.CAMPAIGNS).document(campaignId)
        val sheetRef = store.collection(FirestoreCollections.CHARACTERS).document(character.id)
        val memberRef = campaignRef.collection(FirestoreCollections.CAMPAIGN_MEMBERS_SUBCOLLECTION)
            .document(character.ownerId)

        val (nextRev, updatedMember) = store.runTransaction { transaction ->
            val campaignSnap = transaction.get(campaignRef)
            val remoteCampOwner = campaignSnap.getString("ownerId").orEmpty()
            val remoteCampState = campaignSnap.getString("state").orEmpty()
            check(isOwner || remoteCampOwner == session.uid) {
                "Somente o dono da ficha ou a Mestre da campanha pode desvincular esta ficha."
            }
            if (!isOwner && remoteCampState != CampaignState.ACTIVE.name) {
                error("Campanha arquivada: somente o dono pode desvincular a ficha.")
            }
            val sheetSnap = transaction.get(sheetRef)
            check(sheetSnap.exists()) { "Ficha remota não encontrada." }
            val rawRoot = sheetSnap.data.orEmpty()
            val memberSnap = transaction.get(memberRef)
            val prevMember = if (memberSnap.exists()) {
                memberSnap.toObject(CampaignMemberRecord::class.java)?.toDomain()
            } else {
                null
            }

            val prevRev = (rawRoot["revision"] as? Number)?.toLong() ?: 1L
            val nextRevision = prevRev + 1L
            val opId = UUID.randomUUID().toString()
            val target = SparkTargetSpec(
                kind = TargetKind.SHEET,
                op = TargetOp.UNLINK,
                expectedRevision = prevRev,
                nextRevision = nextRevision,
                baseValueJson = "{\"campaignId\":\"$campaignId\"}",
                valueJson = "{\"campaignId\":\"\"}",
            )
            val payloadHash = computeCanonicalPayloadHash(
                sheetId = character.id,
                type = OperationType.UNLINK_CAMPAIGN,
                targetKey = target.canonicalTargetKey(),
                targets = listOf(target),
            )
            val opRef = sheetRef.collection(FirestoreCollections.CHARACTER_OPERATIONS).document(opId)

            transaction.set(
                opRef,
                linkedMapOf(
                    "id" to opId,
                    "actorUid" to session.uid,
                    "type" to OperationType.UNLINK_CAMPAIGN.name,
                    "outcome" to "COMMITTED",
                    "schemaVersion" to SparkWireCodec.SCHEMA_VERSION,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "targets" to listOf(
                        linkedMapOf(
                            "kind" to TargetKind.SHEET.name,
                            "itemId" to "",
                            "key" to "",
                            "op" to TargetOp.UNLINK.name,
                            "expectedRevision" to prevRev,
                            "nextRevision" to nextRevision,
                            "baseValue" to mapOf("campaignId" to campaignId),
                            "desiredValue" to mapOf("campaignId" to ""),
                        ),
                    ),
                    "preconditions" to mapOf("payloadHash" to payloadHash),
                ),
            )

            val nextRoot = LinkedHashMap<String, Any?>(rawRoot).apply {
                put("campaignId", "")
                put("authorizedCampaignId", "")
                put("revision", nextRevision)
                put("lastOperationId", opId)
                put("updatedBy", session.uid)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            transaction.set(sheetRef, nextRoot)

            val now = System.currentTimeMillis()
            val nextMember = if (prevMember != null && character.id in prevMember.characterIds) {
                prevMember.copy(
                    characterIds = prevMember.characterIds - character.id,
                    updatedAt = now,
                    dirty = false,
                    lastSyncedAt = now,
                )
            } else {
                null
            }
            // Only update members/{uid} when campaign is ACTIVE (or GM); owner unlink on ARCHIVED keeps sheet-only atomic tx.
            if (nextMember != null && (remoteCampState == CampaignState.ACTIVE.name || remoteCampOwner == session.uid)) {
                transaction.set(memberRef, memberToFirestoreMap(nextMember))
            }
            nextRevision to nextMember
        }.await()

        if (updatedMember != null) {
            dao.upsertMember(updatedMember.toRecord())
        }
        val unlinked = character.copy(campaignId = "", dirty = false)
        if (isOwner) {
            syncEngine?.recordConfirmedSheetLinkUpdate(session.uid, character.id, "", nextRev, unlinked)
        } else {
            // Immediately revoke GM's local access and projection.
            syncEngine?.purgeSheetForNonOwner(session.uid, character.id)
        }
        unlinked
    }

    override suspend fun sync(session: UserSession) = syncMutex.withLock {
        val store = Firebase.firestore
        val campaigns = store.collection(FirestoreCollections.CAMPAIGNS)

        val deletedIds = dao.allCampaigns().filter { it.state == CampaignState.DELETED.name }.map { it.id }.toSet()
        val dirtyCampaigns = dao.dirtyCampaigns()
            .filterNot { it.id in deletedIds }
            .filter { session.isAdmin || it.ownerId == session.uid }
        val newCampaigns = dirtyCampaigns.filter { it.lastSyncedAt == 0L }
        val changedCampaigns = dirtyCampaigns.filter { it.lastSyncedAt != 0L }
        val dirtyMembers = dao.dirtyMembers().filterNot { it.campaignId in deletedIds }

        newCampaigns.forEach { local ->
            campaigns.document(local.id).set(local.copy(dirty = false), SetOptions.merge()).await()
            dao.markCampaignSynced(local.id, local.updatedAt)
        }
        dirtyMembers.forEach { local ->
            if (session.isAdmin || local.userId == session.uid || dao.campaign(local.campaignId)?.ownerId == session.uid) {
                val memberRef = campaigns.document(local.campaignId)
                    .collection(FirestoreCollections.CAMPAIGN_MEMBERS_SUBCOLLECTION)
                    .document(local.userId)
                memberRef.set(memberToFirestoreMap(local.toDomain().copy(role = CampaignRole.PLAYER)), SetOptions.merge()).await()
                dao.markMemberSynced(local.campaignId, local.userId, local.updatedAt)
            }
        }
        dao.dirtyInvites().filterNot { it.campaignId in deletedIds }.forEach { local ->
            if (session.isAdmin || dao.campaign(local.campaignId)?.ownerId == session.uid) {
                val reference = campaigns.document(local.campaignId)
                    .collection(FirestoreCollections.CAMPAIGN_INVITES_SUBCOLLECTION)
                    .document(local.id)
                try {
                    reference.set(inviteToFirestoreMap(local.toDomain())).await()
                } catch (error: FirebaseFirestoreException) {
                    if (error.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw error
                    val existing = reference.get().await()
                    val sameInvite = existing.exists() &&
                        existing.getString("campaignId") == local.campaignId &&
                        existing.getString("code") == local.code &&
                        existing.getString("createdBy") == local.createdBy
                    if (!sameInvite) throw error
                }
                dao.markInviteSynced(local.id, local.createdAt)
            }
        }
        changedCampaigns.forEach { local ->
            campaigns.document(local.id).set(local.copy(dirty = false), SetOptions.merge()).await()
            dao.markCampaignSynced(local.id, local.updatedAt)
        }

        val membershipSnapshot = store.collectionGroup(FirestoreCollections.CAMPAIGN_MEMBERS_SUBCOLLECTION)
            .whereEqualTo("userId", session.uid)
            .get()
            .await()
        val remoteMemberships = membershipSnapshot.documents.mapNotNull { document ->
            document.toObject(CampaignMemberRecord::class.java)
        }
        remoteMemberships.forEach { dao.upsertMember(it.copy(dirty = false, lastSyncedAt = it.updatedAt)) }
        if (!session.isAdmin) {
            val activeRemoteCampaignIds = remoteMemberships
                .filter { it.userId == session.uid && it.state == CampaignMemberState.ACTIVE.name }
                .mapTo(hashSetOf()) { it.campaignId }
            dao.allMembers()
                .filter { it.userId == session.uid && it.state == CampaignMemberState.ACTIVE.name && it.campaignId !in activeRemoteCampaignIds }
                .forEach { stale -> dao.purgeMember(stale.campaignId, stale.userId) }
        }

        val campaignIds = remoteMemberships
            .filter { it.state == CampaignMemberState.ACTIVE.name }
            .mapTo(mutableSetOf()) { it.campaignId }
        val visibleCampaignDocuments = campaigns.whereEqualTo("ownerId", session.uid).get().await().documents
        val alreadyFetchedIds = hashSetOf<String>()
        visibleCampaignDocuments.mapNotNull { document ->
            document.toObject(CampaignRecord::class.java)?.copy(id = document.id)
        }.forEach { campaign ->
            campaignIds += campaign.id
            alreadyFetchedIds += campaign.id
            dao.upsertCampaign(campaign.copy(dirty = false, lastSyncedAt = campaign.updatedAt))
        }
        val memberCampaignDocuments = coroutineScope {
            (campaignIds - alreadyFetchedIds).map { campaignId ->
                async { campaigns.document(campaignId).get().await() }
            }.awaitAll()
        }
        memberCampaignDocuments.forEach { document ->
            document.toObject(CampaignRecord::class.java)?.copy(id = document.id)?.let { remote ->
                dao.upsertCampaign(remote.copy(dirty = false, lastSyncedAt = remote.updatedAt))
                if (remote.state == CampaignState.DELETED.name) {
                    characterDao.inCampaign(remote.id).forEach {
                        if (it.ownerId == session.uid) {
                            syncEngine?.recordConfirmedSheetLinkUpdate(session.uid, it.id, "", 1L)
                        } else {
                            syncEngine?.purgeSheetForNonOwner(session.uid, it.id)
                        }
                    }
                }
            }
        }

        val readableIds = dao.allCampaigns()
            .filter { record ->
                record.state != CampaignState.DELETED.name && (
                    session.isAdmin ||
                        record.ownerId == session.uid ||
                        dao.member(record.id, session.uid)?.state == CampaignMemberState.ACTIVE.name
                    )
            }
            .mapTo(mutableSetOf()) { it.id }

        val readableMembersByCampaign = coroutineScope {
            readableIds.map { campaignId ->
                async {
                    campaignId to runCatching {
                        campaigns.document(campaignId)
                            .collection(FirestoreCollections.CAMPAIGN_MEMBERS_SUBCOLLECTION)
                            .get()
                            .await()
                            .documents
                            .mapNotNull { it.toObject(CampaignMemberRecord::class.java) }
                    }.getOrDefault(emptyList())
                }
            }.awaitAll()
        }
        readableMembersByCampaign.forEach { (_, remoteMembers) ->
            remoteMembers.forEach { remote ->
                val normalized = if (remote.role != CampaignRole.PLAYER.name) {
                    remote.copy(role = CampaignRole.PLAYER.name)
                } else {
                    remote
                }
                dao.upsertMember(normalized.copy(dirty = false, lastSyncedAt = normalized.updatedAt))
            }
        }

        val remoteInvites = coroutineScope {
            (campaignIds - deletedIds).map { campaignId ->
                async {
                    runCatching {
                        campaigns.document(campaignId)
                            .collection(FirestoreCollections.CAMPAIGN_INVITES_SUBCOLLECTION)
                            .get()
                            .await()
                            .documents
                            .mapNotNull(::snapshotToInvite)
                    }.getOrDefault(emptyList())
                }
            }.awaitAll().flatten()
        }
        remoteInvites.forEach { dao.upsertInvite(it.copy(dirty = false, lastSyncedAt = it.createdAt).toRecord()) }
    }

    private suspend fun detachMemberCharacters(session: UserSession, member: CampaignMember, campaignId: String) {
        member.characterIds.forEach { characterId ->
            val record = characterDao.get(characterId) ?: return@forEach
            if (normalizeCampaignId(record.campaignId) == campaignId) {
                runCatching { unlinkCharacter(session, record.toDomain()) }
            }
        }
    }

    private suspend fun requireCampaign(id: String): Campaign =
        dao.campaign(id)?.toDomain() ?: error("Campanha não encontrada no cache local.")

    private suspend fun requireActiveCampaign(id: String): Campaign = requireCampaign(id).also {
        check(!it.isArchived) { "Campanhas arquivadas ou excluídas não podem ser alteradas." }
    }

    private fun requireOwner(session: UserSession, campaign: Campaign) {
        check(session.isAdmin || campaign.ownerId == session.uid) {
            "Somente a responsável principal pode executar esta operação."
        }
    }

    private fun memberToFirestoreMap(member: CampaignMember): Map<String, Any?> = linkedMapOf(
        "campaignId" to member.campaignId,
        "userId" to member.userId,
        "role" to CampaignRole.PLAYER.name,
        "state" to member.state.name,
        "characterIds" to member.characterIds,
        "joinedAt" to member.joinedAt,
        "updatedAt" to member.updatedAt,
        "joinedByInviteId" to member.joinedByInviteId,
    )

    private fun inviteToFirestoreMap(invite: CampaignInvite): Map<String, Any?> = linkedMapOf(
        "id" to invite.id,
        "campaignId" to invite.campaignId,
        "code" to invite.code,
        "campaignName" to invite.campaignName,
        "campaignDescription" to invite.campaignDescription,
        "createdBy" to invite.createdBy,
        "recipientUid" to invite.recipientUid,
        "expiresAt" to invite.expiresAt,
        "state" to invite.state,
        "consumedBy" to (invite.consumedBy ?: ""),
        "consumedAt" to invite.consumedAt,
        "revokedAt" to invite.revokedAt,
        "createdAt" to invite.createdAt,
    )

    private fun snapshotToInvite(doc: DocumentSnapshot): CampaignInvite? {
        if (!doc.exists()) return null
        val campaignId = doc.getString("campaignId")
            ?.takeIf { it.isNotBlank() }
            ?: doc.reference.parent.parent?.id
            ?: return null
        val consumedAtMs = doc.getTimestamp("consumedAt")?.toDate()?.time
            ?: doc.getLong("consumedAt")
        val consumedByRaw = doc.getString("consumedBy").orEmpty()
        return CampaignInvite(
            id = doc.getString("id") ?: doc.id,
            campaignId = campaignId,
            campaignName = doc.getString("campaignName").orEmpty(),
            campaignDescription = doc.getString("campaignDescription").orEmpty(),
            code = doc.getString("code").orEmpty(),
            createdBy = doc.getString("createdBy").orEmpty(),
            createdAt = doc.getLong("createdAt") ?: 0L,
            expiresAt = doc.getLong("expiresAt"),
            revokedAt = doc.getLong("revokedAt"),
            recipientUid = doc.getString("recipientUid").orEmpty(),
            state = doc.getString("state") ?: "PENDING",
            consumedBy = consumedByRaw.takeIf { it.isNotBlank() },
            consumedAt = consumedAtMs,
            dirty = false,
            lastSyncedAt = doc.getLong("createdAt") ?: 0L,
        )
    }

    private fun generateCode(): String = buildString(CODE_LENGTH) {
        repeat(CODE_LENGTH) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
    }

    private fun normalizeCode(value: String): String = value.trim().uppercase().filter { it in CODE_ALPHABET }

    private suspend fun createStaticInvite(
        session: UserSession,
        campaign: Campaign,
        createdAt: Long,
    ): CampaignInvite {
        val code = generateCode()
        return CampaignInvite(
            id = code,
            campaignId = campaign.id,
            campaignName = campaign.name,
            campaignDescription = campaign.description,
            code = code,
            createdBy = session.uid,
            createdAt = createdAt,
            generation = 1,
            recipientUid = "",
            state = "PENDING",
            consumedBy = null,
            consumedAt = null,
            dirty = true,
        ).also { dao.upsertInvite(it.toRecord()) }
    }

    companion object {
        private const val CODE_LENGTH = 8
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
