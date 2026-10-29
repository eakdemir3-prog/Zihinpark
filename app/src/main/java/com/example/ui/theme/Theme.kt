package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
private fun schemeFor(p: ZpPalette) = if (p.isLight) lightColorScheme(
    primary = NeonCyan, onPrimary = OnNeon, primaryContainer = NeonCyanDim, onPrimaryContainer = NeonCyan,
    secondary = NeonPink, onSecondary = OnNeon, secondaryContainer = NeonPinkDim, onSecondaryContainer = NeonPink,
    tertiary = NeonPurple, onTertiary = OnNeon, tertiaryContainer = PastelLavender, onTertiaryContainer = TextPrimary,
    error = NeonRed, onError = OnNeon, background = NeonBg, onBackground = TextPrimary,
    surface = NeonSurface, onSurface = TextPrimary, surfaceVariant = NeonCard, onSurfaceVariant = TextSecondary,
    surfaceContainer = NeonCard, surfaceContainerHigh = NeonCardHigh, surfaceContainerHighest = NeonCardHigh,
    surfaceContainerLow = NeonSurface, surfaceContainerLowest = NeonBgDeep, outline = NeonBorder, outlineVariant = NeonBorder,
    inverseSurface = TextPrimary, inverseOnSurface = NeonBg
) else darkColorScheme(
    primary = NeonCyan,
    onPrimary = OnNeon,
    primaryContainer = NeonCyanDim,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPink,
    onSecondary = OnNeon,
    secondaryContainer = NeonPinkDim,
    onSecondaryContainer = NeonPink,
    tertiary = NeonPurple,
    onTertiary = TextPrimary,
    tertiaryContainer = PastelLavender,
    onTertiaryContainer = TextPrimary,
    error = NeonRed,
    onError = OnNeon,
    background = NeonBg,
    onBackground = TextPrimary,
    surface = NeonSurface,
    onSurface = TextPrimary,
    surfaceVariant = NeonCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = NeonCard,
    surfaceContainerHigh = NeonCardHigh,
    surfaceContainerHighest = NeonCardHigh,
    surfaceContainerLow = NeonSurface,
    surfaceContainerLowest = NeonBgDeep,
    outline = NeonBorder,
    outlineVariant = NeonBorder,
    inverseSurface = TextPrimary,
    inverseOnSurface = NeonBg
)

/**
 * ZihinPark her zaman neon koyu temayla çalışır.
 * [darkTheme] parametresi eski çağrılar bozulmasın diye korunur.
 */
@Composable
fun ZihinParkTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = ZpColors.palette.isLight
            controller.isAppearanceLightNavigationBars = ZpColors.palette.isLight
        }
    }
    MaterialTheme(
        colorScheme = schemeFor(ZpColors.palette),
        typography = Typography,
        content = content
    )
}
