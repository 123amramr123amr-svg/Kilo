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

val BrandGradient = Brush.linearGradient(listOf(BrandBlue, BrandPurple, BrandCyan))
val BrandGradientSoft = Brush.linearGradient(
    listOf(BrandBlue.copy(alpha = 0.22f), BrandPurple.copy(alpha = 0.16f)),
)
val BackdropGradient = Brush.verticalGradient(
    listOf(Color(0xFF0A1428), Color(0xFF05070E), Color(0xFF080611)),
)
