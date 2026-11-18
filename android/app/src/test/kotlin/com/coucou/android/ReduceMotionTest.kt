package com.coucou.android

import com.coucou.android.core.KeyValueStore
import com.coucou.android.core.MotionChoice
import com.coucou.android.core.MotionPolicy
import com.coucou.android.core.UserSettings
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReduceMotionTest {
    @Test fun followsTheSystemUnlessTheUserChoseOtherwise() {
        assertTrue(MotionPolicy.reduced(MotionChoice.SYSTEM, 0f))
        assertFalse(MotionPolicy.reduced(MotionChoice.SYSTEM, 1f))
        assertFalse(MotionPolicy.reduced(MotionChoice.SYSTEM, 0.5f))
        assertTrue(MotionPolicy.reduced(MotionChoice.REDUCE, 1f))
        assertFalse(MotionPolicy.reduced(MotionChoice.FULL, 0f))
    }

    @Test fun theDefaultFollowsTheSystemAndIsSaved() {
        assertEquals(MotionChoice.SYSTEM, UserSettings().motion)
        assertEquals(MotionChoice.SYSTEM, MotionChoice.parse(null))
        assertEquals(MotionChoice.SYSTEM, MotionChoice.parse("fast"))
        for (c in MotionChoice.values()) assertEquals(c, MotionChoice.parse(c.key))
        val m = HashMap<String, Any>()
        val store = object : KeyValueStore {
            override fun getBoolean(key: String, default: Boolean) = m[key] as? Boolean ?: default
            override fun getInt(key: String, default: Int) = m[key] as? Int ?: default
            override fun getFloat(key: String, default: Float) = m[key] as? Float ?: default
            override fun getString(key: String, default: String) = m[key] as? String ?: default
            override fun put(key: String, value: Any) { m[key] = value }
        }
        UserSettings.save(store, UserSettings(motion = MotionChoice.REDUCE))
        assertEquals(MotionChoice.REDUCE, UserSettings.load(store).motion)
    }

    @Test fun settingsOffersTheChoice() {
        val ui = File("src/main/kotlin/com/coucou/android/ui/SettingsScreens.kt").readText()
        assertTrue(ui.contains("MotionChoice.REDUCE") && ui.contains("R.string.settings_motion"))
        // The island and sheets share the same motion policy.
        assertTrue(File("src/main/kotlin/com/coucou/android/ui/IslandOverlay.kt").readText().contains("MotionPolicy.reduced"))
        assertTrue(File("src/main/kotlin/com/coucou/android/ui/DesignSystem.kt").readText().contains("MotionPolicy.reduced"))
    }
}
