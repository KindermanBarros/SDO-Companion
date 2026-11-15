package com.kinderman.sdo.domain.model

data class SheetSyncStatus(
    val pendingCount: Int = 0,
    val rejectedCount: Int = 0,
    val isSyncing: Boolean = false,
    val lastError: String? = null,
)
