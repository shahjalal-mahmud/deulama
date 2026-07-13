package com.appriyo.deulama.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.appriyo.deulama.data.mapper.toDomain
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.remote.api.DramaApi
import com.appriyo.deulama.data.remote.dto.DramaListDto
import com.appriyo.deulama.data.remote.map
import com.appriyo.deulama.data.remote.safeApiCall
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.domain.repository.DramaRepository
import com.appriyo.deulama.domain.repository.DramaSort
import com.appriyo.deulama.domain.repository.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

/**
 * Concrete [DramaRepository] backed by Retrofit [DramaApi].
 *
 * Paging is driven by [DramaPagingSource] — the source's `load`
 * function wraps each `listDramas(...)` call through [safeApiCall] so
 * network / validation failures become `LoadResult.Error` instead of
 * uncaught exceptions.
 */
class DramaRepositoryImpl(
    private val dramaApi: DramaApi,
    private val json: Json,
) : DramaRepository {

    override fun pagedCatalog(
        sort: DramaSort,
        order: SortOrder,
    ): Flow<PagingData<Drama>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = INITIAL_LOAD_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = {
            DramaPagingSource(
                api = dramaApi,
                json = json,
                sort = sort,
                order = order,
            )
        },
    ).flow

    override suspend fun listDramas(
        limit: Int,
        sort: DramaSort,
        order: SortOrder,
        genre: String?,
    ): ApiResult<List<Drama>> = safeApiCall(json) {
        dramaApi.listDramas(
            page = 1,  // Always fetch first page for fixed-shelf displays
            limit = limit.coerceIn(1, MAX_LIMIT),
            sort = sort.wire,
            order = order.wire,
            genre = genre,
        )
    }.map { dto -> dto.dramas.map { drama -> drama.toDomain() } }

    override suspend fun dramaDetails(id: Int): ApiResult<Drama> {
        if (id < MIN_DRAMA_ID) {
            // Per api.md, id must be >= 1. Return a typed validation
            // error rather than wasting a round-trip on a guaranteed
            // 422.
            return ApiResult.ValidationError(
                fieldErrors = mapOf("id" to listOf("Drama id must be >= 1.")),
            )
        }
        return when (val result = safeApiCall(json) { dramaApi.getDrama(id) }) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain())
            is ApiResult.ValidationError -> result
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    companion object {
        /** Default page size for the catalog. Sized to fetch the full
         *  newest-first list in a single round-trip so the home screen
         *  shows every drama without triggering paginated reloads. */
        private const val PAGE_SIZE = 50

        /** First-load window. Matches [PAGE_SIZE] so the initial fetch
         *  covers the entire catalog (up to [MAX_LIMIT] items) in one
         *  request and subsequent scrolls don't need to page. */
        private const val INITIAL_LOAD_SIZE = 50

        /** How many items before the end we pre-fetch the next page. */
        private const val PREFETCH_DISTANCE = 6

        /** Max limit per API spec. Used to clamp listDramas requests. */
        private const val MAX_LIMIT = 100

        /** Floor enforced by api.md on the `id` path parameter. */
        private const val MIN_DRAMA_ID = 1
    }
}

/**
 * Bridges the Retrofit-based [DramaApi] into a Paging 3 [PagingSource].
 *
 * Returned lists are mapped to domain [Drama]s at the boundary so the
 * rest of the app only deals with the domain model.
 */
internal class DramaPagingSource(
    private val api: DramaApi,
    private val json: Json,
    private val sort: DramaSort,
    private val order: SortOrder,
) : PagingSource<Int, Drama>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Drama> {
        val page = (params.key ?: STARTING_PAGE).coerceAtLeast(STARTING_PAGE)
        val limit = params.loadSize.coerceIn(1, MAX_LIMIT)
        return when (
            val result = safeApiCall(json) {
                api.listDramas(
                    page = page,
                    limit = limit,
                    sort = sort.wire,
                    order = order.wire,
                    genre = null,
                )
            }
        ) {
            is ApiResult.Success -> result.value.toLoadResult(page)
            is ApiResult.ValidationError -> LoadResult.Error(
                IllegalArgumentException("Invalid catalog query: ${result.fieldErrors}"),
            )
            is ApiResult.Error -> LoadResult.Error(
                DramaApiException(result.httpStatus, result.message, result.code),
            )
            is ApiResult.NetworkError -> LoadResult.Error(result.cause)
        }
    }

    /**
     * Lets Paging 3 know what the *previous* page's key was, based on
     * the response we just got. Empty when we're on page 1.
     */
    override fun getRefreshKey(state: androidx.paging.PagingState<Int, Drama>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }

    private fun DramaListDto.toLoadResult(currentPage: Int): LoadResult.Page<Int, Drama> {
        val items = dramas.map { it.toDomain() }
        val nextKey = if (pagination.has_next) currentPage + 1 else null
        val prevKey = if (pagination.has_prev) currentPage - 1 else null
        return LoadResult.Page(
            data = items,
            prevKey = prevKey,
            nextKey = nextKey,
        )
    }

    companion object {
        private const val STARTING_PAGE = 1
        /** Mirrors the API's MAX_LIMIT — see api.md. */
        private const val MAX_LIMIT = 100
    }
}

/**
 * Lightweight exception the PagingSource throws on non-2xx HTTP
 * responses. Carries the status + optional domain error code so the UI
 * can render a useful empty-state message ("Couldn't load more —
 * server error (HTTP 500)").
 */
internal class DramaApiException(
    val httpStatus: Int,
    val userMessage: String,
    val errorCode: String?,
) : RuntimeException("Catalog HTTP $httpStatus: $userMessage")