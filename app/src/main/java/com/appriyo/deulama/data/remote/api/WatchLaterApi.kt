package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.Envelope
import com.appriyo.deulama.data.remote.dto.EnvelopeRaw
import com.appriyo.deulama.data.remote.dto.EngagementRequestDto
import com.appriyo.deulama.data.remote.dto.WatchLaterListDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * `POST /api/watch-later` and `DELETE /api/watch-later/{drama_id}`.
 * Same idempotency contract as favorites (see [FavoritesApi]).
 *
 * `GET /api/watch-later` (Phase 7) feeds the Activity Timeline.
 */
interface WatchLaterApi {

    @POST("api/watch-later")
    suspend fun add(@Body body: EngagementRequestDto): EnvelopeRaw

    @DELETE("api/watch-later/{drama_id}")
    suspend fun remove(@Path("drama_id") dramaId: Int): EnvelopeRaw

    @GET("api/watch-later")
    suspend fun list(): Envelope<WatchLaterListDto>
}