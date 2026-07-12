package com.appriyo.deulama.data.repository

import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.local.db.dao.LocalFavoriteDao
import com.appriyo.deulama.data.local.db.dao.LocalSwipeDao
import com.appriyo.deulama.data.local.db.dao.LocalWatchLaterDao
import com.appriyo.deulama.data.local.db.dao.LocalWatchedDao
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.remote.api.FavoritesApi
import com.appriyo.deulama.data.remote.api.SwipeApi
import com.appriyo.deulama.data.remote.api.WatchLaterApi
import com.appriyo.deulama.data.remote.api.WatchedApi
import com.appriyo.deulama.data.remote.dto.EngagementRequestDto
import com.appriyo.deulama.data.remote.dto.SwipeRequestDto
import com.appriyo.deulama.data.remote.safeApiCall
import com.appriyo.deulama.data.remote.safeApiCallRaw
import com.appriyo.deulama.data.remote.treatAlreadyAppliedAsSuccess
import com.appriyo.deulama.domain.model.SwipeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Replays anon-mode engagement rows to the server the moment the user
 * signs in. Lives at app-scope (started from [com.appriyo.deulama.HangugDeulamaApp])
 * and observes [SessionManager.sessionFlow].
 *
 * Replay order is fixed per the phase-4 spec:
 *
 *   1. swipes
 *   2. favorites
 *   3. watch-later
 *   4. watched
 *
 * Within each table rows are replayed in `created_at ASC` order.
 *
 * Idempotency rule: **every successful replay deletes its local row.**
 * Anything that returns a non-success ApiResult other than the already-
 * applied collapse (`treatAlreadyAppliedAsSuccess`) leaves the row in
 * place so we retry on the next login.
 *
 * **No network calls are made for anonymous users.** The flow only
 * kicks when the session transitions to non-null.
 */
class EngagementSyncService(
    private val sessionManager: SessionManager,
    private val swipeApi: SwipeApi,
    private val favoritesApi: FavoritesApi,
    private val watchLaterApi: WatchLaterApi,
    private val watchedApi: WatchedApi,
    private val swipeDao: LocalSwipeDao,
    private val favoriteDao: LocalFavoriteDao,
    private val watchLaterDao: LocalWatchLaterDao,
    private val watchedDao: LocalWatchedDao,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    /** Exposed so the UI can show a "syncing..." banner if it wants. */
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun start() {
        scope.launch(Dispatchers.IO) {
            sessionManager.sessionFlow
                .map { it != null }
                .distinctUntilChanged()
                .collect { signedIn ->
                    if (signedIn) replayAll()
                }
        }
    }

    /** Manual trigger — also exposed for tests / future refresh-pull. */
    suspend fun replayAll() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        try {
            replaySwipes()
            replayFavorites()
            replayWatchLater()
            replayWatched()
        } finally {
            _isSyncing.value = false
        }
    }

    // ---- Swipe ---------------------------------------------------

    private suspend fun replaySwipes() {
        val rows = swipeDao.allOrdered()
        for (row in rows) {
            val type = if (row.swipe_type == SwipeType.LIKE.wire) SwipeType.LIKE else SwipeType.DISLIKE
            val result = safeApiCall(json) {
                swipeApi.recordSwipe(SwipeRequestDto(row.drama_id, type.wire))
            }
            // Both 200 (re-swipe) and 201 (first swipe) are success —
            // the endpoint upserts, so any 2xx means the desired state
            // is now reflected server-side. Remove the local row.
            if (result is ApiResult.Success) {
                swipeDao.deleteByDrama(row.drama_id)
            }
            // Any other result: leave the row in place. Next login
            // (or manual refresh) will retry. Network errors are
            // intentionally retryable so offline writes aren't lost.
        }
    }

    // ---- Favorites -----------------------------------------------

    private suspend fun replayFavorites() {
        val rows = favoriteDao.allOrdered()
        for (row in rows) {
            val result = safeApiCallRaw(json) {
                favoritesApi.add(EngagementRequestDto(row.drama_id))
            }.treatAlreadyAppliedAsSuccess()
            if (result is ApiResult.Success) {
                favoriteDao.deleteByDrama(row.drama_id)
            }
        }
    }

    // ---- Watch Later ---------------------------------------------

    private suspend fun replayWatchLater() {
        val rows = watchLaterDao.allOrdered()
        for (row in rows) {
            val result = safeApiCallRaw(json) {
                watchLaterApi.add(EngagementRequestDto(row.drama_id))
            }.treatAlreadyAppliedAsSuccess()
            if (result is ApiResult.Success) {
                watchLaterDao.deleteByDrama(row.drama_id)
            }
        }
    }

    // ---- Watched -------------------------------------------------

    private suspend fun replayWatched() {
        val rows = watchedDao.allOrdered()
        for (row in rows) {
            val result = safeApiCallRaw(json) {
                watchedApi.markWatched(EngagementRequestDto(row.drama_id))
            }.treatAlreadyAppliedAsSuccess()
            if (result is ApiResult.Success) {
                watchedDao.deleteByDrama(row.drama_id)
            }
        }
    }
}
