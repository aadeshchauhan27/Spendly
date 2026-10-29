package com.spendly.app.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val DarkMaterialColorScheme = darkColorScheme(
    primary = ElectricLime,
    onPrimary = DarkNavyBackground,
    primaryContainer = DarkNavyCard,
    onPrimaryContainer = DarkTextPrimary,
    background = DarkNavyBackground,
    onBackground = DarkTextPrimary,
    surface = DarkNavySurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkNavyCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkNavyBorder
)

private val LightMaterialColorScheme = lightColorScheme(
    primary = Color(0xFF09142A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0EFE9),
    onPrimaryContainer = LightTextPrimary,
    background = LightWarmBackground,
    onBackground = LightTextPrimary,
    surface = LightWarmSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF1EFEA),
    onSurfaceVariant = LightTextSecondary,
    outline = LightWarmBorder
)

@Composable
fun SpendlyTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val spendlyColors = if (isDark) DarkColorPalette else LightColorPalette
    val materialColorScheme = if (isDark) DarkMaterialColorScheme else LightMaterialColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = spendlyColors.background.toArgb()
            window.navigationBarColor = spendlyColors.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalSpendlyColors provides spendlyColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = SpendlyTypography,
            shapes = SpendlyShapes,
            content = content
        )
    }
}

object SpendlyTheme {
    val colors: SpendlyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSpendlyColors.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes
}
