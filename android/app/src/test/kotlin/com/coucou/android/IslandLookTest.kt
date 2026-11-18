package com.coucou.android

import com.coucou.android.core.IslandGeometry
import com.coucou.android.core.IslandPalette
import com.coucou.android.core.PxRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        assertEquals(360, box.anchorX)
        assertEquals("the entire pill clears the camera and status bar", 77, box.windowTop)
    }

    @Test fun theWorkingStripIsNarrow() {
        assertTrue("working strip ${dp(box.workingW)} dp", dp(box.workingW) in 170f..180f)
        assertTrue("short status strip below the cutout", dp(box.workingH) <= 36f)
    }

    @Test fun theResultAndRequestCardsAreWiderThanTheStrip() {
        assertTrue(box.cardW > box.workingW)
        assertTrue(box.approvalW >= box.cardW)
        assertTrue("the result fits in a compact strip", dp(box.cardW) <= 200f && dp(box.cardH) <= 38f)
        assertTrue("the approval fits in a smaller panel", dp(box.approvalW) <= 272f && dp(box.approvalH) <= 138f)
    }

    @Test fun theExpandedPanelGrowsDownwardsFromTheSameTop() {
        assertTrue(box.expandedH > box.cardH && box.expandedW > box.workingW)
        assertEquals(box.anchorX, box.leftFor(box.expandedW) + box.expandedW / 2)
        assertTrue(box.seedWidth < box.workingW / 2)
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
        assertEquals(628, right.anchorX)
        assertTrue(right.leftFor(right.workingW) > right.leftFor(right.approvalW))
        assertEquals(right.rightEdge, right.leftFor(right.approvalW) + right.approvalW)
    }

    @Test fun wideNotchesAndLandscapeSideCutoutsStayOutsideThePill() {
        val wideNotch = IslandGeometry.place(1080, 3f, PxRect(290, 0, 790, 126), statusBarH = 90, safeTop = 126)
        assertEquals(138, wideNotch.windowTop)
        assertEquals(540, wideNotch.anchorX)

        val landscape = IslandGeometry.place(1920, 2f, null, statusBarH = 46, safeLeft = 95, safeRight = 24)
        assertTrue(landscape.leftFor(landscape.approvalW) >= 95)
        assertTrue(landscape.leftFor(landscape.approvalW) + landscape.approvalW <= 1920 - 24)
        assertEquals(995, landscape.anchorX)
    }

    @Test fun anOffCentreCutoutStaysAlignedOnAScaledScreen() {
        val left = IslandGeometry.place(1080, 3f, PxRect(85, 20, 155, 96), statusBarH = 72, panelWidth = 1440)
        assertEquals(120, left.anchorX)
        assertEquals(108, left.windowTop)
    }

    @Test fun theGlowFollowsTheProviderAcrossAliases() {
        val claude = IslandPalette.forAgent("integration_claude", "Editing")
        assertEquals(0xFFFF9A68, claude.glow)
        assertEquals(claude, IslandPalette.forAgent("session-a", "Claude Desktop"))
        assertEquals(IslandPalette.forAgent("agent_codex", "Codex"), IslandPalette.forAgent("session-b", "ChatGPT"))
        assertTrue(claude != IslandPalette.forAgent("session-c", "DeepSeek Flash"))
        assertNotEquals(claude.pixels, IslandPalette.forAgent("agent_codex", "Codex").pixels)
    }
}
