package com.coucou.android

import com.coucou.android.core.AiNotificationEvent
import com.coucou.android.core.AiSessionTracker
import com.coucou.android.core.BotState
import com.coucou.android.core.IslandGeometry
import com.coucou.android.core.IslandPosition
import com.coucou.android.core.IslandSpec
import com.coucou.android.core.IslandTimeline
import com.coucou.android.core.KeyValueStore
import com.coucou.android.core.ManualAiSession
import com.coucou.android.core.PxRect
import com.coucou.android.core.UserSettings
import com.coucou.android.core.WatchedActivity
import com.coucou.android.core.WatchedAiApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSessionTrackerTest {
    private class Store : KeyValueStore {
        val values = mutableMapOf<String, Any>()
        override fun getBoolean(key: String, default: Boolean) = values[key] as? Boolean ?: default
        override fun getInt(key: String, default: Int) = values[key] as? Int ?: default
        override fun getFloat(key: String, default: Float) = values[key] as? Float ?: default
        override fun getString(key: String, default: String) = values[key] as? String ?: default
        override fun put(key: String, value: Any) { values[key] = value }
    }

    @Test fun manualSessionsRestoreIndependentlyAndSurviveNotificationUpdates() {
        val disk = Store()
        val tracker = AiSessionTracker(disk)
        tracker.setManual(WatchedAiApp.CHATGPT, true, 100L)
        tracker.setManual(WatchedAiApp.GEMINI, true, 200L)
        tracker.setManual(WatchedAiApp.CHATGPT, true, 300L)
        tracker.record(WatchedAiApp.CHATGPT, AiNotificationEvent.COMPLETION_REPORTED, 400L)
        val restored = AiSessionTracker(disk)
        assertEquals(listOf(200L, 100L), restored.snapshot.manual.map { it.startedAtMs })
        assertEquals(2L, restored.snapshot.manual.first().elapsedMinutes(120_200L))
        assertEquals(0L, restored.snapshot.manual.first().elapsedMinutes(99L))
        restored.setManual(WatchedAiApp.CHATGPT, false, 500L)
        assertEquals(listOf(WatchedAiApp.GEMINI), AiSessionTracker(disk).snapshot.manual.map { it.app })
        restored.retainInstalled(emptySet())
        assertTrue(AiSessionTracker(disk).snapshot.manual.isEmpty())
    }

    @Test fun notificationHistoryIsBoundedAndCanBeClearedWithoutStoppingManualSessions() {
        val disk = Store()
        val tracker = AiSessionTracker(disk)
        tracker.setManual(WatchedAiApp.DEEPSEEK, true, 1L)
        repeat(WatchedActivity.LIMIT + 10) {
            tracker.record(WatchedAiApp.CHATGPT, AiNotificationEvent.COMPLETION_REPORTED, it + 1L)
        }
        tracker.record(WatchedAiApp.DEEPSEEK, AiNotificationEvent.UPDATE, 100L)
        val restored = AiSessionTracker(disk)
        assertEquals(WatchedActivity.LIMIT, restored.snapshot.activity.size)
        assertEquals(AiNotificationEvent.UPDATE, restored.snapshot.activity.first().event)
        assertEquals(AiNotificationEvent.COMPLETION_REPORTED, restored.snapshot.activity[1].event)
        restored.clearActivity(WatchedAiApp.CHATGPT)
        assertEquals(WatchedAiApp.DEEPSEEK, AiSessionTracker(disk).snapshot.activity.single().app)
        restored.clearActivity()
        assertTrue(AiSessionTracker(disk).snapshot.activity.isEmpty())
        assertEquals(WatchedAiApp.DEEPSEEK, AiSessionTracker(disk).snapshot.manual.single().app)
    }

    @Test fun classificationUsesTheProviderAndFullTitleWithoutClaimingProgress() {
        val title = "Your task is complete."
        val event = AiNotificationEvent.classify(WatchedAiApp.CHATGPT, title)
        assertEquals(AiNotificationEvent.COMPLETION_REPORTED, event)
        assertEquals(AiNotificationEvent.UPDATE, AiNotificationEvent.classify(WatchedAiApp.GROK, title))
        assertEquals(AiNotificationEvent.UPDATE, AiNotificationEvent.classify(WatchedAiApp.CHATGPT, "$title Open to read a message"))
        assertEquals(AiNotificationEvent.ATTENTION_REPORTED, AiNotificationEvent.classify(WatchedAiApp.DEEPSEEK, "Response failed"))
        val island = WatchedAiApp.CHATGPT.update(title, event)
        assertEquals(IslandSpec.Kind.APP_UPDATE, island.kind)
        assertEquals(BotState.IDLE, island.state)
        assertFalse(island.persistent)
    }

    @Test fun anotherManualSessionCanRestoreADismissedIslandAndDesktopRequestsStillTakePriority() {
        val timeline = IslandTimeline { 0.0 }
        val first = ManualAiSession(WatchedAiApp.CHATGPT, 100L)
        val second = ManualAiSession(WatchedAiApp.DEEPSEEK, 200L)
        timeline.setActive(first.island())
        timeline.swipeAway()
        timeline.setActive(first.island())
        assertEquals(IslandTimeline.Phase.RETRACTING, timeline.phase)
        val group = ManualAiSession.island(listOf(first, second))
        timeline.setActive(group)
        assertEquals(group, timeline.current)
        val request = IslandSpec(IslandSpec.Kind.APPROVAL, "agent_codex", "Codex", BotState.APPROVAL, "Edit a file", "test")
        timeline.setActive(request)
        assertEquals(request, timeline.current)
    }

    @Test fun appearanceSettingsRoundTripAndKeepEveryPositionInsideTheSafeArea() {
        val store = Store()
        assertEquals(IslandPosition.CAMERA, UserSettings.load(store).islandPosition)
        val settings = UserSettings(islandPosition = IslandPosition.RIGHT, plainIsland = true)
        UserSettings.save(store, settings)
        assertEquals(settings, UserSettings.load(store))
        val box = IslandGeometry.place(400, 1f, PxRect(65, 0, 85, 35), 30, safeLeft = 40)
        val width = 100
        assertEquals(box.leftEdge, box.leftFor(width, IslandPosition.CAMERA))
        assertEquals(box.leftEdge, box.leftFor(width, IslandPosition.LEFT))
        assertEquals(box.rightEdge - width, box.leftFor(width, IslandPosition.RIGHT))
        assertEquals((box.leftEdge + box.rightEdge - width) / 2, box.leftFor(width, IslandPosition.CENTER))
    }
}
