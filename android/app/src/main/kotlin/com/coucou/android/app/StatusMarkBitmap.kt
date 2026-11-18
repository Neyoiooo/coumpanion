package com.coucou.android.app

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.coucou.android.core.WidgetTone
import com.coucou.android.core.BotState
import com.coucou.android.core.StatePalette
import com.coucou.android.ui.drawStatusMark
import com.coucou.android.ui.rgbColor

/** Cached status marks for widgets, which only display static images. */
object StatusMarkBitmap {
    private const val KEEP = 16

    /** How strong the glow is at its centre; a muted state glows less. */
    private const val GLOW_ALPHA = 0.55f
    private const val GLOW_ALPHA_MUTED = 0.32f

    private data class Key(val state: BotState, val size: Int, val glow: Boolean, val tone: WidgetTone)

    private val cache = object : LinkedHashMap<Key, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Bitmap>) = size > KEEP
    }

    /** A status mark with an optional glow in the card’s colour. */
    @Synchronized
    fun of(state: BotState, sizePx: Int, glow: Boolean = false, tone: WidgetTone = WidgetTone.of(state)): Bitmap {
        val key = Key(state, sizePx, glow, tone)
        return cache.getOrPut(key) { draw(state, sizePx, glow, tone) }
    }

    private fun draw(state: BotState, sizePx: Int, glow: Boolean, tone: WidgetTone): Bitmap {
        val image = ImageBitmap(sizePx, sizePx)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(sizePx.toFloat(), sizePx.toFloat())) {
            if (glow) {
                val centre = Offset(sizePx / 2f, sizePx / 2f)
                val light = Color(tone.argb).copy(alpha = if (tone == WidgetTone.MUTED || tone == WidgetTone.IDLE) GLOW_ALPHA_MUTED else GLOW_ALPHA)
                drawCircle(Brush.radialGradient(listOf(light, Color.Transparent), centre, sizePx / 2f), sizePx / 2f, centre)
            }
            drawStatusMark(rgbColor(StatePalette.color(state)))
        }
        return image.asAndroidBitmap()
    }
}
