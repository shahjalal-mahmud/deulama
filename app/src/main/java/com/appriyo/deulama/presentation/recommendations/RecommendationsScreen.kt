package com.appriyo.deulama.presentation.recommendations

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.presentation.components.MatchScoreBadge
import com.appriyo.deulama.presentation.components.MatchScoreKind
import com.appriyo.deulama.presentation.components.recommendBadgeKind
import com.appriyo.deulama.ui.theme.HangugBrandGradient
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

/**
 * "For You" tab. Renders the up-to-10 picks returned by
 * `GET /api/recommendations`, with:
 *
 *  - a cold-start banner when the response is `fallback: true`
 *    (brand-new user with no swipe / favorite / watch history);
 *  - a `MatchScoreBadge` per card derived from `is_personalized` +
 *    the item's rank in the list (per api.md, no numeric scores are
 *    exposed — we show categorical labels only);
 *  - signed-out / empty / error terminal states with retry CTA.
 *
 * Tapping a row passes `fromRecommendations = true` to the Details
 * route so the details screen can render "Because you liked X"
 * reasoning if it wants to.
 */
@Composable
fun RecommendationsScreen(
    onOpenDramaDetails: (Int, Boolean) -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: RecommendationsViewModel = koinViewModel(),
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
                RecommendationsUiState.Loading -> LoadingBlock()
                RecommendationsUiState.SignedOut -> SignedOutBlock(onGoToLogin)
                RecommendationsUiState.Empty -> EmptyBlock(onRetry = viewModel::refresh)
                is RecommendationsUiState.Error -> ErrorBlock(
                    message = s.message,
                    onRetry = viewModel::refresh,
                )
                is RecommendationsUiState.Success -> SuccessBlock(
                    dramas = s.set.items,
                    isPersonalized = s.isPersonalized,
                    isFallback = s.isFallback,
                    onOpenDramaDetails = onOpenDramaDetails,
                )
            }
        }
    }
}

/* ----------------------------- HEADER ----------------------------- */

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            text = "당신을 위한 · FOR YOU",
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.Secondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Personalized picks",
            style = MaterialTheme.typography.headlineSmall,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* --------------------------- SUCCESS LIST ------------------------- */

@Composable
private fun SuccessBlock(
    dramas: List<Drama>,
    isPersonalized: Boolean,
    isFallback: Boolean,
    onOpenDramaDetails: (Int, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isFallback) {
            item { ColdStartBanner() }
        }

        items(count = dramas.size, key = { idx -> dramas[idx].dramaId }) { idx ->
            val rank = idx + 1
            RecommendationRow(
                rank = rank,
                drama = dramas[idx],
                isPersonalized = isPersonalized,
                isFallback = isFallback,
                onClick = { onOpenDramaDetails(dramas[idx].dramaId, true) },
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun RecommendationRow(
    rank: Int,
    drama: Drama,
    isPersonalized: Boolean,
    isFallback: Boolean,
    onClick: () -> Unit,
) {
    val kind = recommendBadgeKind(rank, isPersonalized, isFallback)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HangugColors.SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 110.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HangugBrandGradient),
            contentAlignment = Alignment.Center,
        ) {
            val url = drama.posterUrl
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = drama.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = HangugColors.TextPrimary.copy(alpha = 0.55f),
                    modifier = Modifier.size(32.dp),
                )
            }
            // Rank badge in the top-left corner — we expose the rank
            // as a small ordinal pill because the API gives us order
            // but not a numeric score.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(HangugColors.SurfaceContainerLowest.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelSmall,
                    color = HangugColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MatchScoreBadge(kind = kind, rank = rank.takeIf { kind == MatchScoreKind.TOP_PICK })
            Text(
                text = drama.title,
                style = MaterialTheme.typography.titleMedium,
                color = HangugColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            MetadataLine(drama)
            if (drama.genres.isNotEmpty()) {
                Text(
                    text = drama.genres.take(3).joinToString(" · "),
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
private fun MetadataLine(drama: Drama) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (drama.imdbRating != null) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = HangugColors.Secondary,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "%.1f".format(drama.imdbRating),
                style = MaterialTheme.typography.labelSmall,
                color = HangugColors.TextSecondary,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = drama.releaseYear,
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.TextSecondary,
        )
    }
}

@Composable
private fun ColdStartBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HangugColors.PrimaryContainer.copy(alpha = 0.25f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = HangugColors.Primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Top picks while we learn your taste",
                style = MaterialTheme.typography.titleSmall,
                color = HangugColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Swipe or favorite a few dramas to unlock personalized picks.",
                style = MaterialTheme.typography.bodySmall,
                color = HangugColors.TextSecondary,
            )
        }
    }
}

/* ---------------------------- TERMINALS --------------------------- */

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        CircularProgressIndicator(color = HangugColors.Primary)
    }
}

@Composable
private fun SignedOutBlock(onGoToLogin: () -> Unit) {
    CenteredMessage(
        title = "Sign in to see your picks",
        subtitle = "Recommendations need an account so we can tune them to your taste.",
        icon = Icons.Filled.Login,
        actionLabel = "Go to login",
        onAction = onGoToLogin,
    )
}

@Composable
private fun EmptyBlock(onRetry: () -> Unit) {
    CenteredMessage(
        title = "No picks yet",
        subtitle = "Try refreshing in a moment — we update these daily.",
        icon = Icons.Filled.AutoAwesome,
        actionLabel = "Refresh",
        onAction = onRetry,
    )
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    CenteredMessage(
        title = "Couldn't load recommendations",
        subtitle = message,
        icon = Icons.Filled.Refresh,
        actionLabel = "Try again",
        onAction = onRetry,
    )
}

@Composable
private fun CenteredMessage(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(HangugColors.SurfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HangugColors.Primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = HangugColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onAction) {
            Text(actionLabel, color = HangugColors.Secondary)
        }
    }
}
