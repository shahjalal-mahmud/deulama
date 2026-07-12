package com.appriyo.deulama.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.ui.theme.HangugColors

/**
 * The "match" pill shown on recommendation cards.
 *
 * IMPORTANT: per api.md, internal recommendation scores are **never**
 * exposed by the API. We render a categorical badge
 * ([MatchScoreKind]) instead of a numeric value — the kind comes
 * straight from the response's `is_personalized` / `fallback` flags
 * and the item's index in the list.
 *
 *  - [MatchScoreKind.TOP_PICK]    — gold pill, "Top Pick #1".
 *  - [MatchScoreKind.FOR_YOU]     — rose pill, "For You".
 *  - [MatchScoreKind.TRENDING]    — neutral pill, "Trending".
 */
enum class MatchScoreKind { TOP_PICK, FOR_YOU, TRENDING }

@Composable
fun MatchScoreBadge(
    kind: MatchScoreKind,
    rank: Int? = null,
    modifier: Modifier = Modifier,
) {
    val (bg, fg, label) = when (kind) {
        MatchScoreKind.TOP_PICK -> Triple(
            HangugColors.SecondaryContainer.copy(alpha = 0.6f),
            HangugColors.Secondary,
            "Top Pick" + (rank?.let { " #$it" } ?: ""),
        )
        MatchScoreKind.FOR_YOU -> Triple(
            HangugColors.PrimaryContainer.copy(alpha = 0.55f),
            HangugColors.Primary,
            "For You",
        )
        MatchScoreKind.TRENDING -> Triple(
            HangugColors.SurfaceContainerHigh,
            HangugColors.TextSecondary,
            "Trending",
        )
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(PaddingValues(horizontal = 10.dp, vertical = 4.dp)),
    )
}

/**
 * Helper for the screen: maps an item's index + the set's flags to a
 * [MatchScoreKind]. Top three personalised picks get a rank badge,
 * everything else under personalization gets a plain "For You", and
 * fallback (cold-start) items get "Trending".
 */
fun recommendBadgeKind(rank: Int, isPersonalized: Boolean, isFallback: Boolean): MatchScoreKind =
    when {
        isPersonalized && rank <= TOP_PICK_COUNT -> MatchScoreKind.TOP_PICK
        isPersonalized -> MatchScoreKind.FOR_YOU
        else -> MatchScoreKind.TRENDING // covers fallback + cold-start
    }

/** Number of items that get a "Top Pick #N" label — the rest are just "For You". */
private const val TOP_PICK_COUNT = 3

/** Convenience accessor for screens that want the actual label text. */
@Suppress("unused")
fun MatchScoreKind.label(rank: Int? = null): String = when (this) {
    MatchScoreKind.TOP_PICK -> "Top Pick" + (rank?.let { " #$it" } ?: "")
    MatchScoreKind.FOR_YOU -> "For You"
    MatchScoreKind.TRENDING -> "Trending"
}

/**
 * Returns the [Color] used for the badge background — exposed so a
 * screen can tint the card border to match the badge.
 */
@Suppress("unused")
fun MatchScoreKind.tintColor(): Color = when (this) {
    MatchScoreKind.TOP_PICK -> HangugColors.Secondary
    MatchScoreKind.FOR_YOU -> HangugColors.Primary
    MatchScoreKind.TRENDING -> HangugColors.TextSecondary
}
