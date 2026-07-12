package com.appriyo.deulama.presentation.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.appriyo.deulama.data.local.datastore.AppPrefs
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.presentation.components.DramaCard
import com.appriyo.deulama.presentation.components.DramaCardVariant
import com.appriyo.deulama.ui.theme.HangugColors
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import androidx.compose.runtime.collectAsState

@Composable
fun DiscoverScreen(
    onOpenDramaDetails: (Int) -> Unit,
    discoverViewModel: DiscoverViewModel = koinViewModel(),
    deckViewModel: SwipeDeckViewModel = koinViewModel(),
    appPrefs: AppPrefs = koinInject(),
) {
    val items = discoverViewModel.catalog.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(deckViewModel) {
        deckViewModel.events.collect { event ->
            when (event) {
                is DeckEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val coachSeen by appPrefs.swipeCoachMarkSeen.collectAsStateWithLifecycle(initialValue = false)
    val dismissCoach: () -> Unit = {
        scope.launch { appPrefs.markSwipeCoachMarkSeen() }
    }

    var activeIndex by remember { mutableIntStateOf(0) }

    val activeDrama by remember(items, activeIndex) {
        derivedStateOf {
            if (activeIndex < items.itemCount) items[activeIndex] else null
        }
    }
    val behindDramas by remember(items, activeIndex) {
        derivedStateOf {
            (1..2).mapNotNull { i ->
                val idx = activeIndex + i
                if (idx < items.itemCount) items.peek(idx) else null
            }
        }
    }

    val controller = rememberDeckController()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Header()

            SwipeCoachMark(visible = !coachSeen, onDismiss = dismissCoach)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    items.loadState.refresh is LoadState.Loading &&
                        items.itemCount == 0 -> InitialLoading()
                    items.loadState.refresh is LoadState.Error &&
                        items.itemCount == 0 -> ErrorState(
                        message = (items.loadState.refresh as LoadState.Error)
                            .error.localizedMessage ?: "Couldn't load the catalog.",
                        onRetry = { items.retry() },
                    )
                    activeDrama == null &&
                        items.loadState.append.endOfPaginationReached ->
                        EmptyState()
                    else -> SwipeDeck(
                        activeDrama = activeDrama,
                        behindDramas = behindDramas,
                        onDismiss = { action, drama ->
                            deckViewModel.onActionConfirmed(action, drama)
                            activeIndex += 1
                        },
                        controller = controller,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            DeckActionRow(
                onAction = { action ->
                    val current = activeDrama ?: return@DeckActionRow
                    controller.triggerFlyOff(action) {
                        deckViewModel.onActionConfirmed(action, current)
                        activeIndex += 1
                    }
                },
                activeDramaId = activeDrama?.dramaId,
                enabled = activeDrama != null && !deckViewModel.state.collectAsState().value.isAnimating,
            )

            Spacer(Modifier.height(8.dp))

            UpcomingRow(
                items = items,
                activeIndex = activeIndex,
                onTap = onOpenDramaDetails,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            text = "지금 발견 · DISCOVER",
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.Secondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Swipe through the catalog",
            style = MaterialTheme.typography.headlineSmall,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun UpcomingRow(
    items: LazyPagingItems<Drama>,
    activeIndex: Int,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // SAFE: Check if we have enough items before trying to access them
    val upcoming = remember(items, activeIndex) {
        val startIndex = activeIndex + 3
        val endIndex = activeIndex + 5

        // Only try to access indices if we have enough items
        if (items.itemCount > startIndex) {
            (startIndex..endIndex).mapNotNull { i ->
                // Safe peek - only access if index exists
                if (i < items.itemCount) items.peek(i) else null
            }
        } else {
            emptyList()
        }
    }

    if (upcoming.isEmpty()) {
        Spacer(Modifier.height(48.dp))
        return
    }

    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            count = upcoming.size,
            key = { idx -> upcoming[idx].dramaId },
        ) { idx ->
            val drama = upcoming[idx]
            DramaCard(
                drama = drama,
                onClick = { onTap(drama.dramaId) },
                variant = DramaCardVariant.COMPACT,
            )
        }
    }
}

@Composable
private fun InitialLoading() {
    CircularProgressIndicator(color = HangugColors.Primary)
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = HangugColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text("Try again", color = HangugColors.Secondary)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "You've seen them all",
            style = MaterialTheme.typography.titleMedium,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "That's everything in the catalog right now. Check back when we add more!",
            style = MaterialTheme.typography.bodyMedium,
            color = HangugColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
