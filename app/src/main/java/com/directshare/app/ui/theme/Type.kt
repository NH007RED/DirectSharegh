package com.directshare.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val DefaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private val BasePlatformTextStyle = PlatformTextStyle(
    includeFontPadding = false
)

/**
 * DirectShare v3 — Flagship Precision Typography System
 * Engineered with tight display tracking, clean geometric hierarchy, and zero font-padding bloat
 * so cards, telemetry grids, and action pills align with pixel-level precision across all screen densities.
 */
val DirectShareTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.35).sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.25).sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.15).sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.05.sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.5.sp,
        letterSpacing = 0.15.sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.35.sp,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = DefaultLineHeightStyle
    )
)
