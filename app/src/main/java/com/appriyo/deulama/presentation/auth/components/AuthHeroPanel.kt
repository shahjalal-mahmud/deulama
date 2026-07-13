package com.appriyo.deulama.presentation.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appriyo.deulama.ui.theme.HangugColors
import com.appriyo.deulama.ui.theme.HangugGlassOverlay

/**
 * Bleed-to-edge banner standing in for the web's side-by-side collage
 * — mobile doesn't have room for a second column, so the cinematic
 * mood moves to the top and the form slides up over it in a rounded
 * sheet (see the sheet styling in LoginScreen / RegisterScreen).
 *
 * Visual pass: the flat brand-glyph watermark is joined by two soft
 * ambient glow blobs (radial gradients) for a more cinematic, premium
 * feel, and the eyebrow now sits in a pill badge instead of bare text.
 * Same public API — eyebrow, headline, modifier, height.
 */
@Composable
fun AuthHeroPanel(
    eyebrow: String,
    headline: String,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(HangugColors.BgElevated)
            .background(HangugGlassOverlay),
    ) {
        // Ambient light source, top-right.
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HangugColors.Primary.copy(alpha = 0.28f),
                            HangugColors.Primary.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
        // Secondary tint glow, bottom-left, for depth.
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HangugColors.Secondary.copy(alpha = 0.18f),
                            HangugColors.Secondary.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        // Oversized brand glyph watermark, echoing the "한" mark used
        // elsewhere in the app — faint, top-right, purely decorative.
        Text(
            text = "한",
            fontSize = 200.sp,
            fontWeight = FontWeight.Bold,
            color = HangugColors.TextPrimary.copy(alpha = 0.05f),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 4.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 40.dp),
        ) {
            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(HangugColors.Primary.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = HangugColors.Secondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.size(14.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = HangugColors.TextPrimary,
                lineHeight = 38.sp,
            )
        }
    }
}