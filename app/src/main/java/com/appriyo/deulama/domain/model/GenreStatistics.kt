package com.appriyo.deulama.domain.model

/**
 * Server-computed genre preference snapshot returned by
 * `GET /api/profile/genre-statistics`. The client renders these
 * numbers verbatim — **never** recompute the scoring formula
 * client-side (see api.md).
 *
 * The list is already sorted by the server (highest score first);
 * the UI may rely on that ordering for its ranking.
 */
data class GenreStatistics(
    val statistics: List<GenreScore>,
    val totals: ActivityTotals,
)

/** One row of the genre breakdown. */
data class GenreScore(
    val genre: String,
    val score: Int,
    val liked: Int?,
    val watched: Int?,
    val disliked: Int?,
)

/** Cross-genre activity totals. */
data class ActivityTotals(
    val liked: Int,
    val disliked: Int,
    val watched: Int,
)
