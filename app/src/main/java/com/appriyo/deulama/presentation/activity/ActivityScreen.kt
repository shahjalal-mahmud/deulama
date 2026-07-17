package com.appriyo.deulama.presentation.activity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Header(entryCount = (state as? ActivityUiState.Success)?.entries?.size)

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(280)))
                        .togetherWith(fadeOut(animationSpec = tween(160)))
                },
                label = "activity-state",
            ) { s ->
                when (s) {
                    ActivityUiState.Loading -> LoadingBlock()
                    ActivityUiState.SignedOut -> CenteredMessage(
                        title = "Sign in to see your activity",
                        subtitle = "Your favorites, watch-later picks, and watched dramas will show up here once you sign in.",
                        icon = Icons.AutoMirrored.Filled.Login,
                        accentColor = HangugColors.Primary,
                        actionLabel = "Go to login",
                        onAction = onGoToLogin,
                    )
                    ActivityUiState.Empty -> CenteredMessage(
                        title = "No activity yet",
                        subtitle = "Favorite a drama, add one to watch later, or mark one as watched — they'll appear here, newest first.",
                        icon = Icons.Filled.History,
                        accentColor = HangugColors.Secondary,
                        actionLabel = "Refresh",
                        onAction = viewModel::refresh,
                    )
                    is ActivityUiState.Error -> CenteredMessage(
                        title = "Couldn't load your activity",
                        subtitle = s.message,
                        icon = Icons.Filled.WifiOff,
                        accentColor = HangugColors.Danger,
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
}

@Composable
private fun Header(entryCount: Int?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = HangugColors.TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Your recent favorites, watch-later picks, and watched dramas.",
                    style = MaterialTheme.typography.labelSmall,
                    color = HangugColors.TextSecondary,
                )
            }

            if (entryCount != null && entryCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    HangugColors.Primary,
                                    HangugColors.Primary.copy(alpha = 0.7f),
                                ),
                            ),
                        )
                        .size(46.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = entryCount.coerceAtMost(99).toString() + if (entryCount > 99) "+" else "",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        HeaderDivider()
    }
}

@Composable
private fun HeaderDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        HangugColors.SurfaceContainerHigh,
                        HangugColors.SurfaceContainerHigh.copy(alpha = 0f),
                    ),
                ),
            ),
    )
}

@Composable
private fun LoadingBlock() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(6) {
            ShimmerRow()
        }
    }
}

@Composable
private fun ShimmerRow() {
    val shimmerColor = HangugColors.SurfaceContainer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(shimmerColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp, 88.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HangugColors.SurfaceContainerHigh),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HangugColors.SurfaceContainerHigh),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HangugColors.SurfaceContainerHigh),
            )
        }
    }
}

@Composable
private fun CenteredMessage(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = HangugColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = HangugColors.TextSecondary,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onAction,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White,
            ),
        ) {
            if (actionLabel == "Try again" || actionLabel == "Refresh") {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(actionLabel, fontWeight = FontWeight.SemiBold)
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(entries, key = { entry -> "${entry.kind.name}-${entry.drama.dramaId}-${entry.timestamp}" }) { entry ->
            TimelineRow(entry = entry, onClick = { onClick(entry.drama.dramaId) })
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun TimelineRow(
    entry: EngagementEntry,
    onClick: () -> Unit,
) {
    val chip = chipForKind(entry.kind)
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = chip.color.copy(alpha = 0.3f)),
                onClick = onClick,
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HangugColors.SurfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp, 92.dp)
                    .clip(RoundedCornerShape(14.dp)),
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
                        .fillMaxSize()
                        .background(HangugColors.SurfaceContainerHigh),
                )
                // Subtle bottom scrim so any future overlaid text stays legible
                // and the poster reads as a polished thumbnail, not a raw crop.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f)),
                                startY = 60f,
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp)),
                ) {
                    PosterBorder(color = chip.color.copy(alpha = 0.35f))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.drama.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = HangugColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChipBadge(icon = chip.icon, label = chip.label, color = chip.color)
                }
                Spacer(Modifier.height(6.dp))
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

/**
 * Thin decorative outline drawn on top of the poster to tie the
 * thumbnail's frame color to the row's action chip. Purely cosmetic —
 * does not affect layout, click targets, or image loading.
 */
@Composable
private fun PosterBorder(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRoundRect(
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx()),
            topLeft = Offset.Zero,
        )
    }
}

@Composable
private fun ChipBadge(icon: ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
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
    val color: Color,
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