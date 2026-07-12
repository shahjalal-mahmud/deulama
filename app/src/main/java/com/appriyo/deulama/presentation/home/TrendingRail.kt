package com.appriyo.deulama.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.presentation.components.DramaCard
import com.appriyo.deulama.presentation.components.DramaCardVariant
import com.appriyo.deulama.presentation.components.SectionHeader
import com.appriyo.deulama.ui.theme.HangugColors

/** Trending Now — a poster rail with a rank badge, since this list is
 *  genuinely ordered (unlike the other genre rails, which aren't). */
@Composable
fun TrendingRail(
    items: List<Drama>,
    loading: Boolean,
    onDramaClick: (Drama) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(bottom = 8.dp)) {}
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        SectionHeader(
            eyebrow = "지금 인기 · TRENDING NOW",
            title = "Trending Now",
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        ) {
            if (loading && items.isEmpty()) {
                items(6) { RailSkeleton() }
            } else {
                items(items, key = { it.dramaId }) { drama ->
                    Box {
                        DramaCard(drama = drama, onClick = { onDramaClick(drama) }, variant = DramaCardVariant.POSTER)
                        RankBadge(rank = items.indexOf(drama) + 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    Box(
        modifier = Modifier
            .padding(6.dp)
            .background(HangugColors.SurfaceContainerLowest.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("#$rank", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = HangugColors.Secondary)
    }
}

@Composable
private fun RailSkeleton() {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .background(HangugColors.SurfaceContainer, RoundedCornerShape(14.dp))
            .width(140.dp)
            .height(230.dp),
    )
}