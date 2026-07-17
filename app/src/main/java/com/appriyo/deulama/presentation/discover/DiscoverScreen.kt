package com.appriyo.deulama.presentation.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            HangugColors.SurfaceContainerLowest,
                            HangugColors.SurfaceContainer.copy(alpha = 0.55f),
                        ),
                    ),
                )
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Header(remainingCount = (items.itemCount - activeIndex).coerceAtLeast(0))

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

                Spacer(Modifier.height(14.dp))

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

                Spacer(Modifier.height(10.dp))

                UpcomingRow(
                    items = items,
                    activeIndex = activeIndex,
                    onTap = onOpenDramaDetails,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(remainingCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "지금 발견 · DISCOVER",
                style = MaterialTheme.typography.labelSmall,
                color = HangugColors.Secondary,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Find your next binge",
                style = MaterialTheme.typography.headlineSmall,
                color = HangugColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(HangugColors.SurfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "$remainingCount left",
                    style = MaterialTheme.typography.labelMedium,
                    color = HangugColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
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

    Column(modifier = modifier) {
        Text(
            text = "UP NEXT",
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.TextTertiary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        androidx.compose.foundation.lazy.LazyRow(
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
}

@Composable
private fun InitialLoading() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(color = HangugColors.Primary, strokeWidth = 3.dp)
        Text(
            text = "Loading dramas…",
            style = MaterialTheme.typography.bodyMedium,
            color = HangugColors.TextSecondary,
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(HangugColors.Danger.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = HangugColors.Danger,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            color = HangugColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = HangugColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onRetry) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = HangugColors.Secondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Try again", color = HangugColors.Secondary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(HangugColors.Tertiary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MovieFilter,
                contentDescription = null,
                tint = HangugColors.Tertiary,
                modifier = Modifier.size(32.dp),
            )
        }
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