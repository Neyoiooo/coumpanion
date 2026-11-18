package com.coucou.android

import com.coucou.android.core.BotState
import com.coucou.android.core.StatePalette
import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks the phone’s status colours against the desktop reference. */
class StatePaletteTest {
    private val src = ReferenceFiles.read("windows/src/mochi/engine.ts")

    @Test fun stateColoursMatchReference() {
        val block = src.substringAfter("const C = {").substringBefore("};")
        val re = Regex("""(\w+):\s*\[([\d.]+),\s*([\d.]+),\s*([\d.]+)]""")
        val ref = re.findAll(block).associate { it.groupValues[1] to Triple(it.groupValues[2], it.groupValues[3], it.groupValues[4]) }
        assertEquals(11, ref.size)
        for (s in BotState.entries) {
            val (r, g, b) = ref.getValue(s.key)
            val c = StatePalette.color(s)
            assertEquals("${s.key}.r", r.toDouble(), c.r, 1e-9)
            assertEquals("${s.key}.g", g.toDouble(), c.g, 1e-9)
            assertEquals("${s.key}.b", b.toDouble(), c.b, 1e-9)
        }
    }
}
