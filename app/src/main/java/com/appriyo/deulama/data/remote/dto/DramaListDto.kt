package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * `data` payload of `GET /api/dramas`. Wraps the page's items plus the
 * pagination block the PagingSource uses to compute `nextKey`.
 */
@Serializable
data class DramaListDto(
    val dramas: List<DramaDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
)

/**
 * Pagination metadata from the catalog list endpoint.
 *
 *  - `has_next` drives Paging 3's `nextKey` (null when no more pages).
 *  - `total` / `total_pages` are kept on the wire so future features
 *    (e.g. "X of Y" footers) can read them without an extra request.
 */
@Serializable
data class PaginationDto(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val total_pages: Int = 0,
    val has_next: Boolean = false,
    val has_prev: Boolean = false,
)
