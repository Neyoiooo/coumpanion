package com.coucou.android

import com.coucou.android.core.AiNotificationEvent
import com.coucou.android.core.AvatarFrame
import com.coucou.android.core.AvatarMood
import com.coucou.android.core.AvatarMotion
import com.coucou.android.core.BotState
import com.coucou.android.core.IslandSpec
import com.coucou.android.core.ManualAiSession
import com.coucou.android.core.ProviderAppearance
import com.coucou.android.core.WatchedAiApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarMotionTest {
    @Test fun providerIdsAndAndroidPackagesUseTheSamePalette() {
        val mobileColors = mapOf(
            WatchedAiApp.CHATGPT to 0x78D6A3,
            WatchedAiApp.CLAUDE to 0xE69B71,
            WatchedAiApp.GEMINI to 0x8C9EFF,
            WatchedAiApp.DEEPSEEK to 0x4D80F5,
            WatchedAiApp.PERPLEXITY to 0x43C7C4,
            WatchedAiApp.GROK to 0xA6ADB8,
        )
        mobileColors.forEach { (app, color) ->
            val appearance = ProviderAppearance.resolve(app.packageName)
            assertEquals(color, appearance.accent)
            assertEquals(app.packageName, appearance.iconPackage)
            assertEquals(appearance, ProviderAppearance.resolve(app.label))
        }
        assertEquals(0xB68AEF, ProviderAppearance.resolve("agent_muse").accent)
        assertEquals(0x6366F1, ProviderAppearance.resolve("agent_codex").accent)
        assertTrue(ProviderAppearance.resolve("agent_copilot").colors.distinct().size > 1)
        assertEquals(ProviderAppearance.resolve("example.agent", "First"), ProviderAppearance.resolve("example.agent", "Second"))
    }

    @Test fun manualAndNotificationActivityNeverLookLikeVerifiedWork() {
        val manual = checkNotNull(ManualAiSession.island(listOf(ManualAiSession(WatchedAiApp.CHATGPT, 1))))
        assertEquals(AvatarMood.TRACKING, AvatarMood.forIsland(manual))
        assertEquals(AvatarMood.TRACKING, AvatarMood.forIsland(manual.copy(state = BotState.THINKING)))
        val report = WatchedAiApp.CLAUDE.update("Claude has finished", AiNotificationEvent.COMPLETION_REPORTED)
        assertEquals(AvatarMood.IDLE, AvatarMood.forIsland(report))
        val verified = IslandSpec(IslandSpec.Kind.WORKING, "agent_codex", "Codex", BotState.THINKING, "")
        assertEquals(AvatarMood.THINKING, AvatarMood.forIsland(verified))
        assertEquals(AvatarMood.WORKING, AvatarMood.forIsland(verified.copy(state = BotState.WORKING)))
    }

    @Test fun disabledAnimationsAreStaticAndOneShotEffectsEnd() {
        AvatarMood.entries.forEach { assertEquals(AvatarFrame(), AvatarMotion.frame(it, 2_000, false)) }
        assertTrue(AvatarMotion.frame(AvatarMood.IDLE, 5_260, true).blink)
        assertNotEquals(0f, AvatarMotion.frame(AvatarMood.TRACKING, 300, true).x)
        assertEquals(AvatarMood.COMPLETED, AvatarMotion.frame(AvatarMood.COMPLETED, 300, true).effect)
        assertEquals(AvatarMood.IDLE, AvatarMotion.frame(AvatarMood.COMPLETED, AvatarMotion.CELEBRATION_MS, true).effect)
        assertNotEquals(0f, AvatarMotion.frame(AvatarMood.ERROR, 30, true).x)
        assertEquals(0f, AvatarMotion.frame(AvatarMood.ERROR, AvatarMotion.SHAKE_MS, true).x)
    }
}
