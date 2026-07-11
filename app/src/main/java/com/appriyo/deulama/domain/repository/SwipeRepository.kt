package com.appriyo.deulama.domain.repository

import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.SwipeRecord
import com.appriyo.deulama.domain.model.SwipeType

/**
 * `POST /api/swipe` — upserts (user_id, drama_id).
 *
 *  - Both `200 OK` (re-swipe) and `201 Created` (first swipe) are
 *    success — implementation collapses them so the optimistic UI
 *    never surfaces a re-swipe as an error.
 *  - 422 (validation), 404 (drama missing), and 401 (no JWT) come
 *    through as the matching [ApiResult] subtypes for the screen to
 *    render a one-shot toast.
 */
interface SwipeRepository {
    suspend fun recordSwipe(dramaId: Int, type: SwipeType): ApiResult<SwipeRecord>
}
