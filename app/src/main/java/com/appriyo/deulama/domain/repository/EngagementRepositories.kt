package com.appriyo.deulama.domain.repository

import com.appriyo.deulama.data.remote.ApiResult

/**
 * Add/remove a drama from the authenticated user's favorites.
 * 409 ("already favorited") and 404 ("not in favorites") are treated
 * as soft-success inside the implementation — the optimistic UI is
 * never punished for an idempotent tap-twice.
 */
interface FavoritesRepository {
    suspend fun add(dramaId: Int): ApiResult<Unit>
    suspend fun remove(dramaId: Int): ApiResult<Unit>
}

/**
 * Same idempotency contract as [FavoritesRepository].
 */
interface WatchLaterRepository {
    suspend fun add(dramaId: Int): ApiResult<Unit>
    suspend fun remove(dramaId: Int): ApiResult<Unit>
}

/**
 * `POST /api/watched` — there's intentionally **no** DELETE endpoint
 * (per api.md); once a drama is watched it stays watched. 409 is
 * treated as soft-success for the same idempotency reason.
 */
interface WatchedRepository {
    suspend fun markWatched(dramaId: Int): ApiResult<Unit>
}
