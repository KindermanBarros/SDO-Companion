package com.kinderman.sdo.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSyncPolicyTest {
    @Test fun automaticSyncAllowsLifecycleAndIdleTriggers() {
        RemoteSyncTrigger.entries.filterNot { it == RemoteSyncTrigger.MANUAL }.forEach { trigger ->
            assertTrue(trigger.name, shouldRunRemoteSync(automaticSync = true, trigger))
        }
    }

    @Test fun disabledAutomaticSyncAllowsOnlyManualTrigger() {
        RemoteSyncTrigger.entries.filterNot { it == RemoteSyncTrigger.MANUAL }.forEach { trigger ->
            assertFalse(trigger.name, shouldRunRemoteSync(automaticSync = false, trigger))
        }
        assertTrue(shouldRunRemoteSync(automaticSync = false, RemoteSyncTrigger.MANUAL))
    }
}
