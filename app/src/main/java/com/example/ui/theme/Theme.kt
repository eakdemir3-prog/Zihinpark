package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryCoral,
    onPrimary = SoftCreamSurface,
    primaryContainer = PastelPeach,
    onPrimaryContainer = DarkSlateText,
    secondary = PrimaryCoralVariant,
    onSecondary = SoftCreamSurface,
    secondaryContainer = PastelLavender,
    onSecondaryContainer = DarkSlateText,
    tertiary = MintGreenDark,
    onTertiary = SoftCreamSurface,
    tertiaryContainer = MintGreen,
    onTertiaryContainer = DarkSlateText,
    background = WarmCreamBackground,
    onBackground = DarkSlateText,
    surface = SoftCreamSurface,
    onSurface = DarkSlateText,
    surfaceVariant = LavenderCard,
    onSurfaceVariant = DarkSlateText,
    outline = LightGrayBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkCoralPrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkCardBg,
    onPrimaryContainer = DarkTextPrimary,
    secondary = AccentSalmon,
    onSecondary = DarkBackground,
    secondaryContainer = DarkCardBg,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = MintGreen,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkCardBg,
    onTertiaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardBg,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

@Composable
fun ZihinParkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
