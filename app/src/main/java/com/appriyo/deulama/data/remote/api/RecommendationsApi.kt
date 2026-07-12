package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.Envelope
import com.appriyo.deulama.data.remote.dto.RecommendationsResponseDto
import retrofit2.http.GET

/**
 * `GET /api/recommendations` — JWT-protected. Returns up to 10 dramas
 * in `Drama::publicItem()` shape plus `is_personalized` / `fallback`
 * flags so the UI can show the cold-start banner when needed.
 *
 * The endpoint exposes NO numeric score on the wire — it's a black
 * box from the client's point of view. The repository maps it to a
 * [com.appriyo.deulama.domain.model.RecommendationSet] with no
 * "match score" field on purpose.
 */
interface RecommendationsApi {

    @GET("api/recommendations")
    suspend fun recommendations(): Envelope<RecommendationsResponseDto>
}
