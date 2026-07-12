package com.appriyo.deulama.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.presentation.components.DramaCard
import com.appriyo.deulama.presentation.components.DramaCardVariant
import com.appriyo.deulama.presentation.components.SectionHeader
import com.appriyo.deulama.ui.theme.HangugColors

/**
 * Full newest-first catalog rendered as a 2-column vertical list.
 *
 * Why not a nested `LazyColumn`? Compose forbids a vertically-scrolling
 * `LazyColumn` inside another vertically-scrolling `LazyColumn`, and the
 * home screen's outer list is one. Instead this composable is rendered
 * as a single `item { ... }` slot inside that outer list and uses a
 * plain `Column { Row, Row, ... }` to lay out whichever dramas the
 * Paging 3 stream has produced so far. As `items.itemCount` grows (a
 * new page arrives) Compose re-runs this body, so new rows appear at
 * the bottom — that's our "load more on scroll" behaviour without the
 * scroll-handoff problems of nested LazyContainers.
 *
 * The "Browse Full Library" hand-off button previously lived here, but
 * with the entire catalog shown in place there's no need to route the
 * user to a separate screen.
 */
@Composable
fun AllDramaPreview(
    items: LazyPagingItems<Drama>,
    onDramaClick: (Drama) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        SectionHeader(eyebrow = "전체 목록 · ALL DRAMAS", title = "All Dramas")

        when {
            // Initial load still in flight and we have nothing to show.
            items.loadState.refresh is LoadState.Loading && items.itemCount == 0 -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 24.dp),
                    color = HangugColors.Primary,
                )
            }

            // Initial load failed entirely and we have nothing cached.
            items.loadState.refresh is LoadState.Error && items.itemCount == 0 -> {
                val error = items.loadState.refresh as LoadState.Error
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = error.error.localizedMessage ?: "Couldn't load the catalog.",
                        color = HangugColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = { items.retry() }) {
                        Text("Try again", color = HangugColors.Secondary)
                    }
                }
            }

            else -> {
                // `peek` does NOT trigger a load on a missing index —
                // important inside this recomposing layout because
                // Compose will run this body whenever `itemCount`
                // grows and we don't want a thundering herd of
                // get()-calls re-issuing the same in-flight request.
                val loaded = (0 until items.itemCount).mapNotNull { items.peek(it) }

                loaded.chunked(2).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        rowItems.forEach { drama ->
                            DramaCard(
                                drama = drama,
                                onClick = { onDramaClick(drama) },
                                variant = DramaCardVariant.POSTER,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Odd catalog size — keep the second column empty
                        // so the lone card stays left-aligned instead of
                        // stretching to fill the row.
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Footer reflects append-state so the user knows what's
                // happening as the next page is in flight.
                val append = items.loadState.append
                when {
                    append is LoadState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 12.dp),
                            color = HangugColors.Primary,
                        )
                    }
                    append is LoadState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = append.error.localizedMessage ?: "Couldn't load more.",
                                color = HangugColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                            TextButton(onClick = { items.retry() }) {
                                Text("Try again", color = HangugColors.Secondary)
                            }
                        }
                    }
                    append is LoadState.NotLoading && append.endOfPaginationReached && loaded.isNotEmpty() -> {
                        Text(
                            text = "You've reached the end · ${loaded.size} dramas",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            color = HangugColors.TextTertiary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}