package com.appriyo.deulama.data.repository

import com.appriyo.deulama.data.local.db.dao.LocalFavoriteDao
import com.appriyo.deulama.data.local.db.dao.LocalWatchLaterDao
import com.appriyo.deulama.data.local.db.dao.LocalWatchedDao
import com.appriyo.deulama.data.local.db.entity.LocalFavoriteEntity
import com.appriyo.deulama.data.local.db.entity.LocalWatchLaterEntity
import com.appriyo.deulama.data.local.db.entity.LocalWatchedEntity
import com.appriyo.deulama.data.local.datastore.SessionManager
import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.remote.MIN_DRAMA_ID
import com.appriyo.deulama.data.remote.api.FavoritesApi
import com.appriyo.deulama.data.remote.api.WatchLaterApi
import com.appriyo.deulama.data.remote.api.WatchedApi
import com.appriyo.deulama.data.remote.dto.EngagementRequestDto
import com.appriyo.deulama.data.remote.safeApiCallRaw
import com.appriyo.deulama.data.remote.treatAlreadyAppliedAsSuccess
import com.appriyo.deulama.domain.repository.FavoritesRepository
import com.appriyo.deulama.domain.repository.WatchLaterRepository
import com.appriyo.deulama.domain.repository.WatchedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

/**
 * Engagement repositories, Phase-4 edition.
 *
 *  **Optimistic local update → reconcile with API response → rollback + snackbar on failure.**
 *
 *  - The Room queue is the **single source of truth** for the UI:
 *    every action toggles a row there first, the UI immediately reflects
 *    the change via Flow, then we attempt the API call.
 *  - When the user is **signed out** we *only* write to Room — never
 *    touch the network. Sync-on-login replays the queue later.
 *  - When the API returns a non-idempotent error (anything other than
 *    `409`/`404` already-applied, or `422` validation) we **rollback**
 *    the local row so the UI snaps back to its previous state, and
 *    emit a `SoftFailure` so the screen can show a snackbar.
 *  - `409` is treated as soft-success on **all** paths (manual taps
 *    and replay) — see [treatAlreadyAppliedAsSuccess].
 */

private fun invalidDramaIdError(): ApiResult<Unit> =
    ApiResult.ValidationError(mapOf("drama_id" to listOf("Drama id must be >= $MIN_DRAMA_ID.")))

/**
 * Snapshot of what a write operation tried to do. Used by the screen
 * to know what to rollback on hard failure (e.g. "I just added this
 * favorite locally, but the API rejected me — please undo").
 */
sealed interface EngagementOp {
    data class AddFavorite(val dramaId: Int) : EngagementOp
    data class RemoveFavorite(val dramaId: Int) : EngagementOp
    data class AddWatchLater(val dramaId: Int) : EngagementOp
    data class RemoveWatchLater(val dramaId: Int) : EngagementOp
    data class MarkWatched(val dramaId: Int) : EngagementOp
}

/**
 * Hook the UI binds to for soft-failure toasts. The repository fires
 * these on hard API failures so the screen can show a snackbar without
 * needing to reach back into the repo.
 */
interface EngagementFailureBus {
    fun publish(op: EngagementOp, message: String)
}

internal class NoOpFailureBus : EngagementFailureBus {
    override fun publish(op: EngagementOp, message: String) = Unit
}

class FavoritesRepositoryImpl(
    private val api: FavoritesApi,
    private val localDao: LocalFavoriteDao,
    private val sessionManager: SessionManager,
    private val json: Json,
    private val failureBus: EngagementFailureBus = NoOpFailureBus(),
) : FavoritesRepository {

    override suspend fun add(dramaId: Int): ApiResult<Unit> {
        if (dramaId < MIN_DRAMA_ID) return invalidDramaIdError()
        val isAuthed = sessionManager.currentSession() != null
        if (!isAuthed) {
            // Anonymous: write to Room only, no network call.
            localDao.upsert(
                LocalFavoriteEntity(
                    drama_id = dramaId,
                    created_at = System.currentTimeMillis(),
                ),
            )
            return ApiResult.Success(Unit)
        }
        // Signed-in: optimistic add — if the API call fails on something
        // that isn't "already applied" we rollback.
        localDao.upsert(
            LocalFavoriteEntity(
                drama_id = dramaId,
                created_at = System.currentTimeMillis(),
            ),
        )
        val result = safeApiCallRaw(json) { api.add(EngagementRequestDto(dramaId)) }
            .treatAlreadyAppliedAsSuccess()
        if (result !is ApiResult.Success) {
            localDao.deleteByDrama(dramaId)
            failureBus.publish(EngagementOp.AddFavorite(dramaId), messageFor(result))
        }
        return result
    }

    override suspend fun remove(dramaId: Int): ApiResult<Unit> {
        if (dramaId < MIN_DRAMA_ID) return invalidDramaIdError()
        val isAuthed = sessionManager.currentSession() != null
        if (!isAuthed) {
            // Anonymous users never had a delete affordance — but if a
            // signed-in row exists locally we still allow removal.
            localDao.deleteByDrama(dramaId)
            return ApiResult.Success(Unit)
        }
        // Optimistic: remove locally first, re-add on API failure.
        val wasLocal = localDao.isFavorited(dramaId)
        localDao.deleteByDrama(dramaId)
        val result = safeApiCallRaw(json) { api.remove(dramaId) }
            .treatAlreadyAppliedAsSuccess()
        if (result !is ApiResult.Success && wasLocal) {
            // Roll back: restore the previous local row.
            localDao.upsert(
                LocalFavoriteEntity(
                    drama_id = dramaId,
                    created_at = System.currentTimeMillis(),
                ),
            )
            failureBus.publish(EngagementOp.RemoveFavorite(dramaId), messageFor(result))
        }
        return result
    }

    override fun isFavorited(dramaId: Int): Flow<Boolean> =
        localDao.isFavoritedFlow(dramaId)
}

class WatchLaterRepositoryImpl(
    private val api: WatchLaterApi,
    private val localDao: LocalWatchLaterDao,
    private val sessionManager: SessionManager,
    private val json: Json,
    private val failureBus: EngagementFailureBus = NoOpFailureBus(),
) : WatchLaterRepository {

    override suspend fun add(dramaId: Int): ApiResult<Unit> {
        if (dramaId < MIN_DRAMA_ID) return invalidDramaIdError()
        val isAuthed = sessionManager.currentSession() != null
        if (!isAuthed) {
            localDao.upsert(
                LocalWatchLaterEntity(
                    drama_id = dramaId,
                    created_at = System.currentTimeMillis(),
                ),
            )
            return ApiResult.Success(Unit)
        }
        localDao.upsert(
            LocalWatchLaterEntity(
                drama_id = dramaId,
                created_at = System.currentTimeMillis(),
            ),
        )
        val result = safeApiCallRaw(json) { api.add(EngagementRequestDto(dramaId)) }
            .treatAlreadyAppliedAsSuccess()
        if (result !is ApiResult.Success) {
            localDao.deleteByDrama(dramaId)
            failureBus.publish(EngagementOp.AddWatchLater(dramaId), messageFor(result))
        }
        return result
    }

    override suspend fun remove(dramaId: Int): ApiResult<Unit> {
        if (dramaId < MIN_DRAMA_ID) return invalidDramaIdError()
        val isAuthed = sessionManager.currentSession() != null
        if (!isAuthed) {
            localDao.deleteByDrama(dramaId)
            return ApiResult.Success(Unit)
        }
        val wasLocal = localDao.isQueued(dramaId)
        localDao.deleteByDrama(dramaId)
        val result = safeApiCallRaw(json) { api.remove(dramaId) }
            .treatAlreadyAppliedAsSuccess()
        if (result !is ApiResult.Success && wasLocal) {
            localDao.upsert(
                LocalWatchLaterEntity(
                    drama_id = dramaId,
                    created_at = System.currentTimeMillis(),
                ),
            )
            failureBus.publish(EngagementOp.RemoveWatchLater(dramaId), messageFor(result))
        }
        return result
    }

    override fun isQueued(dramaId: Int): Flow<Boolean> =
        localDao.isQueuedFlow(dramaId)
}

class WatchedRepositoryImpl(
    private val api: WatchedApi,
    private val localDao: LocalWatchedDao,
    private val sessionManager: SessionManager,
    private val json: Json,
    private val failureBus: EngagementFailureBus = NoOpFailureBus(),
) : WatchedRepository {

    override suspend fun markWatched(dramaId: Int): ApiResult<Unit> {
        if (dramaId < MIN_DRAMA_ID) return invalidDramaIdError()
        val isAuthed = sessionManager.currentSession() != null
        if (!isAuthed) {
            localDao.upsert(
                LocalWatchedEntity(
                    drama_id = dramaId,
                    created_at = System.currentTimeMillis(),
                ),
            )
            return ApiResult.Success(Unit)
        }
        localDao.upsert(
            LocalWatchedEntity(
                drama_id = dramaId,
                created_at = System.currentTimeMillis(),
            ),
        )
        val result = safeApiCallRaw(json) { api.markWatched(EngagementRequestDto(dramaId)) }
            .treatAlreadyAppliedAsSuccess()
        if (result !is ApiResult.Success) {
            localDao.deleteByDrama(dramaId)
            failureBus.publish(EngagementOp.MarkWatched(dramaId), messageFor(result))
        }
        return result
    }

    override fun isMarkedWatched(dramaId: Int): Flow<Boolean> =
        localDao.isMarkedWatchedFlow(dramaId)
}

/** Convert any non-success ApiResult into a user-facing snackbar message. */
internal fun messageFor(result: ApiResult<*>): String = when (result) {
    is ApiResult.ValidationError -> result.fieldErrors.values.flatten().firstOrNull()
        ?: "Couldn't save that action."
    is ApiResult.NetworkError -> "Can't reach the server. We'll save this for next time."
    is ApiResult.Error -> result.message.ifBlank { "Couldn't save that action." }
    is ApiResult.Success -> "" // unreachable
}
