package com.appriyo.deulama.data.mapper

import com.appriyo.deulama.data.remote.dto.GenreStatisticDto
import com.appriyo.deulama.data.remote.dto.GenreStatisticsResponseDto
import com.appriyo.deulama.data.remote.dto.RecommendationsResponseDto
import com.appriyo.deulama.domain.model.ActivityTotals
import com.appriyo.deulama.domain.model.GenreScore
import com.appriyo.deulama.domain.model.GenreStatistics
import com.appriyo.deulama.domain.model.RecommendationSet

/**
 * Pure DTO → domain conversions for the recommendations + genre
 * statistics endpoints. No validation; nullable wire fields stay
 * nullable so the UI can render placeholders.
 */
internal fun RecommendationsResponseDto.toDomain(): RecommendationSet = RecommendationSet(
    items = recommendations.map { it.toDomain() },
    isPersonalized = is_personalized,
    fallback = fallback,
)

internal fun GenreStatisticsResponseDto.toDomain(): GenreStatistics = GenreStatistics(
    statistics = statistics.map { it.toDomain() },
    totals = totals.toDomain(),
)

internal fun GenreStatisticDto.toDomain(): GenreScore = GenreScore(
    genre = genre,
    score = score,
    liked = liked,
    watched = watched,
    disliked = disliked,
)

internal fun com.appriyo.deulama.data.remote.dto.ActivityTotalsDto.toDomain(): ActivityTotals =
    ActivityTotals(
        liked = liked,
        disliked = disliked,
        watched = watched,
    )
