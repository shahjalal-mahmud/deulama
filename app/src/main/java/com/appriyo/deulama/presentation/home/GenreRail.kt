package com.appriyo.deulama.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.presentation.components.DramaCard
import com.appriyo.deulama.presentation.components.DramaCardVariant
import com.appriyo.deulama.presentation.components.SectionHeader
import com.appriyo.deulama.ui.theme.HangugColors

/** One curated genre shelf. Mirrors the web's GenreRow: skip rendering
 *  entirely once loaded if the genre came back empty — no dead shelves. */
@Composable
fun GenreRail(
    section: GenreSection,
    onDramaClick: (Drama) -> Unit,
    onViewAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (!section.loading && section.items.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(
            eyebrow = "${section.labelKo} · ${section.labelEn.uppercase()}",
            title = section.labelEn,
            actionLabel = onViewAll?.let { "View All" },
            onActionClick = onViewAll,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            if (section.loading) {
                items(6) {
                    Box(
                        modifier = Modifier
                            .background(HangugColors.SurfaceContainer, RoundedCornerShape(14.dp))
                            .width(140.dp)
                            .height(230.dp),
                    )
                }
            } else {
                items(section.items, key = { it.dramaId }) { drama ->
                    DramaCard(drama = drama, onClick = { onDramaClick(drama) }, variant = DramaCardVariant.POSTER)
                }
            }
        }
    }
}