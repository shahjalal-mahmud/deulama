package com.appriyo.deulama.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.presentation.components.DramaEngagementActions
import com.appriyo.deulama.ui.theme.HangugBrandGradient
import com.appriyo.deulama.ui.theme.HangugColors
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

/**
 * Drama Details screen. Public, read-only.
 *
 * Visual language mirrors the web client (glass-overlay hero, warm
 * rose/gold accents, pill-shaped chips/badges) while staying native:
 * edge-to-edge banner, a floating circular back button over the
 * artwork instead of a solid app bar, and a raised "engagement card"
 * bottom bar instead of an inline action row.
 *
 * Handles Loading / Success / NotFound / Error via [DramaDetailsUiState].
 * No ViewModel / navigation logic changed from the original screen.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DramaDetailsScreen(
    dramaId: Int,
    onBack: () -> Unit,
    fromRecommendations: Boolean = false,
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
        containerColor = HangugColors.BgBase,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val s = state
            if (s is DramaDetailsUiState.Success) {
                EngagementBar(
                    dramaId = s.drama.dramaId,
                    onFavoriteToggle = { viewModel.toggleFavorite() },
                    onWatchLaterToggle = { viewModel.toggleWatchLater() },
                    onMarkWatched = { viewModel.markWatched() },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HangugColors.BgBase),
        ) {
            when (val s = state) {
                DramaDetailsUiState.Loading -> LoadingState()
                DramaDetailsUiState.NotFound -> NotFoundState(onBack)
                is DramaDetailsUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = viewModel::retry,
                    onBack = onBack,
                )
                is DramaDetailsUiState.Success -> DetailBody(
                    drama = s.drama,
                    fromRecommendations = fromRecommendations,
                    bottomInset = innerPadding.calculateBottomPadding(),
                )
            }

            // Floating back button — always pinned over the artwork,
            // never a solid app bar, so the banner reads edge-to-edge.
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 12.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(HangugColors.BgBase.copy(alpha = 0.55f)),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = HangugColors.TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/* ------------------------------ BODY ------------------------------ */

@Composable
private fun DetailBody(
    drama: Drama,
    fromRecommendations: Boolean,
    bottomInset: androidx.compose.ui.unit.Dp,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomInset + 28.dp),
    ) {
        item { Banner(drama) }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(18.dp))

                if (fromRecommendations) {
                    BecauseYouLikedItBanner()
                    Spacer(Modifier.height(18.dp))
                }

                if (drama.genres.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(count = drama.genres.size, key = { drama.genres[it] }) { idx ->
                            GenreChip(drama.genres[idx])
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                }

                InfoCard(drama)
                Spacer(Modifier.height(28.dp))

                if (drama.storyline.isNotBlank()) {
                    SectionHeading(eyebrow = "STORY", title = "Synopsis")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = drama.storyline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = HangugColors.TextSecondary,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(28.dp))
                }

                if (drama.stars.isNotBlank()) {
                    SectionHeading(eyebrow = "WHO'S IN IT", title = "Cast")
                    Spacer(Modifier.height(14.dp))
                    CastRow(drama.stars)
                    Spacer(Modifier.height(12.dp))
                }
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
            .height(400.dp)
            .background(HangugColors.BgBase),
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = HangugColors.TextPrimary.copy(alpha = 0.55f),
                    modifier = Modifier.size(72.dp),
                )
            }
        }

        // Title + rating overlay sits on top of the image so the artwork
        // is shown as-is with no scrim / tint / gradient covering it.

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (drama.imdbRating != null && drama.imdbRating > 0) {
                    RatingPill(rating = drama.imdbRating)
                    Spacer(Modifier.width(10.dp))
                } else {
                    UpcomingPill()
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = drama.releaseYear,
                    style = MaterialTheme.typography.labelLarge,
                    color = HangugColors.TextSecondary,
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = drama.title,
                style = MaterialTheme.typography.headlineLarge,
                color = HangugColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
            )
        }
    }
}

@Composable
private fun RatingPill(rating: Double) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(HangugColors.SurfaceContainerLowest.copy(alpha = 0.65f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = HangugColors.Secondary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "%.1f".format(rating),
            style = MaterialTheme.typography.labelLarge,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = " / 10",
            style = MaterialTheme.typography.labelMedium,
            color = HangugColors.TextTertiary,
        )
    }
}

@Composable
private fun UpcomingPill() {
    Text(
        text = "UPCOMING",
        style = MaterialTheme.typography.labelSmall,
        color = HangugColors.TextSecondary,
        letterSpacing = 1.2.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/* --------------------------- GENRE CHIP ---------------------------- */

@Composable
private fun GenreChip(genre: String) {
    Text(
        text = genre,
        style = MaterialTheme.typography.labelMedium,
        color = HangugColors.TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(HangugColors.SurfaceContainer)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/* ----------------------------- INFO CARD --------------------------- */

@Composable
private fun InfoCard(drama: Drama) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HangugColors.SurfaceContainer)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        InfoCell(
            icon = Icons.Filled.CalendarToday,
            label = "Year",
            value = drama.releaseYear,
        )
        InfoDivider()
        InfoCell(
            icon = Icons.Filled.Star,
            label = "Rating",
            value = drama.imdbRating?.takeIf { it > 0 }?.let { "%.1f".format(it) } ?: "—",
        )
        InfoDivider()
        InfoCell(
            icon = Icons.Filled.Groups,
            label = "Cast",
            value = drama.stars.split(',').firstOrNull()?.trim()?.ifBlank { "—" } ?: "—",
        )
    }
}

@Composable
private fun InfoDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(38.dp)
            .background(HangugColors.BorderSubtle),
    )
}

@Composable
private fun InfoCell(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(HangugColors.Secondary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HangugColors.Secondary,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.TextTertiary,
        )
    }
}

/* ----------------------------- SECTION HEADING ---------------------------- */

@Composable
private fun SectionHeading(eyebrow: String, title: String) {
    Column {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.Secondary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

/* ------------------------------ CAST ROW --------------------------- */

@Composable
private fun CastRow(starsCsv: String) {
    val names = remember(starsCsv) {
        starsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(count = names.size, key = { names[it] }) { idx ->
            CastAvatar(names[idx])
        }
    }
}

@Composable
private fun CastAvatar(name: String) {
    val hue = remember(name) { (abs(name.sumOf { it.code }) % 360).toFloat() }
    val bg = remember(hue) { Color.hsv(hue, 0.28f, 0.20f) }
    val fg = remember(hue) { Color.hsv(hue, 0.35f, 0.85f) }
    val initials = remember(name) {
        name.trim().split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = fg,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/* --------------------------- BANNER: RECS ------------------------- */

@Composable
private fun BecauseYouLikedItBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        HangugColors.PrimaryContainer.copy(alpha = 0.28f),
                        HangugColors.SecondaryContainer.copy(alpha = 0.20f),
                    ),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(HangugColors.Primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = HangugColors.Primary,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Because you liked this kind of drama",
            style = MaterialTheme.typography.titleSmall,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* ------------------------- ENGAGEMENT BAR --------------------------- */

/**
 * Floating "card" bottom bar — visually mirrors the web hero's pill
 * action row, but as a raised native surface rather than an inline row,
 * so it stays reachable while scrolling long detail content.
 */
@Composable
private fun EngagementBar(
    dramaId: Int,
    onFavoriteToggle: () -> Unit,
    onWatchLaterToggle: () -> Unit,
    onMarkWatched: () -> Unit,
) {
    Surface(
        color = HangugColors.SurfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
    ) {
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(vertical = 10.dp),
        ) {
            DramaEngagementActions(
                dramaId = dramaId,
                onFavoriteToggle = onFavoriteToggle,
                onWatchLaterToggle = onWatchLaterToggle,
                onMarkWatched = onMarkWatched,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

/* --------------------------- TERMINAL UI -------------------------- */

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HangugColors.BgBase),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = HangugColors.Primary,
            trackColor = HangugColors.SurfaceContainer,
            strokeWidth = 3.dp,
        )
    }
}

@Composable
private fun NotFoundState(onBack: () -> Unit) {
    CenteredMessage(
        icon = Icons.Filled.SearchOff,
        title = "Drama not found",
        subtitle = "It may have been removed.",
        actionLabel = "Back to browse",
        onAction = onBack,
    )
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    CenteredMessage(
        icon = Icons.Filled.ErrorOutline,
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
    icon: ImageVector,
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
            .background(HangugColors.BgBase)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(brush = HangugBrandGradient, alpha = 0.15f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = HangugColors.Primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = HangugColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = HangugColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(brush = HangugBrandGradient)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 28.dp, vertical = 12.dp),
            ) {
                Text(
                    actionLabel,
                    color = HangugColors.OnPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (secondaryLabel != null && onSecondary != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    secondaryLabel,
                    color = HangugColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onSecondary)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}