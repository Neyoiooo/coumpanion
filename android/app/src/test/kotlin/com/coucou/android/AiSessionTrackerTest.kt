package com.coucou.android

import com.coucou.android.core.AiNotificationEvent
import com.coucou.android.core.AiSessionTracker
import com.coucou.android.core.AiProvider
import com.coucou.android.core.AiSessionResult
import com.coucou.android.core.AiActivitySource
import com.coucou.android.core.ManualSessionPolicy
import com.coucou.android.core.SessionTime
import com.coucou.android.core.BotState
import com.coucou.android.core.IslandGeometry
import com.coucou.android.core.IslandPosition
import com.coucou.android.core.IslandSize
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

    private fun time(ms: Long, elapsed: Long = ms, boot: Int = 1) = SessionTime(ms, elapsed, boot)

    @Test fun manualSessionsRestoreIndependentlyAndSurviveNotificationUpdates() {
        val disk = Store()
        val tracker = AiSessionTracker(disk)
        tracker.start(WatchedAiApp.CHATGPT.packageName, time(100L), ManualSessionPolicy())
        tracker.start(WatchedAiApp.GEMINI.packageName, time(200L), ManualSessionPolicy())
        tracker.start(WatchedAiApp.CHATGPT.packageName, time(300L), ManualSessionPolicy())
        tracker.record(WatchedAiApp.CHATGPT, AiNotificationEvent.COMPLETION_REPORTED, 400L)
        val restored = AiSessionTracker(disk)
        assertEquals(listOf(200L, 100L), restored.snapshot.manual.map { it.startedAtMs })
        assertEquals(2L, restored.snapshot.manual.first().elapsed(time(120_200L)) / 60_000)
        assertEquals(0L, restored.snapshot.manual.first().elapsed(time(99L)) / 60_000)
        restored.end(WatchedAiApp.CHATGPT.packageName, AiSessionResult.CANCELLED, time(500L))
        assertEquals(listOf(WatchedAiApp.GEMINI.packageName), AiSessionTracker(disk).snapshot.manual.map { it.provider.id })
        restored.retainInstalled(emptySet(), time(550L))
        assertTrue(AiSessionTracker(disk).snapshot.manual.isEmpty())
    }

    @Test fun notificationHistoryIsBoundedAndCanBeClearedWithoutStoppingManualSessions() {
        val disk = Store()
        val tracker = AiSessionTracker(disk)
        tracker.start(WatchedAiApp.DEEPSEEK.packageName, time(1L), ManualSessionPolicy())
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
        assertEquals(WatchedAiApp.DEEPSEEK.packageName, AiSessionTracker(disk).snapshot.manual.single().provider.id)
    }

    @Test fun timerIgnoresWallClockChangesPausesAndExpiresWithoutClaimingCompletion() {
        val disk = Store()
        val id = WatchedAiApp.GEMINI.packageName
        val tracker = AiSessionTracker(disk)
        assertTrue(tracker.start(id, time(100_000, 5_000), ManualSessionPolicy(2, 1)))
        tracker.pause(id, time(10, 35_000))
        val restored = AiSessionTracker(disk).also { it.restore(time(1_000_000, 95_000)) }
        assertEquals(30_000L, restored.snapshot.manual.single().elapsed(time(1_000_000, 95_000)))
        restored.resume(id, time(1_000_000, 95_000))
        assertEquals(1, restored.tick(time(20, 125_000)).size)
        assertTrue(restored.tick(time(21, 125_001)).isEmpty())
        restored.tick(time(22, 185_000))
        val entry = AiSessionTracker(disk).snapshot.history.single()
        assertEquals(AiActivitySource.MANUAL, entry.source)
        assertEquals(AiSessionResult.EXPIRED, entry.result)
        assertEquals(120_000L, entry.durationMs)
        assertTrue(restored.snapshot.manual.isEmpty())
    }

    @Test fun customProviderAndLegacyTimersRestoreAcrossRebootWithoutDuplicatingOrStoringUrlsInHistory() {
        val disk = Store()
        val id = WatchedAiApp.CLAUDE.packageName
        disk.put("ai_manual_sessions", "bad row\n$id|1000\n$id|2000")
        val tracker = AiSessionTracker(disk).also { it.restore(time(100_000, 10_000)) }
        tracker.checkpoint(time(110_000, 20_000))
        val afterReboot = AiSessionTracker(disk).also { it.restore(time(200_000, 500, 2)) }
        assertEquals(109_000L, afterReboot.snapshot.manual.single().elapsed(time(200_000, 500, 2)))
        val custom = checkNotNull(AiProvider.custom("Research", false, "https://example.com/private?key=never-in-history", null, true))
        assertTrue(afterReboot.saveProvider(custom))
        assertTrue(afterReboot.start(custom.id, time(200_000, 500, 2), ManualSessionPolicy()))
        assertFalse(afterReboot.start(custom.id, time(200_001, 501, 2), ManualSessionPolicy()))
        afterReboot.end(custom.id, AiSessionResult.MANUAL_DONE, time(300_000, 99_000, 2))
        afterReboot.removeProvider(custom.id, time(300_001, 99_001, 2))
        assertEquals("Research", AiSessionTracker(disk).snapshot.history.single().providerLabel)
        assertFalse(disk.getString("ai_session_records", "").contains("never-in-history"))
        assertTrue(AiProvider.custom("Bad site", false, "http://example.com", null, null) == null)
        assertTrue(AiProvider.custom("Bad app", true, "https://example.com", null, null) == null)
        assertTrue(AiProvider.custom("URL with user info", false, "https://a:b@example.com", null, null) == null)
    }

    @Test fun corruptedRecordsAreSkippedAndHistoryIsBounded() {
        val disk = Store()
        val id = WatchedAiApp.CLAUDE.packageName
        val tracker = AiSessionTracker(disk)
        repeat(AiSessionTracker.HISTORY_LIMIT + 5) {
            tracker.start(id, time(it + 1L), ManualSessionPolicy())
            tracker.end(id, AiSessionResult.MANUAL_DONE, time(it + 2L))
        }
        val valid = disk.getString("ai_session_records", "")
        disk.put("ai_session_records", valid.replaceFirst("v2\n", "v2\nS|broken\nH|invalid\n"))
        val restored = AiSessionTracker(disk)
        assertTrue(restored.snapshot.manual.isEmpty())
        assertEquals(AiSessionTracker.HISTORY_LIMIT, restored.snapshot.history.size)
        restored.clearHistory(restored.snapshot.history.first().id)
        assertEquals(AiSessionTracker.HISTORY_LIMIT - 1, AiSessionTracker(disk).snapshot.history.size)
        restored.clearHistory()
        assertTrue(AiSessionTracker(disk).snapshot.history.isEmpty())
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
        val settings = UserSettings(islandPosition = IslandPosition.RIGHT, islandSize = IslandSize.LARGE, plainIsland = true)
        UserSettings.save(store, settings)
        assertEquals(settings, UserSettings.load(store))
        val box = IslandGeometry.place(400, 1f, PxRect(65, 0, 85, 35), 30, safeLeft = 40)
        val landscape = IslandGeometry.place(660, 1f, null, 25, safeLeft = 48, safeRight = 52,
            size = IslandSize.LARGE, screenH = 280, safeBottom = 24)
        assertEquals(227, landscape.maxHeight)
        assertTrue(landscape.workingW > box.workingW)
        assertTrue(landscape.leftFor(landscape.expandedW, IslandPosition.LEFT) >= 48)
        val width = 100
        assertEquals(box.leftEdge, box.leftFor(width, IslandPosition.CAMERA))
        assertEquals(box.leftEdge, box.leftFor(width, IslandPosition.LEFT))
        assertEquals(box.rightEdge - width, box.leftFor(width, IslandPosition.RIGHT))
        assertEquals((box.leftEdge + box.rightEdge - width) / 2, box.leftFor(width, IslandPosition.CENTER))
    }
}
