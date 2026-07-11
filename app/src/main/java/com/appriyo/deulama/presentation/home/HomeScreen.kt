package com.appriyo.deulama.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.appriyo.deulama.presentation.components.DramaCard
import com.appriyo.deulama.presentation.components.DramaCardVariant
import com.appriyo.deulama.presentation.components.StatusBanner
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

/**
 * Home tab. Reads from:
 *  - session (greeting)
 *  - health-check (status banner)
 *  - catalog (Trending now rail — IMDB rating DESC)
 *
 * Continue Watching row is intentionally a static placeholder until
 * Phase 4. Trending rail is paginated horizontally via Paging 3.
 */
@Composable
fun HomeScreen(
    onOpenDiscover: () -> Unit,
    onOpenDramaDetails: (Int) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val trending = viewModel.trendingPaging.collectAsLazyPagingItems()

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "지금 인기 · TRENDING NOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = HangugColors.Secondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Home",
                        style = MaterialTheme.typography.displayLarge,
                        color = HangugColors.TextPrimary,
                    )
                    val greeting = uiState.user?.fullName
                    if (greeting != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "안녕하세요 · Hi, $greeting",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HangugColors.TextSecondary,
                        )
                    }
                }
            }

            item {
                StatusBanner(
                    status = uiState.status,
                    message = uiState.message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
            }

            // ---- TRENDING NOW rail ---------------------------------
            item { SectionHeader(label = "Trending now") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (trending.itemCount == 0) {
                        // Empty-state copy right-aligned inside the row.
                        item {
                            Text(
                                text = "Loading…",
                                style = MaterialTheme.typography.bodySmall,
                                color = HangugColors.TextTertiary,
                                modifier = Modifier.padding(vertical = 60.dp),
                            )
                        }
                    } else {
                        items(
                            count = trending.itemCount,
                            key = { idx -> trending.peek(idx)?.dramaId ?: idx },
                        ) { idx ->
                            val drama = trending[idx]
                            if (drama != null) {
                                DramaCard(
                                    drama = drama,
                                    onClick = { onOpenDramaDetails(drama.dramaId) },
                                    variant = DramaCardVariant.LANDSCAPE,
                                )
                            }
                        }
                    }
                }
            }

            // ---- CONTINUE WATCHING (static until Phase 4) ----------
            item { SectionHeader(label = "Continue watching") }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(140.dp)
                        .background(
                            color = HangugColors.SurfaceContainer,
                            shape = MaterialTheme.shapes.large,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(HangugColors.Primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "▶",
                                color = HangugColors.OnPrimary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Nothing here yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HangugColors.TextSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Pick up where you left off — coming in Phase 4",
                            style = MaterialTheme.typography.labelSmall,
                            color = HangugColors.TextTertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = HangugColors.TextPrimary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
}
