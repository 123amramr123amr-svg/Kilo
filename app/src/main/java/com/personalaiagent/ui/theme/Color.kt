package com.personalaiagent.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// AURA palette — sand, ink and old gold. The identity borrows from Ancient Egypt:
// warm stone backgrounds, a deep ink foreground and a single gold accent.
val BrandBlue = Color(0xFFB08A5D) // gold (kept name: used widely as the accent)
val BrandBlueDark = Color(0xFF8A6A3F)
val BrandPurple = Color(0xFF7A6A55) // warm taupe
val BrandCyan = Color(0xFF4C7A6B) // nile teal
val BrandSuccess = Color(0xFF4C7F5A)
val BrandWarning = Color(0xFFC08A2E)
val BrandError = Color(0xFFA63A2B)
val BrandTextPrimary = Color(0xFFF3ECE2)
val BrandTextSecondary = Color(0xFFA79C8C)

// Dark surfaces: burnt umber and stone, never blue-black.
val MidnightBackground = Color(0xFF121010)
val MidnightSurface = Color(0xFF1A1714)
val MidnightSurfaceElevated = Color(0xFF24201A)
val MidnightOutline = Color(0xFF3B342A)

// AMOLED surfaces (true black, warm ink).
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF0C0B09)
val AmoledSurfaceElevated = Color(0xFF171410)
val AmoledOutline = Color(0xFF2A2519)

// Light surfaces (default look): warm sand page, parchment cards, ink text.
val LightBackground = Color(0xFFE8E0D5)
val LightSurface = Color(0xFFF5F0EB)
val LightSurfaceElevated = Color(0xFFDFD6C7)
val LightOutline = Color(0xFFC9BDAC)
val LightTextPrimary = Color(0xFF1C1814)
val LightTextSecondary = Color(0xFF5A5248)
val LightBubbleAssistant = Color(0xFFF7F2EA)
val LightBubbleUserFrom = Color(0xFF2A241C)
val LightBubbleUserTo = Color(0xFF1C1814)

val BrandGradient = Brush.linearGradient(listOf(BrandBlue, BrandBlueDark, BrandCyan))
val BrandGradientSoft = Brush.linearGradient(
    listOf(BrandBlue.copy(alpha = 0.22f), BrandCyan.copy(alpha = 0.16f)),
)
val BackdropGradient = Brush.verticalGradient(
    listOf(Color(0xFF171310), Color(0xFF0E0C0A), Color(0xFF100D0A)),
)

// Light-mode gradients: sand to parchment, gold accents.
val LightGradient = Brush.linearGradient(listOf(Color(0xFFC9A06A), Color(0xFF9C7442)))
val LightGradientSoft = Brush.linearGradient(
    listOf(Color(0xFFB08A5D).copy(alpha = 0.12f), Color(0xFF4C7A6B).copy(alpha = 0.08f)),
)
val LightBackdrop = Brush.verticalGradient(
    listOf(Color(0xFFEFE9DF), Color(0xFFE8E0D5), Color(0xFFE3D9CB)),
)
val LightBubbleUser = Brush.linearGradient(listOf(LightBubbleUserFrom, LightBubbleUserTo))
