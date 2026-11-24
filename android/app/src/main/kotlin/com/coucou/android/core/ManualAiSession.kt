package com.coucou.android.core

import java.util.UUID

enum class AiActivitySource { MANUAL, NOTIFICATION, DESKTOP, PROVIDER }
enum class AiSessionResult { MANUAL_DONE, CANCELLED, EXPIRED, UNAVAILABLE, DESKTOP_DONE, DESKTOP_ERROR }

data class AiSessionHistory(
    val id: String,
    val providerId: String,
    val providerLabel: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val durationMs: Long,
    val source: AiActivitySource,
    val result: AiSessionResult,
)

data class SessionTime(val wallMs: Long, val elapsedMs: Long, val bootCount: Int)

data class ManualSessionPolicy(val expirationMinutes: Int = 0, val reminderMinutes: Int = 0) {
    companion object { const val MAX_MINUTES = 10_080 }
}

/** Monotonic time runs only during tracked intervals. The wall clock labels history. */
data class ManualAiSession(
    val provider: AiProvider,
    val startedAtMs: Long,
    val id: String = UUID.randomUUID().toString(),
    val accruedMs: Long = 0,
    val anchorMs: Long? = null,
    val bootCount: Int = -1,
    val paused: Boolean = false,
    val reminded: Boolean = false,
    val policy: ManualSessionPolicy = ManualSessionPolicy(),
) {
    constructor(app: WatchedAiApp, startedAtMs: Long) :
        this(AiProvider.builtIn.first { it.id == app.packageName }, startedAtMs)

    val source: AiActivitySource get() = AiActivitySource.MANUAL

    fun elapsed(now: SessionTime): Long {
        if (paused) return accruedMs
        val delta = when {
            anchorMs == null -> now.wallMs - startedAtMs // A restored v0.6.0 session.
            bootCount != now.bootCount || now.elapsedMs < anchorMs -> 0
            else -> now.elapsedMs - anchorMs
        }
        return accruedMs + delta.coerceIn(0, MAX_DURATION_MS - accruedMs)
    }

    /** A reboot continues from the saved checkpoint because downtime cannot be measured monotonically. */
    internal fun checkpoint(now: SessionTime): ManualAiSession =
        copy(accruedMs = elapsed(now), anchorMs = now.elapsedMs, bootCount = now.bootCount)

    companion object {
        internal const val MAX_DURATION_MS = 315_360_000_000L

        fun island(sessions: List<ManualAiSession>): IslandSpec? {
            val first = sessions.firstOrNull() ?: return null
            return IslandSpec(
                IslandSpec.Kind.WORKING, first.provider.id, first.provider.label, BotState.IDLE, "",
                manualSessions = sessions,
            )
        }
    }

    fun island(): IslandSpec = checkNotNull(island(listOf(this)))
}
