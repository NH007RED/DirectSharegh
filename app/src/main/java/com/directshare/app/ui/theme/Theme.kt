package com.directshare.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.directshare.app.domain.model.ThemePreference

private val CrimsonObsidianColorScheme = darkColorScheme(
    primary = CrimsonRed,
    onPrimary = Color.White,
    primaryContainer = ScarletRed,
    onPrimaryContainer = TextPrimary,
    secondary = DeepRuby,
    onSecondary = TextPrimary,
    secondaryContainer = CarbonElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = RoseNeon,
    onTertiary = Color.White,
    background = DeepBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassCardTop,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderCrimson,
    outlineVariant = GlassBorderSubtle,
    error = CoralError,
    onError = Color.White
)

@Composable
fun DirectShareTheme(
    @Suppress("UNUSED_PARAMETER") themePreference: ThemePreference = ThemePreference.DARK,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CrimsonObsidianColorScheme,
        typography = DirectShareTypography,
        content = content
    )
}
