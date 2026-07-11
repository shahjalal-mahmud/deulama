package com.appriyo.deulama.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Direct port of the web app's CSS custom-property token set (see
 * global.css `@theme` block). Every color anywhere in the app should
 * trace back to one of these — if you find yourself reaching for a raw
 * hex mid-screen, add the token here first instead.
 */
object HangugColors {
    // Background ramp — monotonic, matches --color-bg-base/elevated/elevated-2
    val BgBase = Color(0xFF0B0708)
    val BgElevated = Color(0xFF150F10)
    val BgElevated2 = Color(0xFF1E1516)

    // Surface ramp
    val Surface = Color(0xFF1C1011)
    val SurfaceDim = Color(0xFF170B0C)
    val SurfaceBright = Color(0xFF453536)
    val SurfaceContainerLowest = Color(0xFF120A0A)
    val SurfaceContainerLow = Color(0xFF211516)
    val SurfaceContainer = Color(0xFF2A1C1D)
    val SurfaceContainerHigh = Color(0xFF352627)
    val SurfaceContainerHighest = Color(0xFF403132)

    // Primary — rose
    val Primary = Color(0xFFFFB2B7)
    val PrimaryContainer = Color(0xFFF55C6F)
    val OnPrimary = Color(0xFF67001B)
    val OnPrimaryContainer = Color(0xFF5B0017)
    val InversePrimary = Color(0xFFB12941)

    // Secondary — gold
    val Secondary = Color(0xFFF1BF65)
    val SecondaryContainer = Color(0xFF7D5800)
    val OnSecondary = Color(0xFF422D00)
    val OnSecondaryContainer = Color(0xFFFFD284)

    // Tertiary — mint (watched / success accents)
    val Tertiary = Color(0xFF6EDBA7)
    val TertiaryContainer = Color(0xFF30A374)
    val OnTertiary = Color(0xFF003824)

    // Status
    val Error = Color(0xFFFFB4AB)
    val ErrorContainer = Color(0xFF93000A)
    val Success = Color(0xFF4ADE80)
    val Danger = Color(0xFFF45B69) // dislike-swipe tint

    // Text — single source of truth, same as the web's derived-alias approach
    val TextPrimary = Color(0xFFF7EDEE)
    val TextSecondary = Color(0xFFB39B9C)
    val TextTertiary = Color(0xFF8A7375)

    // Borders / outline
    val Outline = Color(0xFFA7898B)
    val OutlineVariant = Color(0xFF594142)
    val BorderSubtle = Color(0x40403132) // ~0.25 alpha of surface-container-highest
    val BorderStrong = Color(0xA6594142) // ~0.65 alpha
}

val HangugDarkColorScheme = darkColorScheme(
    primary = HangugColors.Primary,
    onPrimary = HangugColors.OnPrimary,
    primaryContainer = HangugColors.PrimaryContainer,
    onPrimaryContainer = HangugColors.TextPrimary,
    inversePrimary = HangugColors.InversePrimary,
    secondary = HangugColors.Secondary,
    onSecondary = HangugColors.OnSecondary,
    secondaryContainer = HangugColors.SecondaryContainer,
    onSecondaryContainer = HangugColors.OnSecondaryContainer,
    tertiary = HangugColors.Tertiary,
    onTertiary = HangugColors.OnTertiary,
    tertiaryContainer = HangugColors.TertiaryContainer,
    background = HangugColors.BgBase,
    onBackground = HangugColors.TextPrimary,
    surface = HangugColors.Surface,
    onSurface = HangugColors.TextPrimary,
    surfaceVariant = HangugColors.SurfaceContainerHighest,
    onSurfaceVariant = HangugColors.TextSecondary,
    surfaceDim = HangugColors.SurfaceDim,
    surfaceBright = HangugColors.SurfaceBright,
    surfaceContainerLowest = HangugColors.SurfaceContainerLowest,
    surfaceContainerLow = HangugColors.SurfaceContainerLow,
    surfaceContainer = HangugColors.SurfaceContainer,
    surfaceContainerHigh = HangugColors.SurfaceContainerHigh,
    surfaceContainerHighest = HangugColors.SurfaceContainerHighest,
    error = HangugColors.Error,
    onError = HangugColors.OnPrimary,
    errorContainer = HangugColors.ErrorContainer,
    outline = HangugColors.Outline,
    outlineVariant = HangugColors.OutlineVariant,
)