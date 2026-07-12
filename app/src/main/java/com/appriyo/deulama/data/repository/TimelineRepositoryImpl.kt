package com.appriyo.deulama.data.repository

import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.EngagementEntry
import com.appriyo.deulama.domain.repository.FavoritesRepository
import com.appriyo.deulama.domain.repository.TimelineRepository
import com.appriyo.deulama.domain.repository.WatchLaterRepository
import com.appriyo.deulama.domain.repository.WatchedRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Concrete [TimelineRepository] — fires the three list endpoints
 * concurrently and merges.
 *
 * Per Phase-7 spec the timeline is a **client-side merge**. We use a
 * single [coroutineScope] so all three calls share a structured
 * concurrency scope; if any call throws unexpectedly the whole load
 * cancels (a thrown CancellationException propagates out so the
 * ViewModel can retry).
 *
 * Sort order:
 * - Each list endpoint returns rows newest-first (server-enforced).
 * - We merge by [EngagementEntry.timestamp] descending using
 *   [String.compareTo] — the API returns fixed-width timestamps so a
 *   lexicographic compare matches chronological order. This is the
 *   same convention used by [com.appriyo.deulama.domain.model.Session]
 *   and the genre-stats screen.
 *
 * Failure semantics:
 * - Any subcall returning a non-Success `ApiResult` short-circuits the
 *   whole load — we don't try to render a half-loaded feed in Phase 7.
 *   (Per-error partial rendering is a Phase-8 polish concern.)
 */
class TimelineRepositoryImpl(
    private val favoritesRepository: FavoritesRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val watchedRepository: WatchedRepository,
) : TimelineRepository {

    override suspend fun loadTimeline(): ApiResult<List<EngagementEntry>> =
        coroutineScope {
            val favoritesDeferred = async { favoritesRepository.listFavorites() }
            val watchLaterDeferred = async { watchLaterRepository.listWatchLater() }
            val watchedDeferred = async { watchedRepository.listWatched() }

            val favorites = favoritesDeferred.await()
            val watchLater = watchLaterDeferred.await()
            val watched = watchedDeferred.await()

            // First-failure-wins. Order doesn't matter for the user —
            // they see one banner either way.
            val firstFailure = listOf(favorites, watchLater, watched)
                .firstOrNull { it !is ApiResult.Success }
            if (firstFailure != null) {
                @Suppress("UNCHECKED_CAST")
                return@coroutineScope firstFailure as ApiResult<List<EngagementEntry>>
            }

            val merged = buildList {
                addAll((favorites as ApiResult.Success<List<EngagementEntry>>).value)
                addAll((watchLater as ApiResult.Success<List<EngagementEntry>>).value)
                addAll((watched as ApiResult.Success<List<EngagementEntry>>).value)
            }.sortedByDescending { it.timestamp }

            ApiResult.Success(merged)
        }
}