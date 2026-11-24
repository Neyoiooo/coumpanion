package com.coucou.android.core

/** A local history entry contains no notification content, identifiers or actions. */
data class WatchedActivity(
    val app: WatchedAiApp,
    val atMs: Long,
    val event: AiNotificationEvent = AiNotificationEvent.UPDATE,
) {
    companion object {
        const val LIMIT = 50

        fun record(
            previous: List<WatchedActivity>,
            app: WatchedAiApp,
            atMs: Long,
            event: AiNotificationEvent = AiNotificationEvent.UPDATE,
        ): List<WatchedActivity> = (listOf(WatchedActivity(app, atMs, event)) + previous).take(LIMIT)
    }
}
