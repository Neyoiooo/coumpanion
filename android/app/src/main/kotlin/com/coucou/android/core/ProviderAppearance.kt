package com.coucou.android.core

import java.util.Locale

/** Colors describe the provider. Status colors remain independent. */
internal data class ProviderAppearance(
    val colors: List<Int>,
    val logo: String? = null,
    val iconPackage: String? = null,
) {
    val accent: Int get() = colors.first()

    companion object {
        private fun known(color: Int, logo: String, app: WatchedAiApp? = null) =
            ProviderAppearance(listOf(color), logo, app?.packageName)

        private val chatGpt = known(0x78D6A3, "chatgpt", WatchedAiApp.CHATGPT)
        private val claude = known(0xE69B71, "claude", WatchedAiApp.CLAUDE)
        private val gemini = known(0x8C9EFF, "gemini", WatchedAiApp.GEMINI)
        private val deepSeek = known(0x4D80F5, "deepseek", WatchedAiApp.DEEPSEEK)
        private val perplexity = known(0x43C7C4, "perplexity", WatchedAiApp.PERPLEXITY)
        private val muse = known(0xB68AEF, "muse")
        private val copilot = ProviderAppearance(listOf(0xB8A9FA, 0x95DDF1, 0x9AE0BC, 0xF6B8D5), "copilot", WatchedAiApp.COPILOT.packageName)
        private val grok = known(0xA6ADB8, "grok", WatchedAiApp.GROK)
        private val codex = known(0x6366F1, "codex")

        private val aliases = mapOf(
            "chatgpt" to chatGpt, "openai" to chatGpt, "ai_openai" to chatGpt,
            "claude" to claude, "anthropic" to claude, "ai_anthropic" to claude,
            "integration_claude" to claude, "agent_claude-desktop" to claude,
            "gemini" to gemini, "agent_gemini" to gemini, "ai_google" to gemini,
            "deepseek" to deepSeek, "ai_deepseek" to deepSeek,
            "perplexity" to perplexity, "ai_perplexity" to perplexity,
            "muse" to muse, "agent_muse" to muse,
            "copilot" to copilot, "agent_copilot" to copilot,
            "grok" to grok, "ai_grok" to grok,
            "codex" to codex, "agent_codex" to codex,
        ) + listOf(chatGpt, claude, gemini, deepSeek, perplexity, copilot, grok)
            .associateBy { checkNotNull(it.iconPackage) }

        private val colorWheel = listOf(
            0x75C8B8, 0x7FB9E9, 0x9A9DEE, 0xC49BE2, 0xE39BBB, 0xE5A88E,
            0xE4BE79, 0xA6CB82, 0x7BCBB6, 0x75BDCF, 0xA0ADD8, 0xCF9FC7,
        )

        fun resolve(providerId: String, name: String = ""): ProviderAppearance {
            val id = providerId.trim().lowercase(Locale.ROOT)
            val firstWord = name.trim().lowercase(Locale.ROOT).substringBefore(' ')
            return aliases[id] ?: aliases[firstWord] ?:
                ProviderAppearance(listOf(colorWheel[Math.floorMod(id.hashCode(), colorWheel.size)]))
        }
    }
}
