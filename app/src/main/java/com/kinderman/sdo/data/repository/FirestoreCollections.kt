package com.kinderman.sdo.data.repository

/** Firestore collection and subcollection paths shared by repository implementations. */
internal object FirestoreCollections {
    const val USERS = "users"
    const val CHARACTERS = "characters"
    const val CHARACTER_FIELDS = "fields"
    const val CHARACTER_ITEMS = "items"
    const val CHARACTER_OPERATIONS = "operations"
    const val CHARACTER_PRESENCE = "presence"
    const val CHARACTER_MEDIA = "media"
    const val AVATAR_DOCUMENT = "avatar"
    const val CAMPAIGNS = "campaigns"
    const val CAMPAIGN_MEMBERS_SUBCOLLECTION = "members"
    const val CAMPAIGN_INVITES_SUBCOLLECTION = "invites"
    const val CAMPAIGN_MEMBERS = "campaignMembers"
    const val CAMPAIGN_INVITES = "campaignInvites"
    const val CAMPAIGN_AUDIT = "campaignAudit"
    const val CAMPAIGN_LIBRARY = "campaignLibrary"
    const val CAMPAIGN_DELIVERIES = "campaignDeliveries"
    const val CAMPAIGN_JOURNAL = "campaignJournal"
}
