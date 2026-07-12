package com.appriyo.deulama.data.repository

import com.appriyo.deulama.data.mapper.toDomain
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.remote.api.RecommendationsApi
import com.appriyo.deulama.data.remote.map
import com.appriyo.deulama.data.remote.safeApiCall
import com.appriyo.deulama.domain.model.RecommendationSet
import com.appriyo.deulama.domain.repository.RecommendationsRepository
import kotlinx.serialization.json.Json

/**
 * Thin wrapper over [RecommendationsApi]. Maps the wire envelope into
 * a [RecommendationSet] domain model so the UI doesn't see snake_case
 * keys or have to interpret `is_personalized` / `fallback` itself.
 */
class RecommendationsRepositoryImpl(
    private val api: RecommendationsApi,
    private val json: Json,
) : RecommendationsRepository {

    override suspend fun recommendations(): ApiResult<RecommendationSet> =
        safeApiCall(json) { api.recommendations() }.map { it.toDomain() }
}
