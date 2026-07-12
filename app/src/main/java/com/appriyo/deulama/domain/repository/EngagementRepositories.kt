package com.appriyo.deulama.domain.repository

import com.appriyo.deulama.data.remote.ApiResult
import kotlinx.coroutines.flow.Flow

/**
 * Favorites, watch-later, watched — Phase-4 contract.
 *
 *  - `add` / `remove` (or `markWatched` for watched) mutate **optimistically**:
 *    the Room queue is updated first so the UI reacts instantly, then
 *    the API call is reconciled. On non-idempotent failure the local
 *    row is rolled back and a snackbar is surfaced.
 *  - Anonymous users: actions are queued to Room only and never call
 *    the network. Sync-on-login replays them.
 *  - `isFavorited(dramaId)` etc. are **Flows over Room state** so the
 *    UI is always reading from the same store it's optimistically
 *    writing to — no risk of stale-screen / fresh-state skew.
 *  - `409` (already applied) is treated as soft-success everywhere.
 */
interface FavoritesRepository {
    suspend fun add(dramaId: Int): ApiResult<Unit>
    suspend fun remove(dramaId: Int): ApiResult<Unit>

    /** Cold flow of "is this drama currently favorited?". */
    fun isFavorited(dramaId: Int): Flow<Boolean>
}

/**
 * Same idempotency contract as [FavoritesRepository].
 */
interface WatchLaterRepository {
    suspend fun add(dramaId: Int): ApiResult<Unit>
    suspend fun remove(dramaId: Int): ApiResult<Unit>

    fun isQueued(dramaId: Int): Flow<Boolean>
}

/**
 * `POST /api/watched` — there's intentionally **no** DELETE endpoint
 * (per api.md); once a drama is watched it stays watched. `409` is
 * treated as soft-success for the same idempotency reason. No un-watch
 * affordance exists in the UI.
 */
interface WatchedRepository {
    suspend fun markWatched(dramaId: Int): ApiResult<Unit>

    fun isMarkedWatched(dramaId: Int): Flow<Boolean>
}
