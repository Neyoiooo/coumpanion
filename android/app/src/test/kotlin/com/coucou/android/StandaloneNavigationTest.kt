package com.coucou.android

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StandaloneNavigationTest {
    private val kotlin = File("src/main/kotlin/com/coucou/android")

    @Test fun phoneHomeAndActivityOpenWithoutAnExistingDesktopLink() {
        val main = kotlin.resolve("MainActivity.kt").readText()
        assertTrue(main.contains("Screen.HOME -> Home("))
        assertTrue(main.contains("CoupiWelcome(model.manualTracking.size)"))
        assertTrue(main.contains("AiLauncher(model,"))
        assertTrue(main.contains("Screen.AI_HISTORY -> AiSessionHistoryScreen(model)"))
    }

    @Test fun retiredPairingEntryPointsAndCameraPermissionAreAbsent() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("android.permission.CAMERA"))
        assertFalse(manifest.contains("android:scheme=\"coucou\""))
        assertFalse(manifest.contains("android.app.shortcuts"))
        val main = kotlin.resolve("MainActivity.kt").readText()
        assertFalse(main.contains("requestPairing("))
        assertFalse(main.contains("Screen.SCAN"))
    }

    @Test fun theAndroidAppDoesNotLogLinkSecretsOrActivityText() {
        val secrets = Regex("""\b(token|certSha256|fingerprint|pairing\.|payload|link|text)\b""")
        for (file in kotlin.walkTopDown().filter { it.isFile && it.extension == "kt" }) {
            file.readLines().filter { it.contains("Log.") || it.contains("println(") }.forEach { line ->
                val call = line.substringAfter("Log.", "").substringAfter("(", "")
                assertFalse("${file.name}: $line", secrets.containsMatchIn(call.substringAfter(",", call)))
            }
        }
    }
}
