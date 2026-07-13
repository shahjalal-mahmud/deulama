package com.appriyo.deulama.data.remote.api

import com.appriyo.deulama.data.remote.dto.DramaListDto
import com.appriyo.deulama.data.remote.dto.DramaSingleDto
import com.appriyo.deulama.data.remote.dto.Envelope
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Public catalog endpoints. Both routes are unauthenticated per
 * api.md (browse-before-login is intentional), so this interface does
 * NOT touch the auth token — the [AuthInterceptor] will simply add no
 * header when the SessionManager has no token stored.
 *
 *  - [listDramas] — paginated + sortable catalog, optionally filtered
 *                    by genre.
 *  - [getDrama]   — single drama details.
 */
interface DramaApi {

    /**
     * GET /api/dramas
     *
     * @param page   1-indexed page number; the caller is responsible for
     *               clamping to >= 1. Out-of-range values produce 422.
     * @param limit  page size in [1..100]; default 20. Caller must clamp.
     * @param sort   one of `title | release_year | imdb_rating | created_at`.
     *               Anything else produces 422 — never pass user input.
     * @param order  one of `asc | desc`. Same caveat as [sort].
     * @param genre  optional genre filter matched server-side against the
     *               drama's genre list. Retrofit omits the query param
     *               entirely when this is null, so passing null here is
     *               the same as not filtering.
     */
    @GET("api/dramas")
    suspend fun listDramas(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("sort") sort: String,
        @Query("order") order: String,
        @Query("genre") genre: String?,
    ): Envelope<DramaListDto>

    /**
     * GET /api/dramas/{id}
     *
     * Response body shape:
     *   { "success": true, "data": { "drama": { ...DramaDto fields... } } }
     *
     * Note that the single-drama endpoint wraps the inner object under a
     * `drama` key — different from the list endpoint which uses
     * `dramas: [...]`. That asymmetry is captured in [DramaSingleDto].
     *
     * @param id drama primary key. Must be `>= 1` — the repository
     *           guards against 0 / negative values before calling.
     */
    @GET("api/dramas/{id}")
    suspend fun getDrama(@Path("id") id: Int): Envelope<DramaSingleDto>
}