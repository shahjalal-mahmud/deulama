package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.Envelope
import com.appriyo.deulama.data.remote.dto.EnvelopeRaw
import com.appriyo.deulama.data.remote.dto.EngagementRequestDto
import com.appriyo.deulama.data.remote.dto.FavoritesListDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * `POST /api/favorites` and `DELETE /api/favorites/{drama_id}`.
 *
 * The POST endpoint returns `201` on first add and `409 Conflict` if
 * the drama is already favorited. Both are valid outcomes from the
 * client's perspective (favorite ends up set either way) — the
 * repository treats `409` as soft-success / idempotent so the
 * optimistic UI doesn't fire an error toast for an accidental
 * double-tap. The DELETE returns `200` on success and `404` when the
 * drama wasn't favorited; `404` is also treated as soft-success.
 *
 * `GET /api/favorites` (Phase 7 — Activity Timeline) lists every
 * favorite with an embedded `drama` object — the activity feed merges
 * this with watch-later/watched entries on the client.
 */
interface FavoritesApi {

    @POST("api/favorites")
    suspend fun add(@Body body: EngagementRequestDto): EnvelopeRaw

    @DELETE("api/favorites/{drama_id}")
    suspend fun remove(@Path("drama_id") dramaId: Int): EnvelopeRaw

    @GET("api/favorites")
    suspend fun list(): Envelope<FavoritesListDto>
}