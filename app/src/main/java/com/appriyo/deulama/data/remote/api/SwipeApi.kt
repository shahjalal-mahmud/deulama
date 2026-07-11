package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.Envelope
import com.appriyo.deulama.data.remote.dto.SwipeRecordDto
import com.appriyo.deulama.data.remote.dto.SwipeRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * `POST /api/swipe` — upserts (user_id, drama_id) on the server.
 *
 * Both `200 OK` (re-swipe / same type) and `201 Created` (first swipe)
 * are success states; the [safeApiCall] helper translates them to
 * [ApiResult.Success] identically. Don't try to branch on which code
 * came back — the row data is the same either way.
 */
interface SwipeApi {

    @POST("api/swipe")
    suspend fun recordSwipe(@Body body: SwipeRequestDto): Envelope<SwipeRecordDto>
}
