package com.appriyo.deulama.domain.repository

import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.EngagementEntry

/**
 * Phase 7 — reverse-chronological Activity Timeline.
 *
 * The timeline is a **client-side merge** of three independent list
 * endpoints (`GET /api/favorites`, `GET /api/watch-later`,
 * `GET /api/watched`). The repositories return rows already in
 * descending order from the server; we just need to:
 *
 *  1. Fetch all three concurrently with `coroutineScope { async … }`
 *     so a slow watch-later call doesn't serialise behind favorites.
 *  2. Merge the results into one [List] sorted newest-first.
 *
 * Swipe rows are **intentionally not** part of the timeline — the
 * server has no `GET /api/swipes` endpoint, and swipe signals already
 * drive Recommendations + GenreBreakdown (Phase 5).
 */
interface TimelineRepository {

    /**
     * Loads the merged, sorted feed. Returns one of:
     *
     * - `Success(emptyList)` — all three endpoints returned 200 with
     *   no rows (brand-new account, or every action removed).
     * - `Success(rows)` — non-empty feed, newest first.
     * - `Error / ValidationError / NetworkError` — at least one of
     *   the three calls failed; the user sees a "couldn't load" state
     *   with a Retry CTA.
     */
    suspend fun loadTimeline(): ApiResult<List<EngagementEntry>>
}