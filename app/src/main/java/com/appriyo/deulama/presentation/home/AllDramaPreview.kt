package com.appriyo.deulama.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.presentation.components.DramaCard
import com.appriyo.deulama.presentation.components.DramaCardVariant
import com.appriyo.deulama.presentation.components.SectionHeader
import com.appriyo.deulama.ui.theme.HangugColors

/**
 * Bounded preview (2 rows) + "View All" — deliberately NOT the web's
 * infinite Load More. A LazyVerticalGrid nested inside Home's outer
 * LazyColumn would need unbounded height and breaks scroll performance;
 * a fixed preview that hands off to the real paginated Discover screen
 * is the correct native pattern here.
 */
@Composable
fun AllDramaPreview(
    items: List<Drama>,
    loading: Boolean,
    onDramaClick: (Drama) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        SectionHeader(eyebrow = "전체 목록 · BROWSE ALL", title = "All Dramas")

        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { drama ->
                    DramaCard(
                        drama = drama,
                        onClick = { onDramaClick(drama) },
                        variant = DramaCardVariant.POSTER,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = onViewAll,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HangugColors.SurfaceContainer, contentColor = HangugColors.TextPrimary),
        ) {
            Text("Browse Full Library")
        }
    }
}