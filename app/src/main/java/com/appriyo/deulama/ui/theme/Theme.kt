package com.appriyo.deulama.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * App-wide theme. Deliberately dark-only, matching the web app's
 * `prefersdark: true` / `color-scheme: dark` — no light theme, no
 * dynamic (Material You) color, ever.
 */
@Composable
fun HangugDeulamaTheme(
    content: @Composable () -> Unit,
) {
    // isSystemInDarkTheme() is intentionally unused for branching — kept
    // only as a reminder that this app has one theme, not a light/dark pair.
    isSystemInDarkTheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = HangugColors.BgBase.toArgb()
            window.navigationBarColor = HangugColors.BgBase.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = HangugDarkColorScheme,
        typography = HangugTypography,
        shapes = HangugShapes,
        content = content,
    )
}