package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.Envelope
import com.appriyo.deulama.data.remote.dto.GenreStatisticsResponseDto
import retrofit2.http.GET

/**
 * `GET /api/profile/genre-statistics` — JWT-protected. Returns the
 * server-computed per-genre preference scores (`score = likes*5 +
 * watched*2 − dislikes*3`, clamped at 0) plus activity totals.
 *
 * Per api.md the client MUST display these scores as-is — the scoring
 * formula is server-side and intentionally opaque.
 */
interface ProfileStatsApi {

    @GET("api/profile/genre-statistics")
    suspend fun genreStatistics(): Envelope<GenreStatisticsResponseDto>
}
