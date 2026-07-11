package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.EnvelopeRaw
import com.appriyo.deulama.data.remote.dto.EngagementRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * `POST /api/watched`. Per api.md there is **no** DELETE — once a
 * drama is watched it stays watched. `409` means already-watched;
 * repository treats it as a soft-success.
 */
interface WatchedApi {

    @POST("api/watched")
    suspend fun markWatched(@Body body: EngagementRequestDto): EnvelopeRaw
}
