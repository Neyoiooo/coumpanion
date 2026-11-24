package com.coucou.android

import com.coucou.android.core.NotificationDeduplicator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeduplicatorTest {
    @Test fun repeatsAndShortReplaysAreSuppressedByIdentityWithoutPairingTasks() {
        val recent = NotificationDeduplicator(limit = 3)
        assertTrue(recent.accept("one", "Aa", 10))
        assertFalse(recent.accept("one", "Aa", 20))
        assertTrue(recent.accept("one", "BB", 30)) // These two strings have the same Java hash.
        assertTrue(recent.accept("two", "BB", 40))
        recent.removed("one", 50)
        assertFalse(recent.accept("one", "BB", 80))
        recent.removed("one", 100)
        assertTrue(recent.accept("one", "BB", 3_000))
        recent.clear()
        assertTrue(recent.accept("one", "BB", 3_010))
    }
}
