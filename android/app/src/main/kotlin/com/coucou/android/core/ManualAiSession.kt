package com.coucou.android.core

/** A user-started marker is intentionally not evidence that an external AI is working. */
data class ManualAiSession(val app: WatchedAiApp, val startedAtMs: Long) {
    fun elapsedMinutes(nowMs: Long): Long = (nowMs - startedAtMs).coerceAtLeast(0) / 60_000

    companion object {
        fun island(sessions: List<ManualAiSession>): IslandSpec? {
            val first = sessions.firstOrNull() ?: return null
            return IslandSpec(
                IslandSpec.Kind.WORKING, first.app.packageName, first.app.label, BotState.IDLE, "",
                manualSessions = sessions,
            )
        }
    }

    fun island(): IslandSpec = checkNotNull(island(listOf(this)))
}
