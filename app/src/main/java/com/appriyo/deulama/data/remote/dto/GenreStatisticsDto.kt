package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * `data` payload of `GET /api/profile/genre-statistics`.
 *
 * Per api.md the server pre-computes per-genre preference scores
 * (`likes +5`, `watched +2`, `disliked -3`, clamped at 0) and the
 * client MUST display them as-is — **do not** try to recompute or
 * alter the formula here. This DTO captures every field the API
 * returns so the UI can show breakdowns for any of them.
 *
 * A brand-new user receives `statistics: []` and zero-valued
 * [ActivityTotals].
 */
@Serializable
data class GenreStatisticsResponseDto(
    val statistics: List<GenreStatisticDto> = emptyList(),
    val totals: ActivityTotalsDto = ActivityTotalsDto(),
)

/**
 * One row in [GenreStatisticsResponseDto.statistics].
 *
 * All counters are nullable defensively so older server payloads
 * that omit a given field don't crash the UI — the screen renders
 * "—" for missing values.
 */
@Serializable
data class GenreStatisticDto(
    val genre: String,
    val score: Int = 0,
    val liked: Int? = null,
    val watched: Int? = null,
    val disliked: Int? = null,
)

/** Activity totals across all genres (liked / disliked / watched counts). */
@Serializable
data class ActivityTotalsDto(
    val liked: Int = 0,
    val disliked: Int = 0,
    val watched: Int = 0,
)
