package com.kinderman.sdo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
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
        dao.observeForUser(session.uid).map { values -> values.map(CampaignRecord::toDomain) }

    override fun observeMembers(campaignId: String): Flow<List<CampaignMember>> =
        dao.observeMembers(campaignId).map { values -> values.map(CampaignMemberRecord::toDomain) }

    override fun observeInvites(campaignId: String): Flow<List<CampaignInvite>> =
        dao.observeInvites(campaignId).map { values -> values.map(CampaignInviteRecord::toDomain) }

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
                role = CampaignRole.MASTER,
                state = CampaignMemberState.ACTIVE,
                joinedAt = now,
                updatedAt = now,
                dirty = true,
            ).toRecord(),
        )
        return campaign
    }

    override suspend fun update(session: UserSession, campaign: Campaign) {
        requireOwner(session, campaign)
        val existing = requireCampaign(campaign.id)
        check(!existing.isArchived) { "Campanhas arquivadas são somente leitura." }
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

    override suspend fun transferOwnership(session: UserSession, campaign: Campaign, newOwnerId: String) {
        requireOwner(session, campaign)
        val existing = requireActiveCampaign(campaign.id)
        require(newOwnerId.isNotBlank() && newOwnerId != session.uid) { "Selecione outro participante ativo." }
        val target = dao.member(existing.id, newOwnerId)?.toDomain()
        check(target?.isActive == true) { "A nova Mestre precisa ser participante ativa da campanha." }
        val now = System.currentTimeMillis()
        dao.upsertCampaign(existing.copy(ownerId = newOwnerId, updatedAt = now, dirty = true).toRecord())
        dao.upsertMember(target.copy(role = CampaignRole.MASTER, updatedAt = now, dirty = true).toRecord())
        val current = dao.member(existing.id, session.uid)?.toDomain()
        if (current != null) {
            dao.upsertMember(current.copy(role = CampaignRole.PLAYER, updatedAt = now, dirty = true).toRecord())
        }
    }

    override suspend fun leave(session: UserSession, campaign: Campaign) {
        val existing = requireCampaign(campaign.id)
        check(existing.ownerId != session.uid) { "Transfira a responsabilidade da campanha antes de sair." }
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
        val code = generateCode()
        val invite = CampaignInvite(
            id = code,
            campaignId = existing.id,
            campaignName = existing.name,
            campaignDescription = existing.description,
            code = code,
            createdBy = session.uid,
            createdAt = System.currentTimeMillis(),
            generation = 1,
            dirty = true,
        )
        dao.upsertInvite(invite.toRecord())
        return invite
    }

    override suspend fun revokeInvite(session: UserSession, invite: CampaignInvite) {
        val campaign = requireCampaign(invite.campaignId)
        requireOwner(session, campaign)
        dao.upsertInvite(invite.copy(revokedAt = System.currentTimeMillis(), dirty = true).toRecord())
    }

    override suspend fun regenerateInvite(session: UserSession, invite: CampaignInvite): CampaignInvite {
        val campaign = requireCampaign(invite.campaignId)
        requireOwner(session, campaign)
        val existing = requireActiveCampaign(campaign.id)
        revokeInvite(session, invite)
        val code = generateCode()
        return CampaignInvite(
            id = code,
            campaignId = existing.id,
            campaignName = existing.name,
            campaignDescription = existing.description,
            code = code,
            createdBy = session.uid,
            createdAt = System.currentTimeMillis(),
            generation = invite.generation + 1,
            dirty = true,
        ).also { dao.upsertInvite(it.toRecord()) }
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
        check(existing.ownerId == session.uid || member?.isActive == true) { "Você não participa desta campanha." }
        val currentCampaignId = normalizeCampaignId(character.campaignId)
        check(currentCampaignId.isBlank() || currentCampaignId == existing.id) {
            "A ficha já está vinculada a outra campanha ativa."
        }
        check(character.ownerId == session.uid || existing.ownerId == session.uid) {
            "Você não pode vincular esta ficha à campanha."
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
        check(character.ownerId == session.uid || campaign.ownerId == session.uid) {
            "Você não pode remover esta ficha da campanha."
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

        val dirtyCampaigns = dao.dirtyCampaigns().filter { it.ownerId == session.uid }
        val newCampaigns = dirtyCampaigns.filter { it.lastSyncedAt == 0L }
        val changedCampaigns = dirtyCampaigns.filter { it.lastSyncedAt != 0L }

        newCampaigns.forEach { local ->
            campaigns.document(local.id).set(local.copy(dirty = false), SetOptions.merge()).await()
            dao.markCampaignSynced(local.id, local.updatedAt)
        }
        dao.dirtyMembers().forEach { local ->
            if (local.userId == session.uid || dao.campaign(local.campaignId)?.ownerId == session.uid) {
                members.document(memberId(local.campaignId, local.userId))
                    .set(local.copy(dirty = false), SetOptions.merge()).await()
                dao.markMemberSynced(local.campaignId, local.userId, local.updatedAt)
            }
        }
        dao.dirtyInvites().forEach { local ->
            if (dao.campaign(local.campaignId)?.ownerId == session.uid) {
                invites.document(local.id).set(local.copy(dirty = false), SetOptions.merge()).await()
                dao.markInviteSynced(local.id, local.createdAt)
            }
        }
        changedCampaigns.forEach { local ->
            campaigns.document(local.id).set(local.copy(dirty = false), SetOptions.merge()).await()
            dao.markCampaignSynced(local.id, local.updatedAt)
        }

        val membershipSnapshot = members.whereEqualTo("userId", session.uid).get().await()
        val remoteMemberships = membershipSnapshot.documents.mapNotNull { document ->
            document.toObject(CampaignMemberRecord::class.java)
        }
        remoteMemberships.forEach { dao.upsertMember(it.copy(dirty = false, lastSyncedAt = it.updatedAt)) }

        val campaignIds = remoteMemberships
            .filter { it.state == CampaignMemberState.ACTIVE.name }
            .mapTo(mutableSetOf()) { it.campaignId }
        campaigns.whereEqualTo("ownerId", session.uid).get().await().documents.mapNotNull { document ->
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

        val ownedIds = dao.allCampaigns().filter { it.ownerId == session.uid }.mapTo(mutableSetOf()) { it.id }
        ownedIds.forEach { campaignId ->
            members.whereEqualTo("campaignId", campaignId).get().await().documents.mapNotNull { document ->
                document.toObject(CampaignMemberRecord::class.java)
            }.forEach { dao.upsertMember(it.copy(dirty = false, lastSyncedAt = it.updatedAt)) }
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
        check(!it.isArchived) { "Campanhas arquivadas são somente leitura." }
    }

    private fun requireOwner(session: UserSession, campaign: Campaign) {
        check(campaign.ownerId == session.uid) { "Somente a Mestre responsável pode executar esta operação." }
    }

    private fun generateCode(): String = buildString(CODE_LENGTH) {
        repeat(CODE_LENGTH) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
    }

    private fun normalizeCode(value: String): String = value.trim().uppercase().filter { it in CODE_ALPHABET }
    private fun memberId(campaignId: String, userId: String): String = "$campaignId::$userId"

    companion object {
        private const val CAMPAIGNS = "campaigns"
        private const val MEMBERS = "campaignMembers"
        private const val INVITES = "campaignInvites"
        private const val CODE_LENGTH = 8
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
