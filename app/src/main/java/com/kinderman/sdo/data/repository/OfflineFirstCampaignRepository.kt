package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore
import com.kinderman.sdo.data.local.CampaignDao
import com.kinderman.sdo.data.local.CampaignInviteRecord
import com.kinderman.sdo.data.local.CampaignMemberRecord
import com.kinderman.sdo.data.local.CampaignRecord
import com.kinderman.sdo.data.local.CharacterDao
import com.kinderman.sdo.data.local.toDomain
import com.kinderman.sdo.data.local.toRecord
import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.CampaignInvite
import com.kinderman.sdo.domain.model.CampaignInvitePreview
import com.kinderman.sdo.domain.model.CampaignMember
import com.kinderman.sdo.domain.model.CampaignMemberState
import com.kinderman.sdo.domain.model.CampaignRole
import com.kinderman.sdo.domain.model.CampaignState
import com.kinderman.sdo.domain.model.Character
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.normalizeCampaignId
import com.kinderman.sdo.domain.repository.CampaignRepository
import java.security.SecureRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class OfflineFirstCampaignRepository(
    private val dao: CampaignDao,
    private val characterDao: CharacterDao,
) : CampaignRepository {
    private val syncMutex = Mutex()
    private val random = SecureRandom()

    override fun observe(session: UserSession): Flow<List<Campaign>> =
        (if (session.isAdmin) dao.observeAll() else dao.observeForUser(session.uid))
            .map { values -> values.map(CampaignRecord::toDomain).filterNot(Campaign::isDeleted) }

    override fun observeMembers(campaignId: String): Flow<List<CampaignMember>> =
        dao.observeMembers(campaignId).map { values -> values.map(CampaignMemberRecord::toDomain) }

    override fun observeMemberships(session: UserSession): Flow<List<CampaignMember>> =
        dao.observeMemberships(session.uid).map { values -> values.map(CampaignMemberRecord::toDomain) }

    override fun observeInvites(campaignId: String): Flow<List<CampaignInvite>> =
        dao.observeInvites(campaignId).map { values -> values.map(CampaignInviteRecord::toDomain) }

    override fun observeAllInvites(): Flow<List<CampaignInvite>> =
        dao.observeAllInvites().map { values -> values.map(CampaignInviteRecord::toDomain) }

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

    // A terminal tombstone prevents stale offline clients from recreating the campaign.
    // Character documents are detached, never deleted.
    override suspend fun delete(session: UserSession, campaign: Campaign) = syncMutex.withLock {
        requireOwner(session, requireCampaign(campaign.id))
        val store = Firebase.firestore
        val reference = store.collection(CAMPAIGNS).document(campaign.id)
        val remote = reference.get(com.google.firebase.firestore.Source.SERVER).await()
        check(remote.exists()) { "Sincronize a campanha antes de excluí-la." }
        check(session.isAdmin || remote.getString("ownerId") == session.uid) { "Sem permissão para excluir." }
        val characters = store.collection("characters").whereEqualTo("campaignId", campaign.id)
            .get(com.google.firebase.firestore.Source.SERVER).await().documents
        check(characters.size <= 450) { "Desvincule algumas fichas antes de excluir: o limite seguro por operação é de 450 fichas." }
        val now = System.currentTimeMillis()
        val batch = store.batch()
        characters.forEach { batch.update(it.reference, mapOf("campaignId" to "", "updatedAt" to now)) }
        batch.update(reference, mapOf("state" to CampaignState.DELETED.name, "updatedAt" to now))
        batch.commit().await()
        characterDao.all().filter { it.campaignId == campaign.id }.forEach {
            characterDao.upsert(it.copy(campaignId = "", updatedAt = now, dirty = true))
        }
        dao.upsertCampaign(requireCampaign(campaign.id).copy(
            state = CampaignState.DELETED, updatedAt = now, dirty = false, lastSyncedAt = now,
        ).toRecord())
    }

    override suspend fun leave(session: UserSession, campaign: Campaign) {
        val existing = requireActiveCampaign(campaign.id)
        check(existing.ownerId != session.uid) { "A Mestre que criou a campanha não pode sair dela." }
        val member = dao.member(existing.id, session.uid)?.toDomain()
            ?: error("Você não participa desta campanha.")
        val now = System.currentTimeMillis()
        detachMemberCharacters(member, existing.id, now)
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
        detachMemberCharacters(member, existing.id, now)
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
        return dao.canonicalInvite(existing.id)?.toDomain()
            ?: createStaticInvite(session, existing, System.currentTimeMillis())
    }

    override suspend fun previewInvite(session: UserSession, code: String): CampaignInvitePreview? {
        val normalized = normalizeCode(code)
        if (normalized.isBlank()) return null
        val local = dao.inviteByCode(normalized)?.toDomain()
        val invite = local ?: run {
            val store = runCatching { Firebase.firestore }.getOrNull() ?: return null
            val document = store.collection(INVITES).document(normalized).get().await()
            document.toObject(CampaignInviteRecord::class.java)?.copy(id = document.id)?.toDomain()
        } ?: return null
        if (!invite.isUsable()) return null

        val campaign = dao.campaign(invite.campaignId)?.toDomain() ?: Campaign(
            id = invite.campaignId,
            name = invite.campaignName.ifBlank { "Campanha" },
            description = invite.campaignDescription,
            ownerId = invite.createdBy,
            state = CampaignState.ACTIVE,
            dirty = false,
        )
        if (campaign.isArchived) return null
        return CampaignInvitePreview(
            campaign = campaign,
            invite = invite,
            alreadyMember = dao.member(campaign.id, session.uid)?.toDomain()?.isActive == true,
        )
    }

    override suspend fun joinByCode(session: UserSession, code: String): Campaign {
        val preview = previewInvite(session, code) ?: error("Convite inválido, revogado ou expirado.")
        if (preview.alreadyMember) return preview.campaign
        val now = System.currentTimeMillis()
        dao.upsertCampaign(preview.campaign.copy(dirty = false).toRecord())
        dao.upsertInvite(preview.invite.copy(dirty = false).toRecord())
        dao.upsertMember(
            CampaignMember(
                campaignId = preview.campaign.id,
                userId = session.uid,
                role = CampaignRole.PLAYER,
                state = CampaignMemberState.ACTIVE,
                joinedAt = now,
                updatedAt = now,
                joinedByInviteId = preview.invite.id,
                dirty = true,
            ).toRecord(),
        )
        return preview.campaign
    }

    override suspend fun linkCharacter(session: UserSession, character: Character, campaign: Campaign): Character {
        val existing = requireActiveCampaign(campaign.id)
        val member = dao.member(existing.id, session.uid)?.toDomain()
        check(session.isAdmin || existing.ownerId == session.uid || member?.isActive == true) { "Você não participa desta campanha." }
        val currentCampaignId = normalizeCampaignId(character.campaignId)
        check(session.isAdmin || currentCampaignId.isBlank() || currentCampaignId == existing.id) {
            "A ficha já está vinculada a outra campanha ativa."
        }
        check(session.isAdmin || character.ownerId == session.uid) { "Somente o dono pode vincular esta ficha." }
        if (session.isAdmin && currentCampaignId.isNotBlank() && currentCampaignId != existing.id) {
            dao.member(currentCampaignId, character.ownerId)?.toDomain()?.let { previousMembership ->
                if (character.id in previousMembership.characterIds) dao.upsertMember(
                    previousMembership.copy(
                        characterIds = previousMembership.characterIds - character.id,
                        updatedAt = System.currentTimeMillis(),
                        dirty = true,
                    ).toRecord(),
                )
            }
        }
        val ownerMembership = dao.member(existing.id, character.ownerId)?.toDomain()
        if (ownerMembership != null && character.id !in ownerMembership.characterIds) {
            dao.upsertMember(
                ownerMembership.copy(
                    characterIds = ownerMembership.characterIds + character.id,
                    updatedAt = System.currentTimeMillis(),
                    dirty = true,
                ).toRecord(),
            )
        }
        return character.copy(campaignId = existing.id)
    }

    override suspend fun unlinkCharacter(session: UserSession, character: Character): Character {
        val campaignId = normalizeCampaignId(character.campaignId)
        if (campaignId.isBlank()) return character.copy(campaignId = "")
        val campaign = requireCampaign(campaignId)
        check(session.isAdmin || character.ownerId == session.uid || campaign.ownerId == session.uid) {
            "Somente o dono da ficha, o dono da campanha ou um administrador pode desvincular esta ficha."
        }
        val member = dao.member(campaignId, character.ownerId)?.toDomain()
        if (member != null && character.id in member.characterIds) {
            dao.upsertMember(
                member.copy(
                    characterIds = member.characterIds - character.id,
                    updatedAt = System.currentTimeMillis(),
                    dirty = true,
                ).toRecord(),
            )
        }
        return character.copy(campaignId = "")
    }

    override suspend fun sync(session: UserSession) = syncMutex.withLock {
        val store = runCatching { Firebase.firestore }.getOrNull() ?: return@withLock
        val campaigns = store.collection(CAMPAIGNS)
        val members = store.collection(MEMBERS)
        val invites = store.collection(INVITES)

        dao.allCampaigns().filter { it.lastSyncedAt != 0L && it.state != CampaignState.DELETED.name && (session.isAdmin || it.ownerId == session.uid || dao.member(it.id, session.uid)?.state == CampaignMemberState.ACTIVE.name) }.forEach { local ->
            val remote = campaigns.document(local.id).get(com.google.firebase.firestore.Source.SERVER).await()
                .toObject(CampaignRecord::class.java)
            if (remote?.state == CampaignState.DELETED.name) {
                dao.upsertCampaign(remote.copy(dirty = false, lastSyncedAt = remote.updatedAt))
                characterDao.inCampaign(local.id).forEach {
                    characterDao.upsert(it.copy(campaignId = "", dirty = true))
                }
            }
        }
        val deletedIds = dao.allCampaigns().filter { it.state == CampaignState.DELETED.name }.map { it.id }.toSet()
        val dirtyCampaigns = dao.dirtyCampaigns().filterNot { it.id in deletedIds }.filter { session.isAdmin || it.ownerId == session.uid }
        val newCampaigns = dirtyCampaigns.filter { it.lastSyncedAt == 0L }
        val changedCampaigns = dirtyCampaigns.filter { it.lastSyncedAt != 0L }
        val dirtyMembers = dao.dirtyMembers().filterNot { it.campaignId in deletedIds }

        newCampaigns.forEach { local ->
            campaigns.document(local.id).set(local.copy(dirty = false), SetOptions.merge()).await()
            dao.markCampaignSynced(local.id, local.updatedAt)
        }
        dirtyMembers.forEach { local ->
            if (session.isAdmin || local.userId == session.uid || dao.campaign(local.campaignId)?.ownerId == session.uid) {
                members.document(memberId(local.campaignId, local.userId))
                    .set(local.copy(role = CampaignRole.PLAYER.name, dirty = false), SetOptions.merge()).await()
                dao.markMemberSynced(local.campaignId, local.userId, local.updatedAt)
            }
        }
        dao.dirtyInvites().filterNot { it.campaignId in deletedIds }.forEach { local ->
            if (session.isAdmin || dao.campaign(local.campaignId)?.ownerId == session.uid) {
                val reference = invites.document(local.id)
                try {
                    reference.set(local.copy(dirty = false)).await()
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

        val membershipSnapshot = if (session.isAdmin) members.get().await()
            else members.whereEqualTo("userId", session.uid).get().await()
        val remoteMemberships = membershipSnapshot.documents.mapNotNull { document ->
            document.toObject(CampaignMemberRecord::class.java)
        }
        remoteMemberships.forEach { dao.upsertMember(it.copy(dirty = false, lastSyncedAt = it.updatedAt)) }

        val campaignIds = remoteMemberships
            .filter { it.state == CampaignMemberState.ACTIVE.name }
            .mapTo(mutableSetOf()) { it.campaignId }
        val visibleCampaignDocuments = if (session.isAdmin) campaigns.get().await().documents
            else campaigns.whereEqualTo("ownerId", session.uid).get().await().documents
        visibleCampaignDocuments.mapNotNull { document ->
            document.toObject(CampaignRecord::class.java)?.copy(id = document.id)
        }.forEach { campaign ->
            campaignIds += campaign.id
            dao.upsertCampaign(campaign.copy(dirty = false, lastSyncedAt = campaign.updatedAt))
        }
        campaignIds.forEach { campaignId ->
            val document = campaigns.document(campaignId).get().await()
            document.toObject(CampaignRecord::class.java)?.copy(id = document.id)?.let { remote ->
                dao.upsertCampaign(remote.copy(dirty = false, lastSyncedAt = remote.updatedAt))
            }
        }

        val manageableIds = dao.allCampaigns()
            .filter { it.state != CampaignState.DELETED.name && (session.isAdmin || it.ownerId == session.uid) }
            .mapTo(mutableSetOf()) { it.id }
        manageableIds.forEach { campaignId ->
            members.whereEqualTo("campaignId", campaignId).get().await().documents.mapNotNull { document ->
                document.toObject(CampaignMemberRecord::class.java)
            }.forEach { remote ->
                val migrated = if (remote.role != CampaignRole.PLAYER.name) remote.copy(role = CampaignRole.PLAYER.name) else remote
                if (remote.role != CampaignRole.PLAYER.name) {
                    members.document(memberId(remote.campaignId, remote.userId))
                        .set(migrated.copy(dirty = false), SetOptions.merge()).await()
                }
                dao.upsertMember(migrated.copy(dirty = false, lastSyncedAt = migrated.updatedAt))
            }
        }
        (campaignIds - deletedIds).forEach { campaignId ->
            invites.whereEqualTo("campaignId", campaignId).get().await().documents.mapNotNull { document ->
                document.toObject(CampaignInviteRecord::class.java)?.copy(id = document.id)
            }.forEach { dao.upsertInvite(it.copy(dirty = false, lastSyncedAt = it.createdAt)) }
        }
    }

    private suspend fun detachMemberCharacters(member: CampaignMember, campaignId: String, now: Long) {
        member.characterIds.forEach { characterId ->
            val record = characterDao.get(characterId) ?: return@forEach
            if (normalizeCampaignId(record.campaignId) == campaignId) {
                characterDao.upsert(
                    record.copy(
                        campaignId = "",
                        updatedAt = now,
                        dirty = true,
                    ),
                )
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

    private fun generateCode(): String = buildString(CODE_LENGTH) {
        repeat(CODE_LENGTH) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
    }

    private fun normalizeCode(value: String): String = value.trim().uppercase().filter { it in CODE_ALPHABET }
    private fun memberId(campaignId: String, userId: String): String = "$campaignId::$userId"

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
            dirty = true,
        ).also { dao.upsertInvite(it.toRecord()) }
    }

    companion object {
        private const val CAMPAIGNS = "campaigns"
        private const val MEMBERS = "campaignMembers"
        private const val INVITES = "campaignInvites"
        private const val CODE_LENGTH = 8
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
