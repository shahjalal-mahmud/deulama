package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.Envelope
import com.appriyo.deulama.data.remote.dto.EnvelopeRaw
import com.appriyo.deulama.data.remote.dto.EngagementRequestDto
import com.appriyo.deulama.data.remote.dto.WatchedListDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * `POST /api/watched`. Per api.md there is **no** DELETE — once a
 * drama is watched it stays watched. `409` means already-watched;
 * repository treats it as a soft-success.
 *
 * `GET /api/watched` (Phase 7) feeds the Activity Timeline.
 */
interface WatchedApi {

    @POST("api/watched")
    suspend fun markWatched(@Body body: EngagementRequestDto): EnvelopeRaw

    @GET("api/watched")
    suspend fun list(): Envelope<WatchedListDto>
}