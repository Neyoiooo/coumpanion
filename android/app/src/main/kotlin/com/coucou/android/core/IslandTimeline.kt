package com.coucou.android.core

import com.coucou.android.core.BotState

/** What the island shows. [pillId] is the session it is about (or the request's pill). */
data class IslandSpec(
    val kind: Kind,
    val pillId: String,
    val agent: String,
    val state: BotState,
    val text: String,
    /** The request this is about (approvals only): what Allow and Deny act on. */
    val fingerprint: String? = null,
    /** False for a request whose command could not be shown in full: the island offers Deny only. */
    val canAllow: Boolean = true,
    /** A user-started local marker, not verified AI activity. */
    val manualSessions: List<ManualAiSession> = emptyList(),
    val notificationEvent: AiNotificationEvent = AiNotificationEvent.UPDATE,
) {
    val manualTracking: Boolean get() = manualSessions.firstOrNull()?.app?.packageName == pillId

    enum class Kind { WORKING, QUESTION, APPROVAL, FINISHED, ERROR, RATELIMIT, APP_UPDATE }

    /** Working, a question and a permission request stay until something changes. */
    val persistent: Boolean get() = kind == Kind.WORKING || kind == Kind.QUESTION || kind == Kind.APPROVAL
}

/**
 * When the island is open and when it goes back up into the notch, with an injectable clock.
 *
 * - [setActive]: what is going on right now (working, a question, a request). It needs no deadline.
 * - [flash]: a finished, failed or rate-limited event. Held [HOLD_MS] (10 s, as the user asked), then
 *   the island retracts. A newer event restarts the hold.
 * - The island re-opens when something starts while it is retracting; a content change (working to
 *   finished) never retracts it.
 * - [swipeAway]: the user swiped the island up. It goes back into the notch and stays there while the same thing
 *   goes on (same kind, same agent, same request); something new brings it back down.
 * - [nextDeadline] is the only time anything must wake up: while hidden it is null, so nothing runs.
 */
class IslandTimeline(private val clockMs: () -> Double) {
    enum class Phase { HIDDEN, OPEN, RETRACTING }

    var phase = Phase.HIDDEN
        private set

    private var active: IslandSpec? = null
    private var flash: IslandSpec? = null
    private var flashUntil = 0.0
    private var retractUntil = 0.0
    // Mobile updates have their own bounded FIFO; desktop completion notices retain their existing policy.
    private data class QueuedUpdate(val spec: IslandSpec, val receivedAt: Double)
    private val updates = ArrayDeque<QueuedUpdate>()
    private var update: IslandSpec? = null
    private var updateUntil = 0.0

    /** What the user swiped away: kept until that thing is over or something else comes up. */
    private var swiped: IslandSpec? = null

    private fun IslandSpec.matches(other: IslandSpec?): Boolean = other != null &&
        kind == other.kind && pillId == other.pillId && fingerprint == other.fingerprint &&
        manualSessions == other.manualSessions

    /** What to draw now (kept while retracting, so the island shrinks around its last content). */
    var current: IslandSpec? = null
        private set

    fun setActive(spec: IslandSpec?) {
        active = spec
        refresh()
    }

    fun flash(spec: IslandSpec) {
        val now = clockMs()
        if (spec.kind == IslandSpec.Kind.APP_UPDATE) {
            refresh()
            if (update == null || update == spec) {
                update = spec
                updateUntil = now + HOLD_MS
                swiped = null
            } else if (updates.none { it.spec == spec }) {
                if (updates.size >= MAX_PENDING_UPDATES) updates.removeFirst()
                updates.addLast(QueuedUpdate(spec, now))
            }
        } else {
            flash = spec
            flashUntil = now + HOLD_MS
        }
        refresh()
    }

    /** Drop one type of transient event without interrupting an active session. */
    fun clearFlash(kind: IslandSpec.Kind) {
        if (kind == IslandSpec.Kind.APP_UPDATE) {
            update = null
            updates.clear()
            if (swiped?.kind == kind) swiped = null
            refresh()
        } else if (flash?.kind == kind) {
            flash = null
            refresh()
        }
    }

    /** The user swiped the island up: it retracts now, and the same thing does not bring it back. */
    fun swipeAway() {
        if (phase != Phase.OPEN) return
        if (current?.kind == IslandSpec.Kind.APP_UPDATE) {
            // A deliberate dismissal clears pending mobile alerts; history still has their metadata.
            clearFlash(IslandSpec.Kind.APP_UPDATE)
        } else {
            swiped = current
            refresh()
        }
    }

    /** Close at once (Coucou came to the front, the switch went off, the link dropped). */
    fun dismiss() {
        swiped = null
        active = null
        flash = null
        update = null
        updates.clear()
        current = null
        phase = Phase.HIDDEN
    }

    /** Call when [nextDeadline] has passed. */
    fun tick() = refresh()

    /** The island has finished moving up: its window can go. */
    fun retractFinished() {
        if (phase == Phase.RETRACTING) {
            phase = Phase.HIDDEN
            current = null
        }
    }

    /** Absolute clock time of the next change, or null when nothing is scheduled (hidden, or persistent content). */
    fun nextDeadline(): Double? = when (phase) {
        Phase.HIDDEN -> null
        Phase.RETRACTING -> retractUntil
        Phase.OPEN -> when {
            current?.kind == IslandSpec.Kind.APP_UPDATE -> updateUntil
            active == null && flash != null -> flashUntil
            else -> null
        }
    }

    private fun refresh() {
        val now = clockMs()
        if (flash != null && now >= flashUntil) flash = null
        if (update != null && now >= updateUntil) update = null
        while (updates.isNotEmpty() && now - updates.first().receivedAt >= MAX_UPDATE_AGE_MS) updates.removeFirst()
        val urgent = active?.takeIf { it.kind == IslandSpec.Kind.APPROVAL || it.kind == IslandSpec.Kind.QUESTION }
        if (urgent == null && update == null && updates.isNotEmpty()) {
            update = updates.removeFirst().spec
            updateUntil = now + HOLD_MS
        }
        // Never cover an approval/question. Mobile alerts may briefly interrupt ordinary working status.
        val candidate = urgent ?: update ?: active ?: flash
        // Nothing going on, or something else than what was swiped: the swipe is forgotten.
        if (candidate == null || !candidate.matches(swiped)) swiped = null
        val shown = candidate?.takeUnless { it.matches(swiped) }
        when {
            shown != null -> {
                current = shown
                phase = Phase.OPEN
            }
            phase == Phase.OPEN -> {
                // Nothing left to show: go back up. The content stays for the animation.
                phase = Phase.RETRACTING
                retractUntil = now + RETRACT_MS
            }
            phase == Phase.RETRACTING && now >= retractUntil -> retractFinished()
        }
    }

    companion object {
        const val HOLD_MS = 10_000.0
        private const val MAX_PENDING_UPDATES = 8
        private const val MAX_UPDATE_AGE_MS = 60_000.0
        const val RETRACT_MS = Tracked.CLOSE_MS
    }
}
