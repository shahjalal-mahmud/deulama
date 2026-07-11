package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.AuthResponseDto
import com.appriyo.deulama.data.remote.dto.Envelope
import com.appriyo.deulama.data.remote.dto.LoginRequest
import com.appriyo.deulama.data.remote.dto.RegisterRequest
import com.appriyo.deulama.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Auth + identity endpoints. Both register and login return an
 * `Envelope<AuthResponseDto>` whose `data` carries the new user and
 * the JWT we need to store.
 *
 * GET /api/me is the JWT-protected call used to refresh the cached
 * user (e.g. after profile edits) — the AuthInterceptor attaches the
 * Bearer header automatically.
 */
interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Envelope<AuthResponseDto>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Envelope<AuthResponseDto>

    @GET("api/me")
    suspend fun me(): Envelope<UserDto>
}