package com.kinderman.sdo.domain.model

import java.util.UUID

data class CampaignJournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val campaignId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String = "",
    val body: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val revision: Long = 1L,
    val lastEditorId: String = "",
    val lastEditorName: String = "",
    val operationId: String = "",
    val deleted: Boolean = false,
    val deletedAt: Long? = null,
)

enum class JournalSyncState {
    LOADING,
    LIVE,
    CACHED_OFFLINE,
    UNAVAILABLE_OFFLINE,
    ACCESS_REVOKED,
    ARCHIVED,
    ERROR,
}

data class JournalSnapshot(
    val entries: List<CampaignJournalEntry> = emptyList(),
    val syncState: JournalSyncState = JournalSyncState.LOADING,
    val errorMessage: String? = null,
)

data class CampaignJournalDraft(
    val entryId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val operationId: String = UUID.randomUUID().toString(),
    val isNew: Boolean = true,
)

sealed interface JournalSaveResult {
    data class Confirmed(val entry: CampaignJournalEntry) : JournalSaveResult
    data class Conflict(val serverEntry: CampaignJournalEntry) : JournalSaveResult
    data class Offline(val message: String) : JournalSaveResult
    data class PermissionDenied(val message: String) : JournalSaveResult
    data class Uncertain(
        val entryId: String,
        val operationId: String,
        val message: String,
    ) : JournalSaveResult

    data class ValidationError(val message: String) : JournalSaveResult
}

object CampaignJournalPolicy {
    const val TITLE_MAX_LENGTH = 120
    const val BODY_MAX_LENGTH = 20_000
    const val REMOVED_AUTHOR_FALLBACK = "Ex-participante"

    fun validateDraft(title: String, body: String): String? {
        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()
        return when {
            trimmedTitle.isEmpty() || trimmedBody.isEmpty() ->
                "Informe o título e o texto do registro."
            trimmedTitle.length > TITLE_MAX_LENGTH ->
                "O título pode ter no máximo $TITLE_MAX_LENGTH caracteres."
            trimmedBody.length > BODY_MAX_LENGTH ->
                "O registro pode ter no máximo $BODY_MAX_LENGTH caracteres."
            else -> null
        }
    }

    fun isCampaignModerator(campaign: Campaign, session: UserSession): Boolean =
        session.isAdmin || campaign.ownerId == session.uid

    fun canCreate(
        campaign: Campaign?,
        session: UserSession?,
        isActiveMember: Boolean,
        isOnline: Boolean,
    ): Boolean {
        if (campaign == null || session == null || !isOnline) return false
        if (campaign.state != CampaignState.ACTIVE) return false
        return isCampaignModerator(campaign, session) || isActiveMember
    }

    fun canMutateEntry(
        entry: CampaignJournalEntry,
        campaign: Campaign?,
        session: UserSession?,
        isActiveMember: Boolean,
        isOnline: Boolean,
    ): Boolean {
        if (campaign == null || session == null || !isOnline || entry.deleted) return false
        if (campaign.state != CampaignState.ACTIVE) return false
        if (isCampaignModerator(campaign, session)) return true
        return isActiveMember && entry.authorId == session.uid
    }

    fun displayAuthor(rawName: String, isActiveMemberOrOwner: Boolean): String {
        val cleaned = rawName.trim()
        return when {
            cleaned.isNotEmpty() -> cleaned
            !isActiveMemberOrOwner -> REMOVED_AUTHOR_FALLBACK
            else -> REMOVED_AUTHOR_FALLBACK
        }
    }
}
