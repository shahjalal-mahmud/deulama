package com.appriyo.deulama.presentation.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.koinInject

/**
 * Floating pill of the 3 *persisted* deck actions — Watch Later,
 * Favorite, Watched. Like / Dislike are deliberately NOT buttons here:
 * they only fire from the swipe gesture on the card itself, matching
 * dating-app conventions and keeping this row free of ambiguity about
 * which interaction does what.
 *
 * `activeDramaId` binds each button to its persisted Room state so the
 * icon + label reflect saved status. Pass `null` to disable binding
 * (buttons still fire actions but won't show filled/active state).
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = HangugColors.SurfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(
                icon = if (isQueued) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                label = if (isQueued) "Queued" else "Later",
                active = isQueued,
                tint = HangugColors.Secondary,
                contentDescription = if (isQueued) "Remove from watch later" else "Save to watch later",
                onClick = { onAction(DeckAction.WatchLater) },
                enabled = enabled,
            )
            ActionButton(
                icon = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                label = "Favorite",
                active = isFav,
                tint = HangugColors.Primary,
                contentDescription = if (isFav) "Remove from favorites" else "Save to favorites",
                onClick = { onAction(DeckAction.Favorite) },
                enabled = enabled,
            )
            ActionButton(
                icon = if (isWatched) Icons.Filled.CheckCircle else Icons.Filled.Check,
                label = if (isWatched) "Watched" else "Mark seen",
                active = isWatched,
                tint = HangugColors.Tertiary,
                contentDescription = if (isWatched) "Already watched" else "Mark as watched",
                onClick = { onAction(DeckAction.Watched) },
                enabled = enabled && !isWatched, // no un-watch affordance
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (active) tint.copy(alpha = 0.16f) else HangugColors.SurfaceContainerHigh),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = tint,
                disabledContentColor = tint.copy(alpha = 0.3f),
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) tint else HangugColors.TextSecondary,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}