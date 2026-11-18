package com.coucou.android.core

/** Provider accents for the island. Session and pill metadata still use their existing colours. */
data class IslandPalette(val glow: Long, val trail: Long, val label: Long, val pixels: List<Int>) {
    companion object {
        // Five rows of five pixels. Each provider has a distinct silhouette even without colour.
        private val claude = IslandPalette(0xFFFF9A68, 0xFFF25770, 0xFFFFBC9A, listOf(0b00100, 0b10101, 0b01110, 0b11111, 0b01010))
        private val openAi = IslandPalette(0xFF65F0BC, 0xFF319CD2, 0xFFACF5D4, listOf(0b01110, 0b11001, 0b10001, 0b10011, 0b01110))
        private val deepSeek = IslandPalette(0xFF6196FF, 0xFF8F66FF, 0xFFAEC6FF, listOf(0b10000, 0b01001, 0b00110, 0b11111, 0b01110))
        private val gemini = IslandPalette(0xFF6EADFF, 0xFFDA68EC, 0xFFC0DAFF, listOf(0b00100, 0b01110, 0b11011, 0b01110, 0b00100))
        private val copilot = IslandPalette(0xFFA496FF, 0xFF55D7D3, 0xFFD5C8FF, listOf(0b11000, 0b11011, 0b00111, 0b11011, 0b11000))
        private val coumpanion = IslandPalette(0xFF6FF1CD, 0xFF9382FF, 0xFFA9F5DF, listOf(0b00100, 0b01010, 0b11111, 0b01010, 0b00100))

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
