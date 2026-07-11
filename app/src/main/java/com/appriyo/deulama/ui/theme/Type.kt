package com.appriyo.deulama.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * TODO: once you drop the real variable font files into res/font/
 * (e.g. sora_variable.ttf, inter_variable.ttf), swap these two lines
 * for:
 *
 *   val Sora = FontFamily(Font(R.font.sora_variable))
 *   val Inter = FontFamily(Font(R.font.inter_variable))
 *
 * Everything else below references these two vals, so nothing else
 * needs to change.
 */
val Sora: FontFamily = FontFamily.SansSerif
val Inter: FontFamily = FontFamily.Default

// Doesn't map to a standard Material role — referenced directly by the
// Home hero composable.
val HeroTitle = TextStyle(
    fontFamily = Sora,
    fontWeight = FontWeight.SemiBold,
    fontSize = 72.sp,
    letterSpacing = (-0.01).em,
)

val HeroTitleCompact = TextStyle(
    fontFamily = Sora,
    fontWeight = FontWeight.SemiBold,
    fontSize = 48.sp,
    letterSpacing = (-0.01).em,
)

val HangugTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        letterSpacing = (-0.01).em,
    ), // --font-h1
    headlineMedium = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
    ), // --font-h2
    titleMedium = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
    ), // --font-card-title
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontSize = 16.sp,
    ), // --font-body-md
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontSize = 14.sp,
    ), // --font-body-sm
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontSize = 13.sp,
        letterSpacing = 0.08.em,
    ), // --font-metadata / eyebrow
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ), // --font-button
)