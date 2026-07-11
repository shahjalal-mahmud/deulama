package com.appriyo.deulama.domain.repository

import androidx.paging.PagingData
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.Drama
import kotlinx.coroutines.flow.Flow

/**
 * Catalog + details operations the presentation layer is allowed to
 * call. All endpoints are public per api.md so no auth is involved —
 * this stays a thin wrapper over [com.appriyo.deulama.data.remote.api.DramaApi].
 */
interface DramaRepository {

    /**
     * Paginated catalog sorted by [sort] in [order]. Returns a cold
     * `Flow<PagingData<Drama>>` so the Paging 3 Compose extension can
     * drive a `LazyColumn` / `LazyVerticalGrid` with built-in
     * scroll-to-load-more.
     */
    fun pagedCatalog(
        sort: DramaSort = DramaSort.CREATED_AT,
        order: SortOrder = SortOrder.DESC,
    ): Flow<PagingData<Drama>>

    /**
     * GET /api/dramas/{id}. Returns [ApiResult.Success] with the
     * drama, or a typed failure the UI layer renders as an error
     * state (ValidationError / Error / NetworkError).
     */
    suspend fun dramaDetails(id: Int): ApiResult<Drama>
}

/**
 * Hardcoded whitelist of the four columns the API accepts for the
 * `sort` query parameter. Anything else returns 422 from the server,
 * so exposing this as an enum keeps callers from sending free-text
 * values by accident.
 */
enum class DramaSort(val wire: String) {
    TITLE("title"),
    RELEASE_YEAR("release_year"),
    IMDB_RATING("imdb_rating"),
    CREATED_AT("created_at"),
}

/** Hardcoded whitelist of the two values the API accepts for `order`. */
enum class SortOrder(val wire: String) {
    ASC("asc"),
    DESC("desc"),
}
