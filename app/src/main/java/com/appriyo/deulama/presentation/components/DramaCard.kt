package com.appriyo.deulama.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.ui.theme.HangugBrandGradient
import com.appriyo.deulama.ui.theme.HangugColors

/**
 * Three layouts, one composable — keeps Home, Discover, and (later)
 * Recommendations visually consistent without per-screen card clones.
 *
 *  - [DramaCardVariant.POSTER]    — 2:3 portrait tile. Discover grid.
 *  - [DramaCardVariant.LANDSCAPE] — 16:9 wide tile. Home trending rail.
 *  - [DramaCardVariant.COMPACT]   — title-only chip. Future "continue
 *                                   watching" / search results rows.
 *
 * Falls back to a branded gradient + film icon when the poster/banner
 * URL is null, so callers never see a broken-image icon.
 */
enum class DramaCardVariant { POSTER, LANDSCAPE, COMPACT }

@Composable
fun DramaCard(
    drama: Drama,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DramaCardVariant = DramaCardVariant.POSTER,
) {
    when (variant) {
        DramaCardVariant.POSTER -> PosterCard(drama, onClick, modifier)
        DramaCardVariant.LANDSCAPE -> LandscapeCard(drama, onClick, modifier)
        DramaCardVariant.COMPACT -> CompactCard(drama, onClick, modifier)
    }
}

/* ----------------------------- POSTER ----------------------------- */

@Composable
private fun PosterCard(
    drama: Drama,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Card(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = HangugColors.SurfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            DramaImage(
                imageUrl = drama.posterUrl,
                contentDescription = drama.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
            )
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = drama.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = HangugColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                RatingLine(drama = drama)
            }
        }
    }
}

/* --------------------------- LANDSCAPE ---------------------------- */

@Composable
private fun LandscapeCard(
    drama: Drama,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .width(260.dp)
            .height(150.dp)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = HangugColors.SurfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DramaImage(
                imageUrl = drama.bannerUrl ?: drama.posterUrl,
                contentDescription = drama.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Bottom gradient so title stays legible over any image.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC0B0708)),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY,
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top-right: rating pill (gold star)
                if (drama.imdbRating != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .background(
                                color = HangugColors.SurfaceContainerLowest.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = HangugColors.Secondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "%.1f".format(drama.imdbRating),
                            style = MaterialTheme.typography.labelSmall,
                            color = HangugColors.TextPrimary,
                        )
                    }
                }
                Text(
                    text = drama.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = HangugColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/* ----------------------------- COMPACT ---------------------------- */

@Composable
private fun CompactCard(
    drama: Drama,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Card(
        modifier = modifier
            .width(220.dp)
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HangugColors.SurfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DramaImage(
                imageUrl = drama.posterUrl,
                contentDescription = drama.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = drama.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = HangugColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                RatingLine(drama = drama)
            }
        }
    }
}

/* ----------------------------- shared ----------------------------- */

/** "★ 8.4 · 2019" — tiny metadata line for Poster + Compact cards. */
@Composable
private fun RatingLine(drama: Drama) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (drama.imdbRating != null) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = HangugColors.Secondary,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(3.dp))
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
            color = HangugColors.TextTertiary,
        )
    }
}

/**
 * Coil3 AsyncImage with a branded fallback. When the URL is set but
 * loading fails we leave the gradient background visible so the slot
 * still reads as a "drama tile" — indistinguishable from a successful
 * load at first glance, and much better than a broken-image glyph.
 */
@Composable
private fun DramaImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(modifier = modifier.background(HangugBrandGradient)) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        } else {
            PlaceholderIcon()
        }
    }
}

@Composable
private fun PlaceholderIcon() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = HangugColors.TextPrimary.copy(alpha = 0.55f),
            modifier = Modifier.size(36.dp),
        )
    }
}
