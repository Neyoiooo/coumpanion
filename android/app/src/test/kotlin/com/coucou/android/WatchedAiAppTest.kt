package com.coucou.android

import com.coucou.android.core.BotState
import com.coucou.android.core.IslandPlan
import com.coucou.android.core.IslandSpec
import com.coucou.android.core.IslandTimeline
import com.coucou.android.core.WatchedAiApp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchedAiAppTest {
    @Test fun onlySelectedPackagesInTheCatalogCanShowAnUpdate() {
        val claude = WatchedAiApp.CLAUDE
        val unknown = claude.packageName + ".other"
        val selected = setOf(claude.packageName, unknown)
        assertEquals(claude, WatchedAiApp.selected(claude.packageName, selected))
        assertNull(WatchedAiApp.selected(WatchedAiApp.CHATGPT.packageName, selected))
        assertNull(WatchedAiApp.selected(unknown, selected))

        val manifest = File("src/main/AndroidManifest.xml").readText()
        val queries = manifest.substringAfter("<queries>").substringBefore("</queries>")
        assertEquals(WatchedAiApp.entries.map { it.packageName }.toSet(),
            Regex("""<package android:name="([^"]+)"""").findAll(queries).map { it.groupValues[1] }.toSet())
        val listenerPermission = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        assertFalse(manifest.contains(listenerPermission))
        assertTrue(File("src/full/AndroidManifest.xml").readText().contains(listenerPermission))
        assertFalse(manifest.contains("android.permission.QUERY_ALL_PACKAGES"))
    }

    @Test fun aNotificationTitleIsShortAndStrippedOfControlsAndLinks() {
        assertEquals("Task complete …", WatchedAiApp.shortTitle("Task\ncomplete\u202E https://example.invalid/?token=1"))
        assertEquals("", WatchedAiApp.shortTitle(null))
        assertTrue(WatchedAiApp.shortTitle("long ".repeat(200)).length <= 160)
    }

    @Test fun aWatchedUpdateTimesOutAndDoesNotReplaceARequest() {
        var now = 0.0
        val timeline = IslandTimeline { now }
        val update = WatchedAiApp.CLAUDE.update("Task complete")
        assertFalse(update.persistent)
        assertEquals(IslandPlan.Shade.ORIGINAL_NOTE, IslandPlan.shadeFor(update.kind))
        timeline.flash(update)
        assertEquals(update, timeline.current)
        assertEquals(IslandTimeline.HOLD_MS, timeline.nextDeadline()!!, 0.0)
        val request = IslandSpec(IslandSpec.Kind.APPROVAL, "codex", "Codex", BotState.APPROVAL, "Edit a file", "request")
        timeline.setActive(request)
        timeline.flash(update)
        assertEquals(request, timeline.current)
        timeline.clearFlash(IslandSpec.Kind.APP_UPDATE)
        assertEquals(request, timeline.current)
        timeline.flash(update)
        now += IslandTimeline.HOLD_MS
        timeline.setActive(null)
        assertEquals(IslandTimeline.Phase.RETRACTING, timeline.phase)
    }
}
