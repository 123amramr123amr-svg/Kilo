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
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
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
        primary = Color(0xFFD2A972),
        onPrimary = Color(0xFF231B10),
        primaryContainer = Color(0xFF4A3720),
        onPrimaryContainer = Color(0xFFF6E9D4),
        secondary = Color(0xFFBFAE96),
        onSecondary = Color(0xFF241E15),
        secondaryContainer = Color(0xFF332B21),
        onSecondaryContainer = BrandTextPrimary,
        tertiary = Color(0xFF8FB3A4),
        onTertiary = Color(0xFF10231C),
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
 * Default look: parchment cards on a warm sand page, ink text and old-gold
 * accents — the AURA identity.
 */
@Composable
private fun LightTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Color(0xFF8A6A3F),
        onPrimary = Color(0xFFFDF8F0),
        primaryContainer = Color(0xFFEFE3D1),
        onPrimaryContainer = Color(0xFF2A2118),
        secondary = Color(0xFF6E5B45),
        onSecondary = Color(0xFFFDF8F0),
        secondaryContainer = Color(0xFFEDE4D6),
        onSecondaryContainer = Color(0xFF2C241B),
        tertiary = Color(0xFF4C6B62),
        onTertiary = Color(0xFFF7FBF8),
        tertiaryContainer = Color(0xFFDDE9E3),
        onTertiaryContainer = Color(0xFF15251F),
        background = LightBackground,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = LightSurfaceElevated,
        onSurfaceVariant = LightTextSecondary,
        surfaceContainer = LightSurfaceElevated,
        surfaceContainerHigh = Color(0xFFD9CFBF),
        outline = LightOutline,
        outlineVariant = LightOutline.copy(alpha = 0.7f),
        error = Color(0xFFA63A2B),
        onError = Color(0xFFFFF6F2),
        errorContainer = Color(0xFFF6E1DB),
        onErrorContainer = Color(0xFF5A1509),
        scrim = Color.Black,
        inverseSurface = LightTextPrimary,
        inverseOnSurface = Color.White,
    )

    val palette = BrandPalette(
        accentGradient = LightGradient,
        accentGradientSoft = LightGradientSoft,
        backdrop = LightBackdrop,
        success = Color(0xFF4C7F5A),
        warning = Color(0xFFA9762A),
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
