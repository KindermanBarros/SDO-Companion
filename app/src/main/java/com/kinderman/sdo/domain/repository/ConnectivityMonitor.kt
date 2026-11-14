package com.kinderman.sdo.domain.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Observes device network connectivity for online-only editing and real-time sync.
 */
interface ConnectivityMonitor {
    val isOnline: StateFlow<Boolean>
}

object AlwaysOnlineConnectivityMonitor : ConnectivityMonitor {
    override val isOnline: StateFlow<Boolean> = MutableStateFlow(true)
}
