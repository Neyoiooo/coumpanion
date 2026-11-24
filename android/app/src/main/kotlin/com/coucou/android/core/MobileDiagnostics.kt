package com.coucou.android.core

import com.coucou.android.link.LinkState

/** A fixed allowlist for reports. There are no fields for user content or connection addresses. */
internal data class MobileDiagnostics(
    val full: Boolean,
    val version: String,
    val buildCode: Int,
    val debug: Boolean,
    val androidApi: Int,
    val overlay: Boolean,
    val notifications: Boolean,
    val notificationAccess: Boolean?,
    val batterySaver: Boolean,
    val linkService: Boolean,
    val activeTimers: Int,
    val desktop: LinkState,
    val avatarAssets: Boolean,
) {
    val edition get() = if (full) "Full" else "Standard"

    fun report(): String = buildString {
        appendLine("Coumpanion $version ($buildCode) · $edition · ${if (debug) "debug" else "release"}")
        appendLine("Android API: $androidApi")
        appendLine("Overlay permission: $overlay")
        appendLine("Notifications enabled: $notifications")
        notificationAccess?.let { appendLine("Notification access: $it") }
        appendLine("Battery saver: $batterySaver")
        appendLine("Paired-link service running: $linkService")
        appendLine("Manual timers: $activeTimers")
        appendLine("Desktop connection: ${desktop.name}")
        append("Avatar assets ready: $avatarAssets")
    }
}
