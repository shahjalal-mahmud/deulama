package com.appriyo.deulama.data.repository

import com.appriyo.deulama.data.mapper.toDomain
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.remote.api.ProfileStatsApi
import com.appriyo.deulama.data.remote.map
import com.appriyo.deulama.data.remote.safeApiCall
import com.appriyo.deulama.domain.model.GenreStatistics
import com.appriyo.deulama.domain.repository.GenreStatsRepository
import kotlinx.serialization.json.Json

/**
 * Thin wrapper over [ProfileStatsApi] that hands the UI the
 * server-computed [GenreStatistics] verbatim. We do **not** sort,
 * scale, or re-weight anything here — the server has already done
 * all of that and any client-side recompute would risk drifting
 * from the backend.
 */
class GenreStatsRepositoryImpl(
    private val api: ProfileStatsApi,
    private val json: Json,
) : GenreStatsRepository {

    override suspend fun genreStatistics(): ApiResult<GenreStatistics> =
        safeApiCall(json) { api.genreStatistics() }.map { it.toDomain() }
}
