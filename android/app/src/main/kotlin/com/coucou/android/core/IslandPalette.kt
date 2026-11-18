package com.coucou.android.core

/** Provider accents for the island. Session and pill metadata still use their existing colours. */
data class IslandPalette(val glow: Long, val trail: Long, val label: Long) {
    companion object {
        private val claude = IslandPalette(0xFFFF9A68, 0xFFF25770, 0xFFFFBC9A)
        private val openAi = IslandPalette(0xFF65F0BC, 0xFF319CD2, 0xFFACF5D4)
        private val deepSeek = IslandPalette(0xFF6196FF, 0xFF8F66FF, 0xFFAEC6FF)
        private val gemini = IslandPalette(0xFF6EADFF, 0xFFDA68EC, 0xFFC0DAFF)
        private val copilot = IslandPalette(0xFFA496FF, 0xFF55D7D3, 0xFFD5C8FF)
        private val coumpanion = IslandPalette(0xFF6FF1CD, 0xFF9382FF, 0xFFA9F5DF)

        fun forAgent(pillId: String, name: String): IslandPalette {
            val id = pillId.lowercase()
            val display = name.lowercase()
            return when {
                id in setOf("integration_claude", "agent_claude-desktop", "ai_anthropic") -> claude
                id in setOf("agent_codex", "ai_openai") -> openAi
                "deepseek" in id -> deepSeek
                id in setOf("agent_gemini", "ai_google") -> gemini
                id == "agent_copilot" -> copilot
                "claude" in display || "anthropic" in display -> claude
                "codex" in display || "chatgpt" in display || "openai" in display -> openAi
                "deepseek" in display -> deepSeek
                "gemini" in display || "google ai" in display -> gemini
                "copilot" in display -> copilot
                else -> coumpanion
            }
        }
    }
}
