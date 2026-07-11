package com.appriyo.deulama.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val HangugShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),  // --radius-default (0.25rem)
    small = RoundedCornerShape(8.dp),        // --radius-lg (0.5rem)
    medium = RoundedCornerShape(12.dp),      // --radius-xl (0.75rem)
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(999.dp), // --radius-full — chips, pills, avatar
)