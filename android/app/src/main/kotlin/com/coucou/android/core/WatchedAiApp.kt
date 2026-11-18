package com.coucou.android.core

/** Only these specific Android packages are offered for notification watching. */
enum class WatchedAiApp(val packageName: String, val label: String) {
    CLAUDE("com.anthropic.claude", "Claude"),
    CHATGPT("com.openai.chatgpt", "ChatGPT"),
    GEMINI("com.google.android.apps.bard", "Gemini"),
    DEEPSEEK("com.deepseek.chat", "DeepSeek"),
    PERPLEXITY("ai.perplexity.app.android", "Perplexity"),
    COPILOT("com.microsoft.copilot", "Copilot"),
    GROK("ai.x.grok", "Grok");

    fun update(title: String): IslandSpec = IslandSpec(
        IslandSpec.Kind.APP_UPDATE, packageName, label, BotState.IDLE, title,
    )

    companion object {
        private val byPackage = entries.associateBy { it.packageName }

        fun fromPackage(packageName: String): WatchedAiApp? = byPackage[packageName]

        /** Recognize approved packages by exact identity, even when Android delivers other apps' notifications. */
        fun selected(packageName: String, allowlist: Set<String>): WatchedAiApp? =
            fromPackage(packageName)?.takeIf { packageName in allowlist }

        fun shortTitle(title: CharSequence?): String = SafeText.prose(
            (title ?: "").take(160)
                .filterNot { (it.isISOControl() && !it.isWhitespace()) || Character.getType(it) == Character.FORMAT.toInt() }
                .toString(),
        )
    }
}
