package com.appriyo.deulama.presentation.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.appriyo.deulama.BuildConfig
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.domain.model.EngagementEntry
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

/**
 * Activity Timeline (Phase 7). One reverse-chronological feed merging
 * the user's favorited + watch-later + watched dramas.
 *
 * Terminal states mirror [com.appriyo.deulama.presentation.genre.GenreStatsScreen]:
 * - `Loading`   — first composition / pull-to-refresh.
 * - `SignedOut` — anonymous (defensive; the auth root already gates
 *                 this tab, but a stale state may slip through).
 * - `Empty`     — signed in with zero timeline rows.
 * - `Error`     — any of the three list calls failed; offers Retry.
 * - `Success`   — `entries` rendered as a `LazyColumn`.
 *
 * The screen is read-only — there's no swipe-to-delete or undo here;
 * optimistic mutations happen from the DramaDetails screen via the
 * existing engagement repositories.
 */
@Composable
fun ActivityScreen(
    onGoToLogin: () -> Unit,
    onOpenDramaDetails: (dramaId: Int) -> Unit,
    viewModel: ActivityViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Header()
            when (val s = state) {
                ActivityUiState.Loading -> LoadingBlock()
                ActivityUiState.SignedOut -> CenteredMessage(
                    title = "Sign in to see your activity",
                    subtitle = "Your favorites, watch-later picks, and watched dramas will show up here once you sign in.",
                    icon = Icons.Filled.Login,
                    actionLabel = "Go to login",
                    onAction = onGoToLogin,
                )
                ActivityUiState.Empty -> CenteredMessage(
                    title = "No activity yet",
                    subtitle = "Favorite a drama, add one to watch later, or mark one as watched — they'll appear here, newest first.",
                    icon = Icons.Filled.History,
                    actionLabel = "Refresh",
                    onAction = viewModel::refresh,
                )
                is ActivityUiState.Error -> CenteredMessage(
                    title = "Couldn't load your activity",
                    subtitle = s.message,
                    icon = Icons.Filled.Refresh,
                    actionLabel = "Try again",
                    onAction = viewModel::refresh,
                )
                is ActivityUiState.Success -> TimelineList(
                    entries = s.entries,
                    onClick = onOpenDramaDetails,
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = "Activity",
            style = MaterialTheme.typography.displayLarge,
            color = HangugColors.TextPrimary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "Your recent favorites, watch-later picks, and watched dramas.",
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.TextSecondary,
        )
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = HangugColors.Primary)
    }
}

@Composable
private fun CenteredMessage(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HangugColors.Secondary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = HangugColors.TextSecondary,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onAction) {
            Text(actionLabel, color = HangugColors.Secondary)
        }
    }
}

@Composable
private fun TimelineList(
    entries: List<EngagementEntry>,
    onClick: (dramaId: Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(entries, key = { entry -> "${entry.kind.name}-${entry.drama.dramaId}-${entry.timestamp}" }) { entry ->
            TimelineRow(entry = entry, onClick = { onClick(entry.drama.dramaId) })
        }
    }
}

@Composable
private fun TimelineRow(
    entry: EngagementEntry,
    onClick: () -> Unit,
) {
    val chip = chipForKind(entry.kind)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HangugColors.SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(resolveDramaPoster(entry.drama))
                .crossfade(true)
                .build(),
            contentDescription = entry.drama.title,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(android.R.drawable.ic_menu_gallery),
            error = painterResource(android.R.drawable.ic_menu_report_image),
            modifier = Modifier
                .size(64.dp, 88.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HangugColors.SurfaceContainerHigh),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.drama.title,
                style = MaterialTheme.typography.titleSmall,
                color = HangugColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChipBadge(icon = chip.icon, label = chip.label, color = chip.color)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = entry.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = HangugColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChipBadge(icon: ImageVector, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

/**
 * Resolve the action chip shown beside each timeline row. Centralises
 * the icon + label + colour mapping so the screen stays declarative.
 */
private fun chipForKind(kind: EngagementEntry.Kind): ChipSpec = when (kind) {
    EngagementEntry.Kind.FAVORITED -> ChipSpec(
        icon = Icons.Filled.Favorite,
        label = "Favorited",
        color = HangugColors.Danger,
    )
    EngagementEntry.Kind.WATCH_LATER -> ChipSpec(
        icon = Icons.Filled.Bookmark,
        label = "Watch later",
        color = HangugColors.Secondary,
    )
    EngagementEntry.Kind.WATCHED -> ChipSpec(
        icon = Icons.Filled.CheckCircle,
        label = "Watched",
        color = HangugColors.Tertiary,
    )
}

private data class ChipSpec(
    val icon: ImageVector,
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
)

/**
 * Resolve an absolute poster URL the API returns. Mirrors the same
 * helper used in EditProfileScreen — kept private to this file rather
 * than promoted, since both helpers are tiny and only consumed once.
 */
private fun resolveDramaPoster(drama: Drama): String? {
    val path = drama.posterUrl ?: return null
    if (path.isBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    val rel = path.trimStart('/')
    return "$base/$rel"
}