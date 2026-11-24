package com.coucou.android.core

import kotlin.math.abs
import kotlin.math.roundToInt

/** A rectangle in pixels (the display cutout's bounding box at the top of the screen). */
data class PxRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX: Int get() = (left + right) / 2
    val width: Int get() = right - left
}

enum class IslandPosition { CAMERA, CENTER, LEFT, RIGHT }
enum class IslandSize(val scale: Float) { SMALL(0.91f), MEDIUM(1f), LARGE(1.16f) }

/** The camera anchor, safe window bounds and layout sizes, in pixels. */
data class IslandBox(
    val anchorX: Int,
    val windowTop: Int,
    val leftEdge: Int,
    val rightEdge: Int,
    val seedWidth: Int,
    val workingW: Int, val workingH: Int,
    val cardW: Int, val cardH: Int,
    val expandedW: Int, val expandedH: Int,
    val approvalW: Int, val approvalH: Int,
    val panelCorner: Int,
    val maxHeight: Int,
) {
    val maxWidth: Int get() = rightEdge - leftEdge

    /** Keep the compact pill close to the actual camera even when the expanded card needs to shift. */
    fun leftFor(width: Int, position: IslandPosition = IslandPosition.CAMERA): Int {
        val lastLeft = (rightEdge - width).coerceAtLeast(leftEdge)
        val left = when (position) {
            IslandPosition.CAMERA -> anchorX - width / 2
            IslandPosition.CENTER -> (leftEdge + rightEdge - width) / 2
            IslandPosition.LEFT -> leftEdge
            IslandPosition.RIGHT -> lastLeft
        }
        return left.coerceIn(leftEdge, lastLeft)
    }
}

/**
 * Place the entire window below the cutout and status bar. Side insets keep the pill clear of a
 * landscape cutout, and phones with no top cutout use the centre of the usable screen.
 */
object IslandGeometry {
    const val MARGIN_DP = 12
    const val CUTOUT_GAP_DP = 0

    fun place(
        screenW: Int, density: Float, cutoutTop: PxRect?, statusBarH: Int,
        safeTop: Int = 0, safeLeft: Int = 0, safeRight: Int = 0,
        panelWidth: Int = screenW, fontScale: Float = 1f, size: IslandSize = IslandSize.MEDIUM,
        screenH: Int = Int.MAX_VALUE, safeBottom: Int = 0,
    ): IslandBox {
        fun dp(v: Int) = (v * density).toInt()
        val safeStart = safeLeft.coerceIn(0, screenW - 1)
        val safeEnd = (screenW - safeRight).coerceIn(safeStart + 1, screenW)
        val margin = minOf(dp(MARGIN_DP), (safeEnd - safeStart - 1) / 2)
        val left = safeStart + margin
        val right = safeEnd - margin
        fun scaledDp(dp: Int) = (dp * density * size.scale).toInt()
        fun width(limit: Int) = scaledDp(limit).coerceIn(1, right - left)

        val cut = cutoutTop?.takeIf { it.width > 0 && it.bottom > 0 }
        val windowTop = maxOf(statusBarH, safeTop, cut?.bottom ?: 0, 0) + dp(CUTOUT_GAP_DP)
        // Some Samsung devices report a centred hole in physical pixels at a lower resolution.
        // Keep valid off-centre bounds unless they are outside the logical screen or at panel centre.
        val anchor = cut?.let {
            val physicalCoordinates = panelWidth > screenW &&
                (it.right > screenW || abs(it.centerX - panelWidth / 2) <= 1)
            if (physicalCoordinates) (it.centerX.toDouble() * screenW / panelWidth).roundToInt() else it.centerX
        } ?: ((safeStart + safeEnd) / 2)
        val extraTextHeight = (fontScale.coerceAtLeast(1f) - 1f) * density
        return IslandBox(
            anchorX = anchor, windowTop = windowTop, leftEdge = left, rightEdge = right,
            seedWidth = width(52),
            workingW = width(220), workingH = scaledDp(52) + (30 * extraTextHeight).toInt(),
            cardW = width(240), cardH = scaledDp(54) + (30 * extraTextHeight).toInt(),
            expandedW = width(264), expandedH = scaledDp(114) + (60 * extraTextHeight).toInt(),
            approvalW = width(272), approvalH = scaledDp(138) + (76 * extraTextHeight).toInt(),
            panelCorner = scaledDp(24),
            maxHeight = (screenH - windowTop - safeBottom).coerceAtLeast(1),
        )
    }

    /** Size for a kind of content. */
    fun sizeFor(box: IslandBox, kind: IslandSpec.Kind, expanded: Boolean = false): Pair<Int, Int> = when {
        kind == IslandSpec.Kind.APPROVAL -> box.approvalW to box.approvalH
        expanded -> box.expandedW to box.expandedH
        kind == IslandSpec.Kind.WORKING -> box.workingW to box.workingH
        else -> box.cardW to box.cardH
    }
}
