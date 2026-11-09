package com.personalaiagent.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    if (themeMode == ThemeMode.LIGHT) {
        LightTheme(content)
        return
    }
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

/**
 * Default look: white cards on a very light lavender page, ink text and soft
 * blue/purple accents. Large radii and low-contrast borders keep it modern
 * without shouting.
 */
@Composable
private fun LightTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Color(0xFF3F5BF6),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8ECFF),
        onPrimaryContainer = Color(0xFF16203F),
        secondary = Color(0xFF7C5CFF),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF0EBFF),
        onSecondaryContainer = Color(0xFF241A46),
        tertiary = Color(0xFF0E9AAE),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE0F7FA),
        onTertiaryContainer = Color(0xFF04333A),
        background = LightBackground,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = LightSurfaceElevated,
        onSurfaceVariant = LightTextSecondary,
        surfaceContainer = LightSurfaceElevated,
        surfaceContainerHigh = Color(0xFFE9EDF8),
        outline = LightOutline,
        outlineVariant = LightOutline.copy(alpha = 0.7f),
        error = Color(0xFFDC2B2B),
        onError = Color.White,
        errorContainer = Color(0xFFFFECEC),
        onErrorContainer = Color(0xFF5A1010),
        scrim = Color.Black,
        inverseSurface = LightTextPrimary,
        inverseOnSurface = Color.White,
    )

    val palette = BrandPalette(
        accentGradient = LightGradient,
        accentGradientSoft = LightGradientSoft,
        backdrop = LightBackdrop,
        success = Color(0xFF12855A),
        warning = Color(0xFFB4690E),
        cardBorder = LightOutline,
        bubbleUser = LightBubbleUser,
        bubbleAssistant = LightBubbleAssistant,
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
