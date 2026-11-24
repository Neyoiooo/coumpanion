package com.coucou.android.core

import java.util.Locale

/** A classified match describes what a provider's notification says, never an observed AI state. */
enum class AiNotificationEvent {
    UPDATE, COMPLETION_REPORTED, ATTENTION_REPORTED;

    companion object {
        // Existing exact English matches. Other wording and languages remain generic updates.
        private data class TitleRule(val locale: Locale, val matches: Map<String, AiNotificationEvent>)
        private fun english(vararg titles: Pair<String, AiNotificationEvent>) = TitleRule(Locale.ENGLISH, mapOf(*titles))

        private val titleRules = mapOf(
            WatchedAiApp.CHATGPT to listOf(english("task complete" to COMPLETION_REPORTED, "your task is complete" to COMPLETION_REPORTED)),
            WatchedAiApp.CLAUDE to listOf(english("claude has finished" to COMPLETION_REPORTED)),
            WatchedAiApp.GEMINI to listOf(english("your deep research report is ready" to COMPLETION_REPORTED)),
            WatchedAiApp.DEEPSEEK to listOf(english("response failed" to ATTENTION_REPORTED)),
            WatchedAiApp.PERPLEXITY to listOf(english("your report is ready" to COMPLETION_REPORTED)),
        )

        fun classify(app: WatchedAiApp, title: String): AiNotificationEvent {
            val cleaned = title.trim().removeSuffix(".")
            for (rule in titleRules[app].orEmpty()) {
                rule.matches[cleaned.lowercase(rule.locale)]?.let { return it }
            }
            return UPDATE
        }
    }
}

/** Main-thread session state. Manual transitions and their history are persisted together. */
class AiSessionTracker(private val store: KeyValueStore) {
    data class Snapshot(
        val manual: List<ManualAiSession>,
        val activity: List<WatchedActivity>,
        val history: List<AiSessionHistory>,
        val providers: List<AiProvider>,
    )

    private val custom = store.safeString(PROVIDERS).lineSequence().take(CUSTOM_LIMIT).mapNotNull { row ->
        runCatching {
            val f = row.split('|')
            require(f.size == 7 && f[2] in setOf("app", "web"))
            AiProvider.custom(decodeLocalField(f[1]), f[2] == "app", decodeLocalField(f[3]),
                f[4].toIntOrNull(), f[5].toBooleanStrictOrNull(), f[6].takeIf { it.isNotEmpty() }, f[0])
        }.getOrNull()
    }.distinctBy { it.id }.toMutableList()

    private val providers get() = AiProvider.builtIn.filterNot { builtIn ->
        builtIn.website != null && custom.any { sameTarget(builtIn, it) }
    } + custom

    var snapshot = loadSnapshot()
        private set

    private fun loadSnapshot(): Snapshot {
        val stored = store.safeString(SESSIONS)
        val rows = stored.removePrefix(HEADER).lineSequence().take(MAX_ACTIVE + HISTORY_LIMIT).toList()
        return Snapshot(
            if (stored.startsWith(HEADER)) rows.mapNotNull(::decodeSession).distinctBy { it.provider.id }.take(MAX_ACTIVE)
            else decodeLegacy(),
            store.safeString(ACTIVITY).lineSequence().take(WatchedActivity.LIMIT).mapNotNull { row ->
                val fields = row.split('|')
                val app = WatchedAiApp.fromPackage(fields.first()) ?: return@mapNotNull null
                val time = fields.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                val event = AiNotificationEvent.entries.firstOrNull { it.name == fields.getOrNull(2) }
                event?.let { WatchedActivity(app, time, it) }
            }.toList(),
            rows.mapNotNull(::decodeHistory).distinctBy { it.id }.take(HISTORY_LIMIT),
            providers,
        )
    }

    fun provider(id: String): AiProvider? = providers.firstOrNull { it.id == id }

    fun saveProvider(provider: AiProvider): Boolean {
        if (!provider.isCustom) return false
        if (providers.any { it.id != provider.id && sameTarget(it, provider) }) return false
        val index = custom.indexOfFirst { it.id == provider.id }
        if (index == -1 && custom.size >= CUSTOM_LIMIT) return false
        if (index == -1) custom.add(provider) else custom[index] = provider
        saveProviders()
        save(snapshot.manual.map { if (it.provider.id == provider.id) it.copy(provider = provider) else it })
        return true
    }

    fun removeProvider(id: String, now: SessionTime) {
        if (custom.none { it.id == id }) return
        end(id, AiSessionResult.UNAVAILABLE, now)
        custom.removeAll { it.id == id }
        saveProviders()
    }

    private fun sameTarget(a: AiProvider, b: AiProvider): Boolean =
        if (b.packageName != null) a.packageName == b.packageName else a.website == b.website

    private fun saveProviders() {
        store.put(PROVIDERS, custom.joinToString("\n") {
            listOf(it.id, encodeLocalField(it.label), if (it.packageName != null) "app" else "web",
                encodeLocalField(it.packageName ?: checkNotNull(it.website)), it.accent ?: "",
                it.autoTrack ?: "", it.logoToken ?: "").joinToString("|")
        })
        snapshot = snapshot.copy(providers = providers)
    }

    fun restore(now: SessionTime) {
        val restored = snapshot.manual.map { s ->
            if (s.anchorMs == null) s.checkpoint(now)
            else if (now.bootCount < 0 || now.bootCount != s.bootCount || now.elapsedMs < s.anchorMs) {
                s.copy(anchorMs = now.elapsedMs, bootCount = now.bootCount)
            } else s
        }
        save(restored, force = !store.safeString(SESSIONS).startsWith(HEADER))
        if (store.safeString(LEGACY).isNotEmpty()) store.put(LEGACY, "")
    }

    fun checkpoint(now: SessionTime) {
        save(snapshot.manual.map { if (it.paused) it else it.checkpoint(now) })
    }

    fun start(id: String, now: SessionTime, policy: ManualSessionPolicy): Boolean {
        if (session(id) != null || snapshot.manual.size >= MAX_ACTIVE) return false
        val provider = provider(id) ?: return false
        save(listOf(newSession(provider, now, policy)) + snapshot.manual)
        return true
    }

    private fun newSession(provider: AiProvider, now: SessionTime, policy: ManualSessionPolicy) =
        ManualAiSession(provider, now.wallMs, anchorMs = now.elapsedMs, bootCount = now.bootCount,
            policy = ManualSessionPolicy(
                policy.expirationMinutes.coerceIn(0, ManualSessionPolicy.MAX_MINUTES),
                policy.reminderMinutes.coerceIn(0, ManualSessionPolicy.MAX_MINUTES),
            ))

    fun pause(id: String, now: SessionTime) {
        change(id) { if (it.paused) it else it.checkpoint(now).copy(paused = true) }
    }

    fun resume(id: String, now: SessionTime) {
        change(id) { if (!it.paused) it else it.checkpoint(now).copy(paused = false) }
    }

    fun restart(id: String, now: SessionTime, policy: ManualSessionPolicy) {
        val timer = session(id) ?: return
        save(listOf(newSession(timer.provider, now, policy)) + snapshot.manual.filterNot { it.id == timer.id },
            listOf(finished(timer, AiSessionResult.CANCELLED, now)) + snapshot.history)
    }

    private fun session(id: String) = snapshot.manual.firstOrNull { it.provider.id == id }

    private fun change(id: String, update: (ManualAiSession) -> ManualAiSession) =
        save(snapshot.manual.map { if (it.provider.id == id) update(it) else it })

    fun end(id: String, status: AiSessionResult, now: SessionTime) {
        val timer = session(id) ?: return
        require(status in MANUAL_RESULTS)
        save(snapshot.manual.filterNot { it.id == timer.id }, listOf(finished(timer, status, now)) + snapshot.history)
    }

    private fun finished(timer: ManualAiSession, status: AiSessionResult, now: SessionTime): AiSessionHistory {
        val duration = timer.elapsed(now).let {
            if (status == AiSessionResult.EXPIRED) it.coerceAtMost(timer.policy.expirationMinutes * MINUTE) else it
        }
        return AiSessionHistory(timer.id, timer.provider.id, timer.provider.label, timer.startedAtMs,
            now.wallMs.coerceAtLeast(timer.startedAtMs), duration, AiActivitySource.MANUAL, status)
    }

    /** Returns reminders due now. A reminder never changes the activity source or reports progress. */
    fun tick(now: SessionTime): List<AiProvider> {
        val reminders = mutableListOf<AiProvider>()
        for (timer in snapshot.manual.filterNot { it.paused }) {
            val elapsed = timer.elapsed(now)
            val policy = timer.policy
            if (policy.expirationMinutes > 0 && elapsed >= policy.expirationMinutes * MINUTE) {
                end(timer.provider.id, AiSessionResult.EXPIRED, now)
            } else if (!timer.reminded && policy.reminderMinutes > 0 && elapsed >= policy.reminderMinutes * MINUTE) {
                change(timer.provider.id) { it.copy(reminded = true).checkpoint(now) }
                reminders += timer.provider
            }
        }
        return reminders
    }

    fun nextCheck(now: SessionTime): Long? = snapshot.manual.asSequence().filterNot { it.paused }
        .flatMap { timer ->
            sequenceOf(timer.policy.expirationMinutes, if (timer.reminded) 0 else timer.policy.reminderMinutes)
                .filter { it > 0 }.map { (it * MINUTE - timer.elapsed(now)).coerceAtLeast(0) }
        }.minOrNull()

    fun retainInstalled(installed: Set<String>, now: SessionTime) {
        snapshot.manual.filter { session ->
            val packageName = session.provider.packageName
            !session.provider.isCustom && packageName != null && packageName !in installed
        }
            .forEach { end(it.provider.id, AiSessionResult.UNAVAILABLE, now) }
    }

    fun recordHistory(entry: AiSessionHistory) = save(history = listOf(entry) + snapshot.history.filterNot { it.id == entry.id })
    fun clearHistory(id: String? = null) = save(history = if (id == null) emptyList() else snapshot.history.filterNot { it.id == id })

    fun record(app: WatchedAiApp, event: AiNotificationEvent, nowMs: Long) =
        saveActivity(WatchedActivity.record(snapshot.activity, app, nowMs, event))

    fun clearActivity(app: WatchedAiApp? = null) =
        saveActivity(if (app == null) emptyList() else snapshot.activity.filterNot { it.app == app })

    fun removeActivity(entry: WatchedActivity) = saveActivity(snapshot.activity - entry)

    private fun save(
        sessions: List<ManualAiSession> = snapshot.manual,
        history: List<AiSessionHistory> = snapshot.history,
        force: Boolean = false,
    ) {
        val entries = history.take(HISTORY_LIMIT)
        if (!force && sessions == snapshot.manual && entries == snapshot.history) return
        val data = sessions.map {
            listOf("S", it.id, it.provider.id, it.startedAtMs, it.accruedMs, it.anchorMs ?: "",
                it.bootCount, it.paused, it.reminded, it.policy.expirationMinutes, it.policy.reminderMinutes).joinToString("|")
        } + entries.map {
            listOf("H", it.id, encodeLocalField(it.providerId), encodeLocalField(it.providerLabel), it.startedAtMs,
                it.endedAtMs, it.durationMs, it.source.name, it.result.name).joinToString("|")
        }
        store.put(SESSIONS, HEADER + data.joinToString("\n"))
        snapshot = snapshot.copy(manual = sessions, history = entries)
    }

    private fun saveActivity(activity: List<WatchedActivity>) {
        if (activity == snapshot.activity) return
        store.put(ACTIVITY, activity.joinToString("\n") { "${it.app.packageName}|${it.atMs}|${it.event.name}" })
        snapshot = snapshot.copy(activity = activity)
    }

    private fun decodeLegacy(): List<ManualAiSession> = store.safeString(LEGACY).lineSequence().take(MAX_ACTIVE)
        .mapNotNull { row ->
            val fields = row.split('|')
            if (fields.size != 2) return@mapNotNull null
            val provider = provider(fields[0]) ?: return@mapNotNull null
            val time = fields[1].toLongOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            ManualAiSession(provider, time, id = "legacy:${provider.id}:$time")
        }.distinctBy { it.provider.id }.toList()

    private fun decodeSession(row: String): ManualAiSession? = runCatching {
        val f = row.split('|')
        if (f.size != 11 || f[0] != "S") return null
        val provider = provider(f[2]) ?: return null
        val start = f[3].toLong().also { require(it > 0) }
        val accrued = f[4].toLong().also { require(it in 0..ManualAiSession.MAX_DURATION_MS) }
        val anchor = f[5].takeIf { it.isNotEmpty() }?.toLong()?.also { require(it >= 0) }
        ManualAiSession(provider, start, f[1], accrued, anchor, f[6].toInt(), f[7].toBooleanStrict(), f[8].toBooleanStrict(),
            ManualSessionPolicy(f[9].toInt().coerceIn(0, ManualSessionPolicy.MAX_MINUTES), f[10].toInt().coerceIn(0, ManualSessionPolicy.MAX_MINUTES)))
    }.getOrNull()

    private fun decodeHistory(row: String): AiSessionHistory? = runCatching {
        val f = row.split('|')
        if (f.size != 9 || f[0] != "H") return null
        val start = f[4].toLong()
        val end = f[5].toLong()
        val elapsed = f[6].toLong()
        require(start > 0 && end >= start && elapsed in 0..ManualAiSession.MAX_DURATION_MS)
        AiSessionHistory(f[1], decodeLocalField(f[2]), decodeLocalField(f[3]), start, end, elapsed,
            AiActivitySource.valueOf(f[7]), AiSessionResult.valueOf(f[8]))
    }.getOrNull()

    companion object {
        const val MAX_ACTIVE = 32
        const val HISTORY_LIMIT = 128
        private const val CUSTOM_LIMIT = 48
        private const val MINUTE = 60_000L
        private const val LEGACY = "ai_manual_sessions"
        private const val SESSIONS = "ai_session_records"
        private const val HEADER = "v2\n"
        private const val ACTIVITY = "ai_notification_history"
        private const val PROVIDERS = "ai_custom_providers"
        private val MANUAL_RESULTS = setOf(AiSessionResult.MANUAL_DONE, AiSessionResult.CANCELLED, AiSessionResult.EXPIRED, AiSessionResult.UNAVAILABLE)
    }
}
