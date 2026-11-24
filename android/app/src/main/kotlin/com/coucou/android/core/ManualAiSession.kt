package com.coucou.android.core

/** A user-started marker is intentionally not evidence that an external AI is working. */
data class ManualAiSession(val app: WatchedAiApp, val startedAtMs: Long) {
    fun island(): IslandSpec = IslandSpec(
        IslandSpec.Kind.WORKING, app.packageName, app.label, BotState.IDLE, "",
        manualTracking = true,
    )
}
