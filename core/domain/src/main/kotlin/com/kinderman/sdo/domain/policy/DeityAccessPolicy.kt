package com.kinderman.sdo.domain.policy

import com.kinderman.sdo.domain.model.Campaign
import com.kinderman.sdo.domain.model.UserSession
import com.kinderman.sdo.domain.model.normalizeCampaignId

/**
 * Authoritative access control for GM-only deity content (`Para o Mestre` and structured GM fields).
 *
 * Rules (`AGENTS.md` §4.3 & product contract):
 * - Verified Administrators (`session.isAdmin`) always have read access.
 * - Campaign GMs (`campaign.ownerId == session.uid` on a non-deleted campaign) have read access
 *   ONLY when evaluated in that relevant campaign context.
 * - `UserRole.MASTER`, `CampaignRole.HISTORIAN`, or opening a Historian UI surface NEVER
 *   independently grants access to `Para o Mestre`.
 */
object DeityAccessPolicy {
    fun canReadGmContent(
        session: UserSession?,
        campaignId: String?,
        campaignsById: Map<String, Campaign>,
    ): Boolean {
        if (session == null) return false
        if (session.isAdmin) return true
        val normalizedId = normalizeCampaignId(campaignId.orEmpty())
        if (normalizedId.isBlank()) return false
        val campaign = campaignsById[normalizedId] ?: return false
        return !campaign.isDeleted && campaign.ownerId == session.uid
    }

    /**
     * Resolves the campaign context to evaluate when opening a deity from the Library.
     * - If an explicit [selectedCampaignId] (other than `"ALL"` / blank) is chosen, returns it verbatim
     *   so switching to a campaign where the user is only a Player immediately revokes GM spoiler access.
     * - If `"ALL"` / blank is selected, resolves a GM campaign ONLY when the user owns at least one
     *   non-deleted campaign AND is not a non-owner player in any other campaign (unambiguous GM-only context).
     */
    fun resolveLibraryCampaignContext(
        session: UserSession?,
        selectedCampaignId: String?,
        campaigns: Collection<Campaign>,
        playerMemberCampaignIds: Set<String> = emptySet(),
    ): String? {
        if (session == null) return null
        val explicit = selectedCampaignId?.takeUnless { it.isBlank() || it == "ALL" }
        if (explicit != null) return normalizeCampaignId(explicit)
        val nonDeleted = campaigns.filterNot(Campaign::isDeleted)
        val owned = nonDeleted.filter { it.ownerId == session.uid }
        if (owned.isEmpty()) return null
        val ownedIds = owned.mapTo(hashSetOf(), Campaign::id)
        val hasNonOwnerPlayerCampaign = playerMemberCampaignIds
            .map(::normalizeCampaignId)
            .any { it.isNotBlank() && it !in ownedIds }
        return if (hasNonOwnerPlayerCampaign) null else owned.first().id
    }
}
