package com.personalaiagent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Default = Typography()

/**
 * Type scale tuned for bilingual use: slightly larger line heights keep Arabic
 * diacritics and descenders from clipping, and Latin sizes stay compact.
 */
val AppTypography = Typography(
    displaySmall = Default.displaySmall.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = Default.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = Default.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = Default.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = Default.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = Default.bodyLarge.copy(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = Default.bodyMedium.copy(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = Default.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = Default.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp),
)
