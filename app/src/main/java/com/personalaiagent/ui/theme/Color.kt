package com.personalaiagent.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand palette shared by both dark variants.
val BrandBlue = Color(0xFF2D7BFF)
val BrandBlueDark = Color(0xFF1B4FD8)
val BrandPurple = Color(0xFF8B5CF6)
val BrandCyan = Color(0xFF22D3EE)
val BrandSuccess = Color(0xFF2ED47A)
val BrandWarning = Color(0xFFFFB020)
val BrandError = Color(0xFFFF5B5B)
val BrandTextPrimary = Color(0xFFEAF0FF)
val BrandTextSecondary = Color(0xFF8FA3C8)

// Midnight surfaces.
val MidnightBackground = Color(0xFF05070E)
val MidnightSurface = Color(0xFF0B1224)
val MidnightSurfaceElevated = Color(0xFF121C33)
val MidnightOutline = Color(0xFF22304F)

// AMOLED surfaces.
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF06080F)
val AmoledSurfaceElevated = Color(0xFF0D1220)
val AmoledOutline = Color(0xFF1B2438)

// Light surfaces (default look): white cards, soft lavender page, deep ink text.
val LightBackground = Color(0xFFF6F7FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFEFF2FB)
val LightOutline = Color(0xFFE2E7F3)
val LightTextPrimary = Color(0xFF111827)
val LightTextSecondary = Color(0xFF5D6780)
val LightBubbleAssistant = Color(0xFFF2F4FB)
val LightBubbleUserFrom = Color(0xFF4F6BFF)
val LightBubbleUserTo = Color(0xFF8B5CF6)

val BrandGradient = Brush.linearGradient(listOf(BrandBlue, BrandPurple, BrandCyan))
val BrandGradientSoft = Brush.linearGradient(
    listOf(BrandBlue.copy(alpha = 0.22f), BrandPurple.copy(alpha = 0.16f)),
)
val BackdropGradient = Brush.verticalGradient(
    listOf(Color(0xFF0A1428), Color(0xFF05070E), Color(0xFF080611)),
)

// Light-mode gradients: airy, low-contrast so white cards stay the focus.
val LightGradient = Brush.linearGradient(listOf(BrandBlue, Color(0xFF7C5CFF)))
val LightGradientSoft = Brush.linearGradient(
    listOf(Color(0xFF4F6BFF).copy(alpha = 0.10f), Color(0xFF8B5CF6).copy(alpha = 0.08f)),
)
val LightBackdrop = Brush.verticalGradient(
    listOf(Color(0xFFF8F9FE), Color(0xFFF3F5FD), Color(0xFFF7F4FE)),
)
val LightBubbleUser = Brush.linearGradient(listOf(LightBubbleUserFrom, LightBubbleUserTo))
