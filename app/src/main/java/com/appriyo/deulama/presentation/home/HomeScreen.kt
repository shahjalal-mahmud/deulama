package com.appriyo.deulama.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.presentation.components.GenreChip
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onOpenDiscover: () -> Unit,
    onOpenDramaDetails: (Int) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val goToDetails: (Drama) -> Unit = { onOpenDramaDetails(it.dramaId) }

    Scaffold(containerColor = HangugColors.BgBase) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
            ),
        ) {
            // Hero bleeds under the status bar — no top inset padding,
            // matching the web's "navbar floats over hero" treatment.
            item {
                HeroSpotlight(
                    items = uiState.spotlight,
                    loading = uiState.spotlightLoading,
                    onDramaClick = goToDetails,
                )
            }

            // Genre filter pills — drive Trending Now below.
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                ) {
                    item {
                        GenreChip(
                            label = "All",
                            selected = uiState.selectedGenre == null,
                            onClick = { viewModel.onSelectGenre(null) },
                        )
                    }
                    items(HOME_GENRE_FILTERS) { genre ->
                        GenreChip(
                            label = genre,
                            selected = uiState.selectedGenre == genre,
                            onClick = { viewModel.onSelectGenre(genre) },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // 1. Trending Now
            item {
                TrendingRail(items = uiState.trending, loading = uiState.trendingLoading, onDramaClick = goToDetails)
                Spacer(Modifier.height(28.dp))
            }

            // 2. Top Picks For You
            item {
                RecommendationList(items = uiState.recommendations, loading = uiState.recommendationsLoading, onDramaClick = goToDetails)
                Spacer(Modifier.height(28.dp))
            }

            // 3-9. Genre shelves
            items(uiState.genreSections, key = { it.key }) { section ->
                GenreRail(
                    section = section,
                    onDramaClick = goToDetails,
                    onViewAll = { onOpenDiscover() },
                )
                Spacer(Modifier.height(28.dp))
            }

            // 10. All Dramas preview
            item {
                AllDramaPreview(
                    items = uiState.allDramaPreview,
                    loading = uiState.allDramaLoading,
                    onDramaClick = goToDetails,
                    onViewAll = onOpenDiscover,
                )
            }
        }
    }
}