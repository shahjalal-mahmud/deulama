package com.appriyo.deulama.presentation.genre

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Login
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appriyo.deulama.domain.model.ActivityTotals
import com.appriyo.deulama.domain.model.GenreScore
import com.appriyo.deulama.domain.model.GenreStatistics
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

/**
 * Per-genre preference breakdown. Renders the server-computed scores
 * as a ranked list with proportional bars. Critical: **never**
 * recompute or scale the scores — server has the only authority on
 * the formula per api.md.
 *
 * Terminal states:
 *  - signed-out (no JWT)
 *  - empty   (brand-new account, statistics: [])
 *  - error   (non-2xx with retry CTA)
 */
@Composable
fun GenreStatsScreen(
    onGoToLogin: () -> Unit,
    viewModel: GenreStatsViewModel = koinViewModel(),
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
                GenreStatsUiState.Loading -> LoadingBlock()
                GenreStatsUiState.SignedOut -> CenteredMessage(
                    title = "Sign in to see your taste",
                    subtitle = "Genre breakdown uses your swipe / favorite / watched history to score each genre — sign in to unlock it.",
                    icon = Icons.Filled.Login,
                    actionLabel = "Go to login",
                    onAction = onGoToLogin,
                )
                GenreStatsUiState.Empty -> CenteredMessage(
                    title = "Not enough activity yet",
                    subtitle = "Swipe or favorite a few dramas to start building your breakdown.",
                    icon = Icons.Filled.AutoAwesome,
                    actionLabel = "Refresh",
                    onAction = viewModel::refresh,
                )
                is GenreStatsUiState.Error -> CenteredMessage(
                    title = "Couldn't load your breakdown",
                    subtitle = s.message,
                    icon = Icons.Filled.Refresh,
                    actionLabel = "Try again",
                    onAction = viewModel::refresh,
                )
                is GenreStatsUiState.Success -> SuccessBody(s.stats)
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
            text = "취향 분석 · GENRE BREAKDOWN",
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.Secondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Your taste by genre",
            style = MaterialTheme.typography.headlineSmall,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Scores are computed by the server — we just display them.",
            style = MaterialTheme.typography.bodySmall,
            color = HangugColors.TextTertiary,
        )
    }
}

/* --------------------------- SUCCESS LIST ------------------------- */

@Composable
private fun SuccessBody(stats: GenreStatistics) {
    val maxScore = stats.statistics.maxOfOrNull { it.score }?.coerceAtLeast(1) ?: 1

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TotalsStrip(stats.totals)
            Spacer(Modifier.height(4.dp))
        }

        items(count = stats.statistics.size, key = { idx -> stats.statistics[idx].genre }) { idx ->
            val row = stats.statistics[idx]
            GenreRow(rank = idx + 1, genre = row, maxScore = maxScore)
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TotalsStrip(totals: ActivityTotals) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HangugColors.SurfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TotalCell(label = "Liked", value = totals.liked)
        TotalCell(label = "Watched", value = totals.watched)
        TotalCell(label = "Disliked", value = totals.disliked)
    }
}

@Composable
private fun TotalCell(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.TextTertiary,
        )
    }
}

@Composable
private fun GenreRow(rank: Int, genre: GenreScore, maxScore: Int) {
    // Width percent driven directly by the server-computed score vs
    // the max in the list — we deliberately do NOT scale or clamp
    // any further, per api.md.
    val ratio = (genre.score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HangugColors.SurfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(HangugColors.SurfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelSmall,
                color = HangugColors.TextSecondary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = genre.genre,
                    style = MaterialTheme.typography.titleSmall,
                    color = HangugColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = genre.score.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = HangugColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(6.dp))
            // Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HangugColors.SurfaceContainerHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = ratio)
                        .background(HangugColors.Primary),
                )
            }
            Spacer(Modifier.height(4.dp))
            BreakdownLine(genre)
        }
    }
}

@Composable
private fun BreakdownLine(genre: GenreScore) {
    val liked = genre.liked?.toString() ?: "—"
    val watched = genre.watched?.toString() ?: "—"
    val disliked = genre.disliked?.toString() ?: "—"
    Text(
        text = "👍 $liked  ·  👁 $watched  ·  ✕ $disliked",
        style = MaterialTheme.typography.labelSmall,
        color = HangugColors.TextTertiary,
    )
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(32.dp))
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
