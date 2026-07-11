package com.appriyo.deulama.presentation.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.appriyo.deulama.presentation.components.DramaCard
import com.appriyo.deulama.presentation.components.DramaCardVariant
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

/**
 * Phase 2 Discover: the full catalog in a 2-column grid of POSTER
 * DramaCards, paginated via Paging 3. Phase 3 will replace this with a
 * swipe-deck (Tinder-style) recommendation flow.
 *
 * Loading / error states are rendered inside the same grid so the
 * surrounding scaffold doesn't reflow when more pages stream in.
 */
@Composable
fun DiscoverScreen(
    onOpenDramaDetails: (Int) -> Unit,
    viewModel: DiscoverViewModel = koinViewModel(),
) {
    val items = viewModel.catalog.collectAsLazyPagingItems()

    Scaffold { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(COLUMNS),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // First-load spinner when *nothing* has arrived yet.
            if (items.loadState.refresh is LoadState.Loading) {
                item(span = { GridItemSpan(COLUMNS) }) {
                    InitialLoading()
                }
            }

            // First-load fatal: show a retry CTA the whole grid instead
            // of empty cells.
            val refreshError = items.loadState.refresh as? LoadState.Error
            if (refreshError != null) {
                item(span = { GridItemSpan(COLUMNS) }) {
                    ErrorState(
                        message = refreshError.error.localizedMessage
                            ?: "Couldn't load the catalog.",
                        onRetry = { items.retry() },
                    )
                }
            }

            // Main catalog body. `count == 0` while loading, so the
            // spinner above covers the truly-empty case.
            items(
                count = items.itemCount,
                key = { idx -> items.peek(idx)?.dramaId ?: idx },
            ) { idx ->
                val drama = items[idx]
                if (drama != null) {
                    DramaCard(
                        drama = drama,
                        onClick = { onOpenDramaDetails(drama.dramaId) },
                        variant = DramaCardVariant.POSTER,
                    )
                }
            }

            // Append-state UI: spinner when loading the next page,
            // "tap to retry" when it failed.
            val append = items.loadState.append
            when (append) {
                is LoadState.Loading -> {
                    item(span = { GridItemSpan(COLUMNS) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = HangugColors.Primary) }
                    }
                }
                is LoadState.Error -> {
                    item(span = { GridItemSpan(COLUMNS) }) {
                        ErrorState(
                            message = append.error.localizedMessage
                                ?: "Couldn't load more.",
                            onRetry = { items.retry() },
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}

private const val COLUMNS = 2

/* LazyVerticalGrid's GridItemSpan lives in the grid DSL — import inline
 * above so this file stays self-contained. */
private fun GridItemSpan(span: Int) =
    androidx.compose.foundation.lazy.grid.GridItemSpan(span)

@Composable
private fun InitialLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = HangugColors.Primary) }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = HangugColors.TextSecondary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text("Try again", color = HangugColors.Secondary)
            }
        }
    }
}
