package com.coucou.android

import com.coucou.android.core.Screen
import com.coucou.android.core.SecureScreens
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureScreensTest {
    private fun needed(
        screen: Screen, approval: Boolean = false, question: Boolean = false,
    ) = SecureScreens.needed(screen, approval, question)

    @Test fun chatApprovalAndQuestionAreHidden() {
        assertTrue("chat", needed(Screen.CHAT))
        assertTrue("approval over Home", needed(Screen.HOME, approval = true))
        assertTrue("approval over Settings", needed(Screen.SETTINGS, approval = true))
        assertTrue("question", needed(Screen.HOME, question = true))
    }

    @Test fun otherScreensAreNotHidden() {
        for (s in listOf(Screen.HOME, Screen.SETTINGS, Screen.HISTORY, Screen.AI_HISTORY, Screen.SESSION)) {
            assertFalse("$s", needed(s))
        }
    }

    @Test fun theActivityAppliesAndClearsTheFlagFromThatDecision() {
        val src = File("src/main/kotlin/com/coucou/android/MainActivity.kt").readText()
        assertTrue(src.contains("SecureScreens.needed("))
        assertTrue(src.contains("window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)"))
        assertTrue("cleared when the screen is no longer sensitive", src.contains("window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)"))
        // The decision sees every sheet that exists: a new sheet must be added to it.
        for (sheet in listOf("ApprovalSheet(", "QuestionSheet(")) assertTrue(sheet, src.contains(sheet))
    }
}
