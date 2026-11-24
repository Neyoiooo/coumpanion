package com.coucou.android.core

import java.util.Locale

/** A classified match describes what a provider's notification says, never an observed AI state. */
enum class AiNotificationEvent {
    UPDATE, COMPLETION_REPORTED, ATTENTION_REPORTED;

    companion object {
        private val titleRules = mapOf(
            WatchedAiApp.CHATGPT to mapOf("task complete" to COMPLETION_REPORTED, "your task is complete" to COMPLETION_REPORTED),
            WatchedAiApp.CLAUDE to mapOf("claude has finished" to COMPLETION_REPORTED),
            WatchedAiApp.GEMINI to mapOf("your deep research report is ready" to COMPLETION_REPORTED),
            WatchedAiApp.DEEPSEEK to mapOf("response failed" to ATTENTION_REPORTED),
            WatchedAiApp.PERPLEXITY to mapOf("your report is ready" to COMPLETION_REPORTED),
        )

        fun classify(app: WatchedAiApp, title: String): AiNotificationEvent =
            titleRules[app]?.get(title.trim().lowercase(Locale.ROOT).removeSuffix(".")) ?: UPDATE
    }
}

/** Holds mobile sessions and notification history on the app's main thread. Only enum values, packages and times are written. */
class AiSessionTracker(private val store: KeyValueStore) {
    data class Snapshot(val manual: List<ManualAiSession>, val activity: List<WatchedActivity>)

    var snapshot = Snapshot(
        decode(MANUAL, WatchedAiApp.entries.size) { fields, app, time ->
            ManualAiSession(app, time).takeIf { fields.size == 2 }
        }.distinctBy { it.app },
        decode(ACTIVITY, WatchedActivity.LIMIT) { fields, app, time ->
            val event = AiNotificationEvent.entries.firstOrNull { it.name == fields.getOrNull(2) }
            event?.let { WatchedActivity(app, time, it) }
        },
    ); private set

    fun setManual(app: WatchedAiApp, enabled: Boolean, nowMs: Long) {
        val sessions = snapshot.manual
        if (enabled == sessions.any { it.app == app }) return
        saveManual(if (enabled) listOf(ManualAiSession(app, nowMs)) + sessions else sessions.filterNot { it.app == app })
    }

    fun retainInstalled(installed: Set<WatchedAiApp>) = saveManual(snapshot.manual.filter { it.app in installed })

    fun record(app: WatchedAiApp, event: AiNotificationEvent, nowMs: Long) =
        saveActivity(WatchedActivity.record(snapshot.activity, app, nowMs, event))

    fun clearActivity(app: WatchedAiApp? = null) =
        saveActivity(if (app == null) emptyList() else snapshot.activity.filterNot { it.app == app })

    private fun saveManual(sessions: List<ManualAiSession>) {
        if (sessions == snapshot.manual) return
        store.put(MANUAL, sessions.joinToString("\n") { "${it.app.packageName}|${it.startedAtMs}" })
        snapshot = snapshot.copy(manual = sessions)
    }

    private fun saveActivity(activity: List<WatchedActivity>) {
        if (activity == snapshot.activity) return
        store.put(ACTIVITY, activity.joinToString("\n") { "${it.app.packageName}|${it.atMs}|${it.event.name}" })
        snapshot = snapshot.copy(activity = activity)
    }

    private fun <T> decode(key: String, limit: Int, row: (List<String>, WatchedAiApp, Long) -> T?): List<T> =
        store.getString(key, "").lineSequence().take(limit).mapNotNull { line ->
            val fields = line.split('|')
            val app = WatchedAiApp.fromPackage(fields[0]) ?: return@mapNotNull null
            val time = fields.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            row(fields, app, time)
        }.toList()

    companion object {
        private const val MANUAL = "ai_manual_sessions"
        private const val ACTIVITY = "ai_notification_history"
    }
}
