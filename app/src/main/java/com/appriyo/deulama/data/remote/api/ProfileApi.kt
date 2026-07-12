package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.Envelope
import com.appriyo.deulama.data.remote.dto.ProfileResponseDto
import com.appriyo.deulama.data.remote.dto.ProfileUpdateRequestDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap

/**
 * User-profile endpoints (`/api/profile`). JWT-protected.
 *
 * `PUT /api/profile` accepts two content types:
 *
 * - `application/json` — name and/or password only. Use [updateProfileJson].
 * - `multipart/form-data` — any combination including an `image` file.
 *   Use [updateProfileMultipart].
 *
 * We expose two methods rather than one because Retrofit dispatches
 * based on the declared Content-Type; one method cannot legally take
 * "either" a `@Body` and a `@Part` simultaneously. The repository layer
 * picks the right method based on whether the caller is uploading an
 * image.
 *
 * GOTCHA — `default.png` avatar is never null on the wire. Do not code
 * a null-avatar fallback path on this client.
 */
interface ProfileApi {

    @GET("api/profile")
    suspend fun getProfile(): Envelope<ProfileResponseDto>

    @PUT("api/profile")
    suspend fun updateProfileJson(
        @Body body: ProfileUpdateRequestDto,
    ): Envelope<ProfileResponseDto>

    /**
     * Multipart variant. Text parts are sent as
     * `text/plain; charset=utf-8` RequestBody values (Retrofit treats
     * them as plain strings). The image part is supplied by the
     * caller as a fully prepared `MultipartBody.Part`.
     */
    @Multipart
    @PUT("api/profile")
    suspend fun updateProfileMultipart(
        @PartMap textParts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part,
    ): Envelope<ProfileResponseDto>
}
