package com.appriyo.deulama.presentation.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.ui.theme.HangugColors

/**
 * First-launch coach mark for the swipe deck. Renders only while
 * [visible] is true; tapping it persists the dismissal flag via the
 * caller. Reuses the same Tertiary / Danger colours as the like /
 * dislike stamps so the colour-to-action mapping is already
 * in the user's head.
 */
@Composable
fun SwipeCoachMark(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            HangugColors.Danger.copy(alpha = 0.18f),
                            HangugColors.SurfaceContainerHigh,
                            HangugColors.Tertiary.copy(alpha = 0.18f),
                        ),
                    ),
                )
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallSwatch(color = HangugColors.Danger, icon = Icons.Filled.Close)
                    Text(
                        text = "Swipe left to skip",
                        style = MaterialTheme.typography.labelMedium,
                        color = HangugColors.TextPrimary,
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    SmallSwatch(color = HangugColors.Tertiary, icon = Icons.Filled.Favorite)
                    Text(
                        text = "Swipe right to like",
                        style = MaterialTheme.typography.labelMedium,
                        color = HangugColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = HangugColors.Secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "Tap the badge to dismiss — this won't show again.",
                        style = MaterialTheme.typography.labelSmall,
                        color = HangugColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallSwatch(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp),
        )
    }
}
