package com.coucou.android

import com.coucou.android.core.BotState
import com.coucou.android.core.IslandTimeline
import com.coucou.android.core.ManualAiSession
import com.coucou.android.core.WatchedAiApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualAiSessionTest {
    @Test fun manualMarkerIsPersistentButNotConfirmedWorking() {
        var now = 0.0
        val session = ManualAiSession(WatchedAiApp.CHATGPT, 123L)
        val marker = session.island()
        val timeline = IslandTimeline { now }
        timeline.setActive(marker)
        assertTrue(marker.manualTracking)
        assertEquals(BotState.IDLE, marker.state)
        assertEquals(WatchedAiApp.CHATGPT.packageName, marker.pillId)
        now += 60_000.0
        timeline.tick()
        assertEquals(marker, timeline.current)
        assertNull(timeline.nextDeadline())
        timeline.setActive(null)
        assertEquals(IslandTimeline.Phase.RETRACTING, timeline.phase)
    }
}
