package com.coucou.android

import com.coucou.android.core.MobileDiagnostics
import com.coucou.android.link.LinkState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileDiagnosticsTest {
    @Test fun standardReportContainsOnlyLocalStatusAndOmitsListenerAccess() {
        val status = MobileDiagnostics(false, "0.6.1", 10, false, 36, false, true, null,
            true, false, 2, LinkState.DISCONNECTED, true)
        val report = status.report()
        assertTrue(report.contains("Standard") && report.contains("Manual timers: 2"))
        for (field in listOf("Notification access", "token", "address", "prompt", "certificate", "device model")) {
            assertFalse(field, report.contains(field, ignoreCase = true))
        }
        assertTrue(status.copy(full = true, notificationAccess = false).report().contains("Notification access: false"))
    }
}
