package com.kinderman.sdo.data.repository

/** Firestore collection paths shared by repository implementations. */
internal object FirestoreCollections {
    const val USERS = "users"
    const val CHARACTERS = "characters"
    const val CAMPAIGNS = "campaigns"
    const val CAMPAIGN_MEMBERS = "campaignMembers"
    const val CAMPAIGN_INVITES = "campaignInvites"
    const val CAMPAIGN_AUDIT = "campaignAudit"
    const val CAMPAIGN_LIBRARY = "campaignLibrary"
    const val CAMPAIGN_DELIVERIES = "campaignDeliveries"
}
