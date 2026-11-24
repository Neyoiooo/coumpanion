package com.coucou.android.core

/** A local history entry contains no notification content, identifiers or actions. */
data class WatchedActivity(val app: WatchedAiApp, val atMs: Long) {
    companion object {
        const val LIMIT = 50

        fun record(previous: List<WatchedActivity>, app: WatchedAiApp, atMs: Long): List<WatchedActivity> =
            (listOf(WatchedActivity(app, atMs)) + previous).take(LIMIT)
    }
}
