package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * `data` payload of `GET /api/recommendations`.
 *
 *  - [recommendations] — up to 10 dramas in the same shape as
 *    [DramaDto] (`Drama::publicItem()`); the API intentionally does
 *    NOT expose any internal "score" number, only the flags below.
 *  - [isPersonalized]  — `false` for a cold-start user (no swipes /
 *    favorites / watch-later / watched history yet).
 *  - [fallback]        — `true` when the server returned highest-rated
 *    dramas as a generic fallback. Both flags can technically be
 *    `false` if the user has activity but no personalization was
 *    computed; the UI just shows whatever came back.
 *
 * `count` is included in the wire so future header-banners can read
 * "Showing X picks" without a second round-trip.
 */
@Serializable
data class RecommendationsResponseDto(
    val recommendations: List<DramaDto> = emptyList(),
    val count: Int = 0,
    val is_personalized: Boolean = false,
    val fallback: Boolean = false,
)
