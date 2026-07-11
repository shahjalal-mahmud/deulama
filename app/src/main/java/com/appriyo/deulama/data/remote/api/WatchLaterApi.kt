package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.EnvelopeRaw
import com.appriyo.deulama.data.remote.dto.EngagementRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * `POST /api/watch-later` and `DELETE /api/watch-later/{drama_id}`.
 * Same idempotency contract as favorites (see [FavoritesApi]).
 */
interface WatchLaterApi {

    @POST("api/watch-later")
    suspend fun add(@Body body: EngagementRequestDto): EnvelopeRaw

    @DELETE("api/watch-later/{drama_id}")
    suspend fun remove(@Path("drama_id") dramaId: Int): EnvelopeRaw
}
