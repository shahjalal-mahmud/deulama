package com.appriyo.deulama.domain.repository

import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.RecommendationSet

/**
 * `GET /api/recommendations`.
 *
 * The returned [ApiResult.Success] wraps a [RecommendationSet] whose
 * `is_personalized` / `fallback` flags drive the cold-start banner
 * in the UI. The endpoint is JWT-protected so any [ApiResult.Error]
 * with `httpStatus == 401` is expected when the user is signed out —
 * callers should map that to a "log in to see picks" CTA rather than
 * treating it as a failure.
 */
interface RecommendationsRepository {
    suspend fun recommendations(): ApiResult<RecommendationSet>
}
