package com.personalaiagent.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personalaiagent.ui.theme.BrandBlue
import com.personalaiagent.ui.theme.BrandCyan

/**
 * The AURA mark: a pyramid with its sun and horizon, drawn on the canvas so it
 * stays crisp at any size and needs no image asset.
 */
@Composable
fun AuraMark(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    animated: Boolean = false,
) {
    val gold = BrandBlue // AURA gold
    val glow = BrandCyan

    val pulse = if (animated) {
        val transition = rememberInfiniteTransition(label = "aura-mark")
        val value by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2_200, easing = LinearEasing), RepeatMode.Reverse),
            label = "aura-glow",
        )
        value
    } else {
        0.6f
    }

    Canvas(modifier = modifier.then(Modifier.size(size))) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f

        // Sun behind the apex.
        drawCircle(
            color = gold.copy(alpha = 0.28f * pulse + 0.12f),
            radius = w * 0.17f,
            center = Offset(cx, h * 0.18f),
        )
        drawCircle(color = gold, radius = w * 0.075f, center = Offset(cx, h * 0.18f))

        // Lit face.
        val apex = Offset(cx, h * 0.34f)
        val leftBase = Offset(w * 0.16f, h * 0.78f)
        val rightBase = Offset(w * 0.84f, h * 0.78f)
        drawPath(
            path = Path().apply {
                moveTo(apex.x, apex.y)
                lineTo(leftBase.x, leftBase.y)
                lineTo(cx, leftBase.y)
                close()
            },
            color = gold,
        )
        // Shadow face.
        drawPath(
            path = Path().apply {
                moveTo(apex.x, apex.y)
                lineTo(cx, rightBase.y)
                lineTo(rightBase.x, rightBase.y)
                close()
            },
            color = gold.copy(alpha = 0.55f),
        )
        // Rim light on the lit edge.
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = apex,
            end = leftBase,
            strokeWidth = w * 0.018f,
        )
        // Horizon.
        drawLine(
            color = glow.copy(alpha = 0.55f * pulse + 0.2f),
            start = Offset(w * 0.1f, h * 0.84f),
            end = Offset(w * 0.9f, h * 0.84f),
            strokeWidth = w * 0.022f,
        )
        drawCircle(
            color = glow.copy(alpha = 0.5f),
            radius = w * 0.03f,
            center = Offset(w * 0.5f, h * 0.92f),
            style = Stroke(width = w * 0.018f),
        )
    }
}
