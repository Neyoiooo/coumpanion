package com.coucou.android

import com.coucou.android.sound.SoundPlayer
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetsTest {
    private val raw = File("src/main/res/raw")
    private val res = File("src/main/res")

    @Test fun noBundledSoundCopiesAnUpstreamRecording() {
        val upstream = ReferenceFiles.file("NotchBuddy/Resources/sounds").listFiles { f -> f.extension == "wav" }.orEmpty()
        for (sound in raw.listFiles().orEmpty()) {
            val recording = sound.readBytes()
            assertTrue(sound.name, upstream.none { it.readBytes().contentEquals(recording) })
        }
    }

    @Test fun everySoundTheAppPlaysIsBundled() {
        val code = File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        val played = Regex("""\.play\("(\w+)"\)""").findAll(code).map { it.groupValues[1] }.toSet() + SoundPlayer.stateSounds.values
        assertTrue(played.isNotEmpty())
        played.forEach { assertTrue("$it.wav", File(raw, "$it.wav").exists()) }
    }

    @Test fun noBundledSoundIsLeftUnused() {
        val code = File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" && it.name != "SoundPlayer.kt" }.joinToString("\n") { it.readText() }
        val played = Regex("""\.play\("(\w+)"\)""").findAll(code).map { it.groupValues[1] }.toSet() + SoundPlayer.stateSounds.values
        assertEquals(raw.listFiles()!!.map { it.nameWithoutExtension }.toSet(), played)
    }


    @Test fun rawNamesAreValidAndroidResourceNames() {
        raw.listFiles()!!.forEach { assertTrue(it.name, Regex("""[a-z][a-z0-9_]*\.wav""").matches(it.name)) }
    }

    @Test fun soundTableCoversEveryBundledSound() {
        val src = File("src/main/kotlin/com/coucou/android/sound/SoundPlayer.kt").readText()
        val mapped = Regex("\"(\\w+)\" to R\\.raw\\.(\\w+)").findAll(src).map { assertEquals(it.groupValues[1], it.groupValues[2]); it.groupValues[1] }.toSet()
        assertEquals(raw.listFiles()!!.map { it.nameWithoutExtension }.toSet(), mapped)
    }

    @Test fun notificationIconIsMonochromeVector() {
        assertTrue(File(res, "drawable/ic_stat_coumpanion.xml").readText().contains("#FFFFFFFF"))
    }

    @Test fun launcherIconIsPresent() {
        assertTrue(File(res, "mipmap-anydpi/ic_launcher.xml").readText().contains("<vector"))
    }
}
