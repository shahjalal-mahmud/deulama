package com.appriyo.deulama.presentation.details

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.presentation.components.DramaEngagementActions
import com.appriyo.deulama.ui.theme.HangugBrandGradient
import com.appriyo.deulama.ui.theme.HangugColors
import com.appriyo.deulama.ui.theme.HangugGlassOverlay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Drama Details screen. Public, read-only.
 *
 * Layout:
 *  - banner image (banner_url || poster_url) with a [HangugGlassOverlay]
 *    scrim and the title floating on top;
 *  - info grid (year / rating / genre count);
 *  - genre chips;
 *  - storyline;
 *  - cast ("stars");
 *  - "loaded at" footer (mostly so QA can spot stale data).
 *
 * Handles Loading / Success / NotFound / Error via [DramaDetailsUiState].
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DramaDetailsScreen(
    dramaId: Int,
    onBack: () -> Unit,
    viewModel: DramaDetailsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(dramaId) { viewModel.load(dramaId) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DramaDetailsEvent.Info -> scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { /* hidden under the banner */ },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HangugColors.TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HangugColors.BgBase,
                    navigationIconContentColor = HangugColors.TextPrimary,
                ),
            )
        },
        bottomBar = {
            // Only render the engagement bar once we have the drama.
            val s = state
            if (s is DramaDetailsUiState.Success) {
                Surface(
                    color = HangugColors.BgElevated,
                    tonalElevation = 6.dp,
                ) {
                    DramaEngagementActions(
                        dramaId = s.drama.dramaId,
                        onFavoriteToggle = { viewModel.toggleFavorite() },
                        onWatchLaterToggle = { viewModel.toggleWatchLater() },
                        onMarkWatched = { viewModel.markWatched() },
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
        },
    ) { innerPadding ->
        when (val s = state) {
            DramaDetailsUiState.Loading -> LoadingState(innerPadding)
            DramaDetailsUiState.NotFound -> NotFoundState(innerPadding, onBack)
            is DramaDetailsUiState.Error -> ErrorState(
                innerPadding,
                message = s.message,
                onRetry = viewModel::retry,
                onBack = onBack,
            )
            is DramaDetailsUiState.Success -> DetailBody(
                drama = s.drama,
                contentPadding = innerPadding,
            )
        }
    }
}

/* ------------------------------ BODY ------------------------------ */

@Composable
private fun DetailBody(
    drama: Drama,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HangugColors.BgBase),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item { Banner(drama) }
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                InfoGrid(drama)
                Spacer(Modifier.height(20.dp))
                if (drama.genres.isNotEmpty()) {
                    Text(
                        text = "Genres",
                        style = MaterialTheme.typography.titleSmall,
                        color = HangugColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(count = drama.genres.size, key = { drama.genres[it] }) { idx ->
                            val genre = drama.genres[idx]
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = HangugColors.TextPrimary,
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = HangugColors.SurfaceContainer,
                                    labelColor = HangugColors.TextPrimary,
                                ),
                                border = null,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                if (drama.storyline.isNotBlank()) {
                    SectionTitle("Synopsis")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = drama.storyline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = HangugColors.TextSecondary,
                    )
                    Spacer(Modifier.height(20.dp))
                }
                if (drama.stars.isNotBlank()) {
                    SectionTitle("Cast")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = drama.stars,
                        style = MaterialTheme.typography.bodyMedium,
                        color = HangugColors.TextSecondary,
                    )
                    Spacer(Modifier.height(24.dp))
                }
                FooterLine(drama)
            }
        }
    }
}

/* ----------------------------- BANNER ----------------------------- */

@Composable
private fun Banner(drama: Drama) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(HangugBrandGradient),
    ) {
        val imageUrl = drama.bannerUrl ?: drama.posterUrl
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = drama.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Branded fallback: gradient + film icon.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = HangugColors.TextPrimary.copy(alpha = 0.55f),
                    modifier = Modifier.size(72.dp),
                )
            }
        }
        // Scrim — matches the web client's `.glass-overlay` over hero
        // images so the title stays legible on any banner art.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HangugGlassOverlay),
        )
        // Title overlay — pinned to bottom-start.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = drama.title,
                style = MaterialTheme.typography.headlineMedium,
                color = HangugColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = drama.releaseYear,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HangugColors.TextPrimary.copy(alpha = 0.85f),
                )
                if (drama.imdbRating != null) {
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = HangugColors.Secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "%.1f IMDb".format(drama.imdbRating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = HangugColors.TextPrimary.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

/* ---------------------------- INFO GRID --------------------------- */

@Composable
private fun InfoGrid(drama: Drama) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HangugColors.SurfaceContainer)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        InfoCell(
            icon = Icons.Filled.CalendarToday,
            label = "Year",
            value = drama.releaseYear,
        )
        InfoCell(
            icon = Icons.Filled.Star,
            label = "Rating",
            value = drama.imdbRating?.let { "%.1f".format(it) } ?: "—",
        )
        InfoCell(
            icon = Icons.Filled.People,
            label = "Cast",
            value = drama.stars.split(',').firstOrNull()?.trim()?.ifBlank { "—" } ?: "—",
        )
    }
}

@Composable
private fun InfoCell(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HangugColors.Secondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.TextTertiary,
        )
    }
}

/* ----------------------------- helpers ---------------------------- */

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = HangugColors.TextPrimary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun FooterLine(drama: Drama) {
    Text(
        text = "Drama #${drama.dramaId} · added ${drama.createdAt}",
        style = MaterialTheme.typography.labelSmall,
        color = HangugColors.TextTertiary,
    )
}

/* --------------------------- TERMINAL UI -------------------------- */

@Composable
private fun LoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(HangugColors.BgBase),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = HangugColors.Primary) }
}

@Composable
private fun NotFoundState(padding: PaddingValues, onBack: () -> Unit) {
    CenteredMessage(
        padding = padding,
        title = "Drama not found",
        subtitle = "It may have been removed.",
        actionLabel = "Back to browse",
        onAction = onBack,
    )
}

@Composable
private fun ErrorState(
    padding: PaddingValues,
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    CenteredMessage(
        padding = padding,
        title = "Something went wrong",
        subtitle = message,
        actionLabel = "Try again",
        onAction = onRetry,
        secondaryLabel = "Back",
        onSecondary = onBack,
    )
}

@Composable
private fun CenteredMessage(
    padding: PaddingValues,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(HangugColors.BgBase)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = HangugColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = HangugColors.TextSecondary,
            )
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel, color = HangugColors.Secondary)
            }
            if (secondaryLabel != null && onSecondary != null) {
                TextButton(onClick = onSecondary) {
                    Text(secondaryLabel, color = HangugColors.TextSecondary)
                }
            }
        }
    }
}