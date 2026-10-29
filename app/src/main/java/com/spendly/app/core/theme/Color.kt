package com.spendly.app.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Primary Brand Colors
val ElectricLime = Color(0xFFC8FF5A)
val ElectricLimeHover = Color(0xFFB5F040)
val ElectricLimeDim = Color(0x33C8FF5A)

// Dark Palette (From Reference: Deep Navy)
val DarkNavyBackground = Color(0xFF08152E)
val DarkNavySurface = Color(0xFF102344)
val DarkNavyCard = Color(0xFF142B52)
val DarkNavyBorder = Color(0xFF1F3B6E)
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFF8E9EB5)
val DarkTextMuted = Color(0xFF5A6E8C)

// Light Palette (From Reference: Warm Off-White)
val LightWarmBackground = Color(0xFFF8F7F2)
val LightWarmSurface = Color(0xFFFFFFFF)
val LightWarmCard = Color(0xFFFFFFFF)
val LightWarmBorder = Color(0xFFE9E7DF)
val LightTextPrimary = Color(0xFF09142A)
val LightTextSecondary = Color(0xFF64748B)
val LightTextMuted = Color(0xFF94A3B8)

// Semantic Category Colors
val CategoryOrangeFood = Color(0xFFFF7A00)
val CategoryPurpleShopping = Color(0xFF7C5CFC)
val CategoryBlueTravel = Color(0xFF2979FF)
val CategoryGreenBills = Color(0xFF00C853)
val CategoryPinkEntertainment = Color(0xFFE040FB)
val CategorySlateOthers = Color(0xFF546E7A)
val CategoryTealInvestment = Color(0xFF00B0FF)
val CategoryEmeraldSalary = Color(0xFF00E676)

// Financial Status Colors
val StatusIncomeGreen = Color(0xFF00C853)
val StatusExpenseRed = Color(0xFFFF5252)
val StatusWarningAmber = Color(0xFFFFAB00)

@Immutable
data class SpendlyColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primaryAccent: Color,
    val primaryAccentHover: Color,
    val primaryAccentDim: Color,
    val isDark: Boolean
)

val DarkColorPalette = SpendlyColors(
    background = DarkNavyBackground,
    surface = DarkNavySurface,
    card = DarkNavyCard,
    border = DarkNavyBorder,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    primaryAccent = ElectricLime,
    primaryAccentHover = ElectricLimeHover,
    primaryAccentDim = ElectricLimeDim,
    isDark = true
)

val LightColorPalette = SpendlyColors(
    background = LightWarmBackground,
    surface = LightWarmSurface,
    card = LightWarmCard,
    border = LightWarmBorder,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    primaryAccent = Color(0xFF0B1936), // High contrast dark navy for light theme buttons or lime
    primaryAccentHover = Color(0xFF142B52),
    primaryAccentDim = Color(0x1F0B1936),
    isDark = false
)

val LocalSpendlyColors = staticCompositionLocalOf { DarkColorPalette }
