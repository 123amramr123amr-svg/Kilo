package com.personalaiagent.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.personalaiagent.domain.model.ThemeMode

@Immutable
data class BrandPalette(
    val accentGradient: Brush,
    val accentGradientSoft: Brush,
    val backdrop: Brush,
    val success: Color,
    val warning: Color,
    val cardBorder: Color,
    val bubbleUser: Brush,
    val bubbleAssistant: Color,
)

val LocalBrand = staticCompositionLocalOf {
    BrandPalette(
        accentGradient = BrandGradient,
        accentGradientSoft = BrandGradientSoft,
        backdrop = BackdropGradient,
        success = BrandSuccess,
        warning = BrandWarning,
        cardBorder = MidnightOutline,
        bubbleUser = BrandGradient,
        bubbleAssistant = MidnightSurfaceElevated,
    )
}

private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun PersonalAiAgentTheme(
    themeMode: ThemeMode = ThemeMode.MIDNIGHT,
    content: @Composable () -> Unit,
) {
    val amoled = themeMode == ThemeMode.AMOLED
    val background = if (amoled) AmoledBackground else MidnightBackground
    val surface = if (amoled) AmoledSurface else MidnightSurface
    val elevated = if (amoled) AmoledSurfaceElevated else MidnightSurfaceElevated
    val outline = if (amoled) AmoledOutline else MidnightOutline

    val scheme = darkColorScheme(
        primary = BrandBlue,
        onPrimary = Color.White,
        primaryContainer = BrandBlueDark,
        onPrimaryContainer = Color.White,
        secondary = BrandPurple,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF2A1B4D),
        onSecondaryContainer = BrandTextPrimary,
        tertiary = BrandCyan,
        onTertiary = Color(0xFF04202B),
        background = background,
        onBackground = BrandTextPrimary,
        surface = surface,
        onSurface = BrandTextPrimary,
        surfaceVariant = elevated,
        onSurfaceVariant = BrandTextSecondary,
        surfaceContainer = elevated,
        surfaceContainerHigh = elevated,
        outline = outline,
        outlineVariant = outline.copy(alpha = 0.6f),
        error = BrandError,
        onError = Color.White,
        errorContainer = Color(0xFF3B1216),
        onErrorContainer = Color(0xFFFFD9D9),
        scrim = Color.Black,
        inverseSurface = BrandTextPrimary,
        inverseOnSurface = background,
    )

    val palette = BrandPalette(
        accentGradient = BrandGradient,
        accentGradientSoft = BrandGradientSoft,
        backdrop = if (amoled) Brush.verticalGradient(listOf(Color.Black, Color.Black)) else BackdropGradient,
        success = BrandSuccess,
        warning = BrandWarning,
        cardBorder = outline,
        bubbleUser = BrandGradient,
        bubbleAssistant = elevated,
    )

    CompositionLocalProvider(LocalBrand provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = Shapes,
            content = content,
        )
    }
}
