package com.coucou.android.core

/** Bounded in-memory receipts for Android notification identities, never task identities. */
internal class NotificationDeduplicator(private val limit: Int = 96, private val replayMs: Long = 2_500) {
    private data class Receipt(val title: String, val removedAtMs: Long? = null)
    private val receipts = LinkedHashMap<String, Receipt>(16, 0.75f, true)

    fun accept(key: String, title: String, nowMs: Long): Boolean {
        val previous = receipts.put(key, Receipt(title))
        if (receipts.size > limit) receipts.remove(receipts.keys.first())
        if (previous == null || previous.title != title) return true
        val removed = previous.removedAtMs ?: return false
        return nowMs - removed > replayMs
    }

    fun removed(key: String, nowMs: Long) {
        receipts[key]?.let { receipts[key] = it.copy(removedAtMs = nowMs) }
    }

    fun clear() = receipts.clear()
}
