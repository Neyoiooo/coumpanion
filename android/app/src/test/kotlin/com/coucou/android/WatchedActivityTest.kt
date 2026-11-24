package com.coucou.android

import com.coucou.android.core.WatchedActivity
import com.coucou.android.core.WatchedAiApp
import com.coucou.android.core.IslandTimeline
import com.coucou.android.core.IslandSpec
import com.coucou.android.core.BotState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchedActivityTest {
    @Test fun metadataHistoryIsNewestFirstAndBounded() {
        var events = emptyList<WatchedActivity>()
        repeat(60) { events = WatchedActivity.record(events, WatchedAiApp.CLAUDE, it.toLong()) }
        assertEquals(50, events.size)
        assertEquals(59L, events.first().atMs)
        assertEquals(10L, events.last().atMs)
    }

    @Test fun mobileUpdatesQueueAndReturnToWorkingAgent() {
        var now = 0.0
        val timeline = IslandTimeline { now }
        val working = IslandSpec(IslandSpec.Kind.WORKING, "codex", "Codex", BotState.WORKING, "")
        val first = WatchedAiApp.CLAUDE.update("first")
        val second = WatchedAiApp.CHATGPT.update("second")
        timeline.setActive(working)
        timeline.flash(first)
        timeline.flash(second)
        assertEquals(first, timeline.current)
        now += IslandTimeline.HOLD_MS
        timeline.tick()
        assertEquals(second, timeline.current)
        now += IslandTimeline.HOLD_MS
        timeline.tick()
        assertEquals(working, timeline.current)
        assertNull(timeline.nextDeadline())
    }

    @Test fun clearingMobileUpdatesRemovesQueuedTitlesWithoutRemovingApproval() {
        val timeline = IslandTimeline { 0.0 }
        val approval = IslandSpec(IslandSpec.Kind.APPROVAL, "codex", "Codex", BotState.APPROVAL, "review", "id")
        timeline.flash(WatchedAiApp.CLAUDE.update("first"))
        timeline.flash(WatchedAiApp.CHATGPT.update("second"))
        timeline.setActive(approval)
        timeline.clearFlash(IslandSpec.Kind.APP_UPDATE)
        assertEquals(approval, timeline.current)
        timeline.setActive(null)
        assertEquals(IslandTimeline.Phase.RETRACTING, timeline.phase)
    }
}
