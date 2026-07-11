package com.appriyo.deulama.data.repository

import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.data.remote.MIN_DRAMA_ID
import com.appriyo.deulama.data.remote.api.SwipeApi
import com.appriyo.deulama.data.remote.dto.SwipeRequestDto
import com.appriyo.deulama.data.remote.map
import com.appriyo.deulama.data.remote.safeApiCall
import com.appriyo.deulama.domain.model.SwipeRecord
import com.appriyo.deulama.domain.model.SwipeType
import com.appriyo.deulama.domain.repository.SwipeRepository
import kotlinx.serialization.json.Json

/**
 * The server's swipe endpoint upserts, so re-swipes surface as Success
 * regardless of insert (201) vs update (200) — we don't branch on status.
 */
class SwipeRepositoryImpl(
    private val swipeApi: SwipeApi,
    private val json: Json,
) : SwipeRepository {

    override suspend fun recordSwipe(dramaId: Int, type: SwipeType): ApiResult<SwipeRecord> {
        if (dramaId < MIN_DRAMA_ID) {
            return ApiResult.ValidationError(
                mapOf("drama_id" to listOf("Drama id must be >= $MIN_DRAMA_ID.")),
            )
        }
        val request = SwipeRequestDto(drama_id = dramaId, swipe_type = type.wire)
        return safeApiCall(json) { swipeApi.recordSwipe(request) }
            .map { it.toDomain() }
    }
}

internal fun com.appriyo.deulama.data.remote.dto.SwipeRecordDto.toDomain(): SwipeRecord =
    SwipeRecord(
        dramaId = drama_id,
        swipeType = if (swipe_type == SwipeType.LIKE.wire) SwipeType.LIKE else SwipeType.DISLIKE,
        createdAt = created_at,
        updatedAt = updated_at,
    )
