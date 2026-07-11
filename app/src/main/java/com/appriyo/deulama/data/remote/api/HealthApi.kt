package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.Envelope
import com.appriyo.deulama.data.remote.dto.HealthDto
import retrofit2.http.GET

interface HealthApi {
    @GET("api/health")
    suspend fun health(): Envelope<HealthDto>
}