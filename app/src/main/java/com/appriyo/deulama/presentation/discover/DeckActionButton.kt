package com.appriyo.deulama.presentation.discover

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appriyo.deulama.domain.repository.FavoritesRepository
import com.appriyo.deulama.domain.repository.WatchLaterRepository
import com.appriyo.deulama.domain.repository.WatchedRepository
import com.appriyo.deulama.ui.theme.HangugColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.koinInject

/**
 * Pill row of 5 action buttons that share the same "fly the card
 * off-screen" path as a real swipe. Keeping these visual mirrors of
 * the deck gesture (so users learn the shortcut).
 *
 * Wire order (left to right):
 *   Dislike · Favorite · Skip · Watch Later · Watched · Like
 * (Dislike on the far left, Like on the far right — same direction
 *  language as the card itself.)
 *
 * `activeDramaId` is used to bind the Favorite / Watch Later / Watched
 * buttons to the persisted Room state. Pass `null` to disable state
 * binding (the buttons still fire actions but don't reflect server
 * state).
 */
@Composable
fun DeckActionRow(
    onAction: (DeckAction) -> Unit,
    activeDramaId: Int? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    favoritesRepository: FavoritesRepository = koinInject(),
    watchLaterRepository: WatchLaterRepository = koinInject(),
    watchedRepository: WatchedRepository = koinInject(),
) {
    val isFavFlow: Flow<Boolean> = activeDramaId?.let { favoritesRepository.isFavorited(it) }
        ?: flowOf(false)
    val isQueuedFlow: Flow<Boolean> = activeDramaId?.let { watchLaterRepository.isQueued(it) }
        ?: flowOf(false)
    val isWatchedFlow: Flow<Boolean> = activeDramaId?.let { watchedRepository.isMarkedWatched(it) }
        ?: flowOf(false)

    val isFav by isFavFlow.collectAsStateWithLifecycle(initialValue = false)
    val isQueued by isQueuedFlow.collectAsStateWithLifecycle(initialValue = false)
    val isWatched by isWatchedFlow.collectAsStateWithLifecycle(initialValue = false)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        ActionCircle(
            icon = Icons.Filled.Close,
            tint = HangugColors.Danger,
            background = HangugColors.SurfaceContainer,
            contentDescription = "Dislike",
            onClick = { onAction(DeckAction.Dislike) },
            enabled = enabled,
        )
        ActionCircle(
            icon = if (isQueued) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            tint = HangugColors.Secondary,
            background = HangugColors.SurfaceContainer,
            contentDescription = if (isQueued) "Remove from watch later" else "Watch later",
            onClick = { onAction(DeckAction.WatchLater) },
            enabled = enabled,
        )
        // Centre button is a slightly larger "skip" affordance that
        // also maps to Dislike — gives the row visual balance.
        ActionCircle(
            icon = Icons.Filled.Close,
            tint = HangugColors.TextSecondary,
            background = HangugColors.SurfaceContainerHigh,
            contentDescription = "Skip",
            onClick = { onAction(DeckAction.Dislike) },
            enabled = enabled,
            size = 48.dp,
        )
        ActionCircle(
            icon = if (isWatched) Icons.Filled.CheckCircle else Icons.Filled.Check,
            tint = if (isWatched) HangugColors.Tertiary else HangugColors.Secondary,
            background = HangugColors.SurfaceContainer,
            contentDescription = if (isWatched) "Already watched" else "Watched",
            onClick = { onAction(DeckAction.Watched) },
            enabled = enabled && !isWatched, // no un-watch affordance
        )
        ActionCircle(
            icon = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            tint = HangugColors.Primary,
            background = HangugColors.SurfaceContainer,
            contentDescription = if (isFav) "Remove from favorites" else "Favorite",
            onClick = { onAction(DeckAction.Favorite) },
            enabled = enabled,
        )
        ActionCircle(
            icon = Icons.Filled.Favorite,
            tint = HangugColors.Tertiary,
            background = HangugColors.SurfaceContainer,
            contentDescription = "Like",
            onClick = { onAction(DeckAction.Like) },
            enabled = enabled,
        )
    }
}

@Composable
private fun ActionCircle(
    icon: ImageVector,
    tint: Color,
    background: Color,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    size: androidx.compose.ui.unit.Dp = 52.dp,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint,
            disabledContentColor = tint.copy(alpha = 0.3f),
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}
