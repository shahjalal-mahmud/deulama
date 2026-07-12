package com.appriyo.deulama.presentation.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

private const val ROTATE_MS = 5000L

/**
 * Home's hero — a native HorizontalPager instead of the web's manual
 * crossfade carousel. Swiping is free, and dot indicators (not
 * left/right arrow buttons) are the idiomatic Android pattern for a
 * touch-first surface.
 */
@Composable
fun HeroSpotlight(
    items: List<Drama>,
    loading: Boolean,
    onDramaClick: (Drama) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (loading && items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(440.dp)
                .background(HangugColors.SurfaceContainer),
        )
        return
    }
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(pagerState) {
        while (true) {
            delay(ROTATE_MS)
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next, animationSpec = tween(600))
        }
    }

    Box(modifier = modifier.fillMaxWidth().height(440.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val drama = items[page]
            Box(modifier = Modifier.fillMaxSize().clickable { onDramaClick(drama) }) {
                Box(modifier = Modifier.fillMaxSize().background(HangugBrandGradient)) {
                    val img = drama.bannerUrl ?: drama.posterUrl
                    if (!img.isNullOrBlank()) {
                        AsyncImage(
                            model = img,
                            contentDescription = drama.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xE60B0708)),
                            startY = 260f,
                        ),
                    ),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Text(
                        text = "지금 주목할 드라마 · SPOTLIGHT",
                        style = MaterialTheme.typography.labelSmall,
                        color = HangugColors.Secondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (drama.imdbRating != null) {
                            Row(
                                modifier = Modifier
                                    .background(HangugColors.SurfaceContainerLowest.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Star, null, tint = HangugColors.Secondary, modifier = Modifier.width(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("%.1f".format(drama.imdbRating), style = MaterialTheme.typography.labelSmall, color = HangugColors.TextPrimary)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(drama.releaseYear, style = MaterialTheme.typography.labelSmall, color = HangugColors.TextTertiary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = drama.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = HangugColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Dot indicator — the native replacement for the web's thumbnail rail.
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            repeat(items.size) { i ->
                val active = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(if (active) HangugColors.Primary else HangugColors.TextPrimary.copy(alpha = 0.35f))
                        .width(if (active) 16.dp else 6.dp)
                        .height(6.dp),
                )
            }
        }
    }
}