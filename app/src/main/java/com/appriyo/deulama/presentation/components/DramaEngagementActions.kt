package com.appriyo.deulama.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appriyo.deulama.domain.repository.FavoritesRepository
import com.appriyo.deulama.domain.repository.WatchLaterRepository
import com.appriyo.deulama.domain.repository.WatchedRepository
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.compose.koinInject

/**
 * Tappable pill row of the three persistent engagement actions
 * (Favorite, Watch Later, Watched). Read state is bound to the
 * repositories' Room Flows so the icons update instantly when the
 * user taps, including after a sync-on-login replay.
 *
 * Visual treatment mirrors the web app's rose/gold/mint accent split:
 * each action gets its own soft tinted circle that brightens when
 * active, instead of one flat neutral background for all three, so
 * the state is legible at a glance.
 *
 * Used in:
 *   - DramaDetailsScreen — floating engagement card below the content.
 *   - (DiscoverScreen continues to use the existing `DeckActionRow`
 *     so the swipe-deck gestures stay mapped to the same buttons.)
 *
 * Note: callers are responsible for triggering the actual repository
 * call in a coroutine. Each callback is fire-and-forget — the optimistic
 * UI already happens via the Room Flow, so a slow API call won't block
 * the click.
 */
@Composable
fun DramaEngagementActions(
    dramaId: Int,
    onFavoriteToggle: () -> Unit,
    onWatchLaterToggle: () -> Unit,
    onMarkWatched: () -> Unit,
    favoritesRepository: FavoritesRepository = koinInject(),
    watchLaterRepository: WatchLaterRepository = koinInject(),
    watchedRepository: WatchedRepository = koinInject(),
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val isFav by favoritesRepository.isFavorited(dramaId)
        .collectAsStateWithLifecycle(initialValue = false)
    val isQueued by watchLaterRepository.isQueued(dramaId)
        .collectAsStateWithLifecycle(initialValue = false)
    val isWatched by watchedRepository.isMarkedWatched(dramaId)
        .collectAsStateWithLifecycle(initialValue = false)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionCircle(
            icon = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            tint = HangugColors.Primary,
            background = if (isFav) HangugColors.Primary.copy(alpha = 0.18f) else HangugColors.SurfaceContainerHigh,
            label = "Like",
            active = isFav,
            contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
            onClick = onFavoriteToggle,
            enabled = enabled,
        )
        ActionCircle(
            icon = if (isQueued) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            tint = HangugColors.Secondary,
            background = if (isQueued) HangugColors.Secondary.copy(alpha = 0.18f) else HangugColors.SurfaceContainerHigh,
            label = "Save",
            active = isQueued,
            contentDescription = if (isQueued) "Remove from watch later" else "Add to watch later",
            onClick = onWatchLaterToggle,
            enabled = enabled,
        )
        ActionCircle(
            icon = if (isWatched) Icons.Filled.CheckCircle else Icons.Filled.Check,
            tint = HangugColors.Tertiary,
            background = if (isWatched) HangugColors.Tertiary.copy(alpha = 0.18f) else HangugColors.SurfaceContainerHigh,
            label = if (isWatched) "Watched" else "Mark watched",
            active = isWatched,
            contentDescription = if (isWatched) "Already watched" else "Mark as watched",
            onClick = onMarkWatched,
            enabled = enabled && !isWatched, // no un-watch affordance
        )
    }
}

@Composable
private fun ActionCircle(
    icon: ImageVector,
    tint: Color,
    background: Color,
    label: String,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    size: androidx.compose.ui.unit.Dp = 52.dp,
) {
    val animatedBg by animateColorAsState(
        targetValue = background,
        animationSpec = tween(220),
        label = "actionCircleBg",
    )

    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(animatedBg),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = tint,
                disabledContentColor = tint.copy(alpha = 0.35f),
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else tint.copy(alpha = 0.35f),
                modifier = Modifier.size(22.dp),
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) tint else HangugColors.TextTertiary,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}