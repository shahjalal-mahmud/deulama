package com.appriyo.deulama.domain.repository

import androidx.paging.PagingData
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.Drama
import kotlinx.coroutines.flow.Flow

interface DramaRepository {

    fun pagedCatalog(
        sort: DramaSort = DramaSort.CREATED_AT,
        order: SortOrder = SortOrder.DESC,
    ): Flow<PagingData<Drama>>

    /**
     * Single-shot, non-paged fetch capped at [limit] — for homepage
     * rails (spotlight, trending, per-genre, "all dramas" preview)
     * where we want a fixed shelf, not infinite scroll. Mirrors the
     * web's `dramasApi.listDramas({ limit, sort, order, genre })`.
     *
     * [genre] is matched server-side against the drama's genre list;
     * pass null for no filter.
     */
    suspend fun listDramas(
        limit: Int,
        sort: DramaSort = DramaSort.IMDB_RATING,
        order: SortOrder = SortOrder.DESC,
        genre: String? = null,
    ): ApiResult<List<Drama>>

    suspend fun dramaDetails(id: Int): ApiResult<Drama>
}

enum class DramaSort(val wire: String) {
    TITLE("title"),
    RELEASE_YEAR("release_year"),
    IMDB_RATING("imdb_rating"),
    CREATED_AT("created_at"),
}

enum class SortOrder(val wire: String) {
    ASC("asc"),
    DESC("desc"),
}