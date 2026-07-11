package com.appriyo.deulama.data.repository

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
import kotlinx.serialization.json.Json

/**
 * All three engagement repositories share the same idempotency rule:
 * 409 (already in the list) and 404 (not in the list) collapse to Success
 * because the desired state is already true and we update the UI
 * optimistically — a double-tap never produces an error toast.
 */

private fun invalidDramaIdError(): ApiResult<Unit> =
    ApiResult.ValidationError(mapOf("drama_id" to listOf("Drama id must be >= $MIN_DRAMA_ID.")))

class FavoritesRepositoryImpl(
    private val api: FavoritesApi,
    private val json: Json,
) : FavoritesRepository {

    override suspend fun add(dramaId: Int): ApiResult<Unit> {
        if (dramaId < MIN_DRAMA_ID) return invalidDramaIdError()
        return safeApiCallRaw(json) { api.add(EngagementRequestDto(dramaId)) }
            .treatAlreadyAppliedAsSuccess()
    }

    override suspend fun remove(dramaId: Int): ApiResult<Unit> {
        if (dramaId < MIN_DRAMA_ID) return invalidDramaIdError()
        return safeApiCallRaw(json) { api.remove(dramaId) }
            .treatAlreadyAppliedAsSuccess()
    }
}

class WatchLaterRepositoryImpl(
    private val api: WatchLaterApi,
    private val json: Json,
) : WatchLaterRepository {

    override suspend fun add(dramaId: Int): ApiResult<Unit> {
        if (dramaId < MIN_DRAMA_ID) return invalidDramaIdError()
        return safeApiCallRaw(json) { api.add(EngagementRequestDto(dramaId)) }
            .treatAlreadyAppliedAsSuccess()
    }

    override suspend fun remove(dramaId: Int): ApiResult<Unit> {
        if (dramaId < MIN_DRAMA_ID) return invalidDramaIdError()
        return safeApiCallRaw(json) { api.remove(dramaId) }
            .treatAlreadyAppliedAsSuccess()
    }
}

class WatchedRepositoryImpl(
    private val api: WatchedApi,
    private val json: Json,
) : WatchedRepository {

    override suspend fun markWatched(dramaId: Int): ApiResult<Unit> {
        if (dramaId < MIN_DRAMA_ID) return invalidDramaIdError()
        return safeApiCallRaw(json) { api.markWatched(EngagementRequestDto(dramaId)) }
            .treatAlreadyAppliedAsSuccess()
    }
}
