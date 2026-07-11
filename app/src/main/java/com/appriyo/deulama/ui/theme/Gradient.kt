package com.appriyo.deulama.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Direct port of `.btn-gradient` from global.css: a 135° linear
 * gradient rose -> deep rose -> gold. Reuse this everywhere the web
 * uses `.btn-gradient` (primary CTAs, match-score badges, gradient
 * icon buttons) so the two clients stay visually identical.
 */
val HangugBrandGradient = Brush.linearGradient(
    colors = listOf(
        HangugColors.Primary,
        HangugColors.PrimaryContainer,
        HangugColors.Secondary,
    ),
)

/** `.btn-gradient-subtle` equivalent — same hues, much lower opacity. */
val HangugBrandGradientSubtle = Brush.linearGradient(
    colors = listOf(
        HangugColors.Primary.copy(alpha = 0.15f),
        HangugColors.PrimaryContainer.copy(alpha = 0.15f),
        HangugColors.Secondary.copy(alpha = 0.15f),
    ),
)

/**
 * `.glass-overlay` equivalent — diagonal rose-black -> near-black
 * scrim used over hero/backdrop images so text stays legible over
 * any poster art.
 */
val HangugGlassOverlay = Brush.linearGradient(
    colors = listOf(
        Color(0xCC67001B), // rgba(103, 0, 27, 0.8)
        Color(0xF2150F10), // rgba(21, 15, 16, 0.95)
    ),
)