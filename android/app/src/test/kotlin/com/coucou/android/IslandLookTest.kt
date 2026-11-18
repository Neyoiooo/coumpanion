package com.coucou.android

import com.coucou.android.core.IslandGeometry
import com.coucou.android.core.IslandPalette
import com.coucou.android.core.PxRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The island on a 720x1600 phone with a small centred camera hole (the Galaxy A12s class). The
 * hole's rectangle is an assumption (about 56x66 px at the top centre), not a measurement: the real
 * inset is read from the display at run time.
 */
class IslandLookTest {
    private val density = 1.75f // 720 px wide = about 411 dp
    private val hole = PxRect(332, 0, 388, 66)
    private val box = IslandGeometry.place(720, density, hole, statusBarH = 70)

    private fun dp(px: Int) = px / density

    @Test fun theIslandIsCentredOnTheCameraHole() {
        assertEquals(0, box.centerOffsetX)
        assertEquals("content starts under the hole", 66, box.topInset)
    }

    @Test fun theWorkingStripIsNarrow() {
        assertTrue("working strip ${dp(box.workingW)} dp", dp(box.workingW) in 170f..180f)
        assertTrue("short status strip below the cutout", dp(box.workingH - box.topInset) <= 38f)
    }

    @Test fun theResultAndRequestCardsAreWiderThanTheStrip() {
        assertTrue(box.cardW > box.workingW)
        assertTrue(box.approvalW >= box.cardW)
        assertTrue("the result fits in a compact strip", dp(box.cardW) <= 200f && dp(box.cardH - box.topInset) <= 44f)
        assertTrue("the approval fits in a smaller panel", dp(box.approvalW) <= 272f && dp(box.approvalH - box.topInset) <= 138f)
    }

    @Test fun hiddenItIsJustAboveTheHoleSoItGrowsOutOfIt() {
        assertTrue("notch ${dp(box.notchWidth)} dp", dp(box.notchWidth) in 40f..70f)
        assertTrue(box.notchWidth > hole.width)
        assertTrue(box.notchWidth < box.workingW / 2)
    }

    @Test fun largerTextGetsMoreHeightWithoutCoveringMoreWidth() {
        val large = IslandGeometry.place(720, density, hole, statusBarH = 70, fontScale = 1.5f)
        assertEquals(box.workingW, large.workingW)
        assertTrue(large.workingH > box.workingH)
        assertTrue(large.cardH > box.cardH)
        assertTrue(large.approvalH > box.approvalH)
    }

    @Test fun anOffCentreHoleStillKeepsTheWholeIslandOnScreen() {
        val right = IslandGeometry.place(720, density, PxRect(600, 0, 656, 66), statusBarH = 70)
        assertTrue(right.centerOffsetX > 0)
        assertTrue(right.centerOffsetX + right.approvalW / 2 <= 360)
    }

    @Test fun theFlaresFitBesideTheIslandOnANarrowPhone() {
        // 360 dp wide (1080 px at 3.0): island plus a flare on each side stays inside the screen with its margin.
        val narrow = IslandGeometry.place(1080, 3.0f, PxRect(504, 0, 576, 90), statusBarH = 90)
        val ear = (IslandGeometry.EAR_DP * 3.0f).toInt()
        assertTrue(narrow.approvalW + 2 * ear <= 1080 - 2 * (IslandGeometry.MARGIN_DP * 3.0f).toInt())
    }

    @Test fun theGlowFollowsTheProviderAcrossAliases() {
        val claude = IslandPalette.forAgent("integration_claude", "Editing")
        assertEquals(0xFFFF9A68, claude.glow)
        assertEquals(claude, IslandPalette.forAgent("session-a", "Claude Desktop"))
        assertEquals(IslandPalette.forAgent("agent_codex", "Codex"), IslandPalette.forAgent("session-b", "ChatGPT"))
        assertTrue(claude != IslandPalette.forAgent("session-c", "DeepSeek Flash"))
    }
}
