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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personalaiagent.ui.theme.BrandBlue
import com.personalaiagent.ui.theme.BrandCyan
import com.personalaiagent.ui.theme.BrandPurple
import com.personalaiagent.ui.theme.PersonalAiAgentTheme

/**
 * The app's mascot: a glowing AI orb with a friendly robot face.
 *
 * Drawn with the Compose canvas (no bitmaps, no image assets) so it scales to any
 * phone or tablet size and animates cheaply.
 */
@Composable
fun AgentOrb(
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    listening: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(if (listening) 750 else 2200),
            RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val faceRadius = radius * 0.56f * pulse

        // Outer halo.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BrandPurple.copy(alpha = 0.30f), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )

        // Rotating gradient ring.
        rotate(rotation, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(listOf(BrandBlue, BrandPurple, BrandCyan, BrandBlue)),
                startAngle = 0f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.92f, center.y - radius * 0.92f),
                size = Size(radius * 1.84f, radius * 1.84f),
                style = Stroke(width = radius * 0.075f, cap = StrokeCap.Round),
            )
        }

        // Dark face plate.
        drawCircle(color = Color(0xFF060B16), radius = faceRadius, center = center)
        drawCircle(
            brush = Brush.linearGradient(listOf(BrandBlue, BrandPurple, BrandCyan)),
            radius = faceRadius,
            center = center,
            style = Stroke(width = radius * 0.045f),
        )

        // Eyes.
        val eyeOffset = faceRadius * 0.42f
        val eyeRadius = faceRadius * 0.145f
        val eyeY = center.y - faceRadius * 0.12f
        drawCircle(BrandCyan, eyeRadius, Offset(center.x - eyeOffset, eyeY))
        drawCircle(BrandCyan, eyeRadius, Offset(center.x + eyeOffset, eyeY))

        // Smile.
        val smileSize = faceRadius * 0.86f
        drawArc(
            color = BrandCyan.copy(alpha = 0.85f),
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - smileSize / 2f, center.y - smileSize * 0.32f),
            size = Size(smileSize, smileSize * 0.70f),
            style = Stroke(width = radius * 0.035f, cap = StrokeCap.Round),
        )
    }
}

@Preview
@Composable
private fun AgentOrbPreview() {
    PersonalAiAgentTheme {
        AgentOrb(size = 160.dp)
    }
}
