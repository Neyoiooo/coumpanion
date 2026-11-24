package com.coucou.android

import com.coucou.android.core.AiNotificationEvent
import com.coucou.android.core.AvatarMood
import com.coucou.android.core.AvatarMotion
import com.coucou.android.core.BotState
import com.coucou.android.core.IslandSpec
import com.coucou.android.core.ManualAiSession
import com.coucou.android.core.ProviderAppearance
import com.coucou.android.core.WatchedAiApp
import java.io.File
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

    @Test fun providersResolveToDistinctLogoAssets() {
        val providers = listOf("chatgpt", "claude", "gemini", "deepseek", "perplexity", "muse", "copilot", "grok", "codex")
        assertEquals(providers, providers.map { ProviderAppearance.resolve(it).logo })
        assertEquals("codex", ProviderAppearance.resolve("agent_codex").logo)
        assertEquals(null, ProviderAppearance.resolve("example.agent").logo)
    }

    @Test fun disabledAnimationsAreStaticAndOneShotEffectsEnd() {
        val motion = AvatarMotion.parse(File("src/main/assets/avatars/face-timeline.txt").readText())
        val staticPoses = AvatarMood.entries.map { mood ->
            motion.frame(mood, 0, true).also { initial ->
                assertEquals(initial, motion.frame(mood, 9_000, false))
            }
        }
        assertEquals(AvatarMood.entries.size, staticPoses.distinct().size)
        assertNotEquals(motion.frame(AvatarMood.IDLE, 0, true), motion.frame(AvatarMood.IDLE, 700, true))
        assertNotEquals(motion.frame(AvatarMood.TRACKING, 0, true), motion.frame(AvatarMood.TRACKING, 450, true))
        assertEquals(motion.frame(AvatarMood.WORKING, 0, true), motion.frame(AvatarMood.WORKING, 840, true))
        assertEquals(motion.frame(AvatarMood.IDLE, 0, true), motion.frame(AvatarMood.COMPLETED, 1_500, true))
    }
}
