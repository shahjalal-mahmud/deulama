package com.appriyo.deulama.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.presentation.components.EmptyState
import com.appriyo.deulama.presentation.components.SectionHeader
import com.appriyo.deulama.ui.theme.HangugBrandGradient
import com.appriyo.deulama.ui.theme.HangugColors

@Composable
fun RecommendationList(
    items: List<Drama>,
    loading: Boolean,
    onDramaClick: (Drama) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        SectionHeader(
            eyebrow = "맞춤 추천 · FOR YOU",
            title = "Top Picks For You",
            subtitle = "Top picks to get you started",
        )

        if (!loading && items.isEmpty()) {
            EmptyState(
                title = "Like a few dramas to unlock picks",
                description = "Your recommendations get sharper the more you rate.",
            )
            return
        }

        Column {
            items.forEach { drama ->
                PickRow(drama, onClick = { onDramaClick(drama) })
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun PickRow(drama: Drama, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HangugColors.SurfaceContainer, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        val img = drama.posterUrl
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .width(84.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(HangugBrandGradient),
        ) {
            if (!img.isNullOrBlank()) {
                AsyncImage(model = img, contentDescription = drama.title, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f), contentScale = ContentScale.Crop)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (drama.imdbRating != null) {
                    Icon(Icons.Filled.Star, null, tint = HangugColors.Secondary, modifier = Modifier.height(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("%.1f".format(drama.imdbRating), style = MaterialTheme.typography.labelSmall, color = HangugColors.TextSecondary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(drama.releaseYear, style = MaterialTheme.typography.labelSmall, color = HangugColors.TextTertiary)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = drama.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = HangugColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row {
                drama.genres.take(2).forEach { g ->
                    Text(
                        text = "  $g  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = HangugColors.TextSecondary,
                        modifier = Modifier
                            .background(HangugColors.SurfaceContainerLowest, RoundedCornerShape(6.dp))
                            .padding(end = 6.dp),
                    )
                }
            }
        }
    }
}