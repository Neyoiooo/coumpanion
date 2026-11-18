package com.coucou.android.core

import kotlin.math.max
import kotlin.math.min

/** A rectangle in pixels (the display cutout's bounding box at the top of the screen). */
data class PxRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX: Int get() = (left + right) / 2
    val width: Int get() = right - left
}

/** Where the island sits and how big it is, in pixels. */
data class IslandBox(
    /** Horizontal offset of the island's centre from the screen's centre (negative = left). */
    val centerOffsetX: Int,
    /** Space at the top that the camera cut-out or status bar takes: content starts below it. */
    val topInset: Int,
    /** Width of the island when it is hidden in the notch. */
    val notchWidth: Int,
    val workingW: Int, val workingH: Int,
    val cardW: Int, val cardH: Int,
    val approvalW: Int, val approvalH: Int,
    val cornerSmall: Int, val cornerLarge: Int,
)

/**
 * The island is centred on the camera cut-out (a centred waterdrop on the Galaxy A12s), its black
 * merges with the hole, and its content starts under it. With no cut-out it hangs from the
 * status bar, centred on the screen. Approval controls have more room than status messages.
 */
object IslandGeometry {
    const val MARGIN_DP = 12
    /** The concave flare on each side where the island meets the top edge of the screen (the PC's `#island::before/after`). */
    const val EAR_DP = 8
    const val WORKING_W_DP = 176
    const val RESULT_W_DP = 200
    const val APPROVAL_W_DP = 272
    const val MIN_NOTCH_DP = 44
    const val HOLE_PAD_DP = 10

    /**
     * [displayScaled]: the screen runs below the panel's own resolution (Samsung's FHD+ on a WQHD+
     * panel). Some builds then report the cut-out's x in the panel's pixels (a Galaxy S20 at 1080 px
     * wide says the centred hole is at x = 720), so its x is not trusted and the island is centred.
     */
    fun place(
        screenW: Int, density: Float, cutoutTop: PxRect?, statusBarH: Int,
        displayScaled: Boolean = false, fontScale: Float = 1f,
    ): IslandBox {
        fun dp(v: Int) = (v * density).toInt()
        val margin = dp(MARGIN_DP) + dp(EAR_DP)
        val maxW = (screenW - 2 * margin).coerceAtLeast(1)
        val cut = cutoutTop?.takeIf { it.width > 0 && it.bottom > 0 }
        val top = max(cut?.bottom ?: statusBarH, 0)
        val notch = min(max((cut?.width ?: 0) + dp(HOLE_PAD_DP), dp(MIN_NOTCH_DP)), maxW)
        fun width(limit: Int) = dp(limit).coerceIn(notch, maxW)
        val approvalW = width(APPROVAL_W_DP)
        val extraTextHeight = (fontScale.coerceAtLeast(1f) - 1f) * density
        // Leave room for the flares when keeping the widest state close to an off-centre camera.
        val rawOffset = cut?.takeUnless { displayScaled }?.let { it.centerX - screenW / 2 } ?: 0
        val room = (screenW / 2 - approvalW / 2 - margin).coerceAtLeast(0)
        val offset = rawOffset.coerceIn(-room, room)
        return IslandBox(
            centerOffsetX = offset, topInset = top, notchWidth = notch,
            workingW = width(WORKING_W_DP), workingH = top + dp(38) + (32 * extraTextHeight).toInt(),
            cardW = width(RESULT_W_DP), cardH = top + dp(44) + (34 * extraTextHeight).toInt(),
            approvalW = approvalW, approvalH = top + dp(138) + (76 * extraTextHeight).toInt(),
            cornerSmall = dp(16), cornerLarge = dp(19),
        )
    }

    /** Size for a kind of content. */
    fun sizeFor(box: IslandBox, kind: IslandSpec.Kind): Pair<Int, Int> = when (kind) {
        IslandSpec.Kind.WORKING -> box.workingW to box.workingH
        IslandSpec.Kind.APPROVAL -> box.approvalW to box.approvalH
        else -> box.cardW to box.cardH
    }
}
