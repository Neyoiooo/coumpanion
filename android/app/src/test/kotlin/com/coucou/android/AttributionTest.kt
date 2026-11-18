package com.coucou.android

import org.junit.Assert.assertEquals
import org.junit.Test

class AttributionTest {
    @Test fun linksToTheFork() {
        assertEquals("https://github.com/Neyoiooo/coumpanion", Attribution.REPO_URL)
    }
}
