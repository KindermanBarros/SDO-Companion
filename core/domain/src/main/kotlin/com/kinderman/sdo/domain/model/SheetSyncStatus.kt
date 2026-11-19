package com.kinderman.sdo.domain.model

/**
 * Account-scoped synchronization summary across all Spark v2 sheet intents.
 * Keeps the first four positional parameters compatible with legacy callers until Wave 2 cleanup.
 */
data class SheetSyncStatus(
    val pendingCount: Int = 0,
    val rejectedCount: Int = 0,
    val isSyncing: Boolean = false,
    val lastError: String? = null,
    val draftCount: Int = 0,
    val sendingCount: Int = 0,
    val confirmedCount: Int = 0,
    val conflictCount: Int = 0,
    val accessDeniedCount: Int = 0,
    val pausedCount: Int = 0,
) {
    val hasActionRequired: Boolean
        get() = conflictCount > 0 || rejectedCount > 0 || accessDeniedCount > 0

    val totalQueuedCount: Int
        get() = draftCount + pendingCount + sendingCount + conflictCount + rejectedCount + accessDeniedCount + pausedCount
}
