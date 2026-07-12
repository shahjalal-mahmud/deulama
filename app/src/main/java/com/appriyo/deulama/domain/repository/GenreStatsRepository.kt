package com.appriyo.deulama.domain.repository

import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.GenreStatistics

/**
 * `GET /api/profile/genre-statistics` — server-computed genre
 * preference snapshot.
 *
 * UI callers MUST render the values inside [ApiResult.Success.value]
 * as-is. The scoring formula is server-owned; recomputing it
 * client-side would risk drifting from the backend and is explicitly
 * disallowed by api.md.
 */
interface GenreStatsRepository {
    suspend fun genreStatistics(): ApiResult<GenreStatistics>
}
