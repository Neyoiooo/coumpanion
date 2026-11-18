package com.coucou.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.coucou.android.core.BotState
import com.coucou.android.core.StatePalette

@Composable
internal fun StatusMark(state: BotState, modifier: Modifier, accent: Color? = null) {
    Canvas(modifier) { drawStatusMark(rgbColor(StatePalette.color(state)), accent) }
}

internal fun DrawScope.drawStatusMark(color: Color, accent: Color? = null) {
    val unit = size.minDimension
    val bar = Size(unit * 0.23f, unit * 0.085f)
    drawCircle(color.copy(alpha = 0.16f), unit * 0.37f)
    drawCircle(color, unit * 0.29f, style = Stroke(unit * 0.04f))
    drawRoundRect(
        color = accent ?: color,
        topLeft = Offset(center.x - bar.width / 2, center.y - bar.height / 2),
        size = bar,
        cornerRadius = CornerRadius(bar.height / 2),
    )
}
