package com.appriyo.deulama.data.repository

import com.appriyo.deulama.data.remote.ApiResult
import com.appriyo.deulama.domain.model.EngagementEntry
import com.appriyo.deulama.domain.repository.FavoritesRepository
import com.appriyo.deulama.domain.repository.WatchLaterRepository
import com.appriyo.deulama.domain.repository.WatchedRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Section G of UNIT_TEST_PLAN.md — backs `data/repository/TimelineRepositoryImpl.kt`.
 * All ✅ verified in the audit — every test here is PROTECTIVE.
 *
 * This file mocks the three engagement repositories directly rather than
 * their underlying DAOs/DTOs, since `TimelineRepositoryImpl` only ever
 * talks to those three interfaces. That keeps this file fully decoupled
 * from `EngagementListItemDto`/`DramaDto` shape details.
 *
 * `EngagementEntry` is constructed with a relaxed-mocked `Drama` since
 * its internal fields are irrelevant here — these tests only assert on
 * `kind`, `timestamp`, and list membership/ordering.
 */
class TimelineRepositoryImplTest {

    private val favoritesRepository = mockk<FavoritesRepository>()
    private val watchLaterRepository = mockk<WatchLaterRepository>()
    private val watchedRepository = mockk<WatchedRepository>()

    private val repository = TimelineRepositoryImpl(
        favoritesRepository = favoritesRepository,
        watchLaterRepository = watchLaterRepository,
        watchedRepository = watchedRepository,
    )

    private fun entry(kind: EngagementEntry.Kind, timestamp: String): EngagementEntry {
        val drama = mockk<com.appriyo.deulama.domain.model.Drama>(relaxed = true)
        return EngagementEntry(kind = kind, drama = drama, timestamp = timestamp)
    }

    // -----------------------------------------------------------------
    // Concurrency
    // -----------------------------------------------------------------

    @Test
    fun `loadTimeline fires all three list calls concurrently, not sequentially`() = runTest {
        // Each repo's call blocks on its own CompletableDeferred until we
        // release it. If the three calls were sequential (not launched via
        // async before the first await), the second/third `coEvery` blocks
        // would never even be reached until the first resolves — this test
        // fails by hanging/timing out under that regression.
        val favoritesGate = CompletableDeferred<Unit>()
        val watchLaterGate = CompletableDeferred<Unit>()
        val watchedGate = CompletableDeferred<Unit>()

        coEvery { favoritesRepository.listFavorites() } coAnswers {
            favoritesGate.complete(Unit)
            watchLaterGate.await()
            ApiResult.Success(emptyList())
        }
        coEvery { watchLaterRepository.listWatchLater() } coAnswers {
            watchLaterGate.complete(Unit)
            watchedGate.await()
            ApiResult.Success(emptyList())
        }
        coEvery { watchedRepository.listWatched() } coAnswers {
            watchedGate.complete(Unit)
            ApiResult.Success(emptyList())
        }

        val result = repository.loadTimeline()
        assertTrue(result is ApiResult.Success)
    }

    // -----------------------------------------------------------------
    // Merge + sort
    // -----------------------------------------------------------------

    @Test
    fun `loadTimeline merges favorites + watch_later + watched rows newest-first by timestamp`() = runTest {
        coEvery { favoritesRepository.listFavorites() } returns ApiResult.Success(
            listOf(entry(EngagementEntry.Kind.FAVORITED, "2026-07-01 10:00:00")),
        )
        coEvery { watchLaterRepository.listWatchLater() } returns ApiResult.Success(
            listOf(entry(EngagementEntry.Kind.WATCH_LATER, "2026-07-03 10:00:00")),
        )
        coEvery { watchedRepository.listWatched() } returns ApiResult.Success(
            listOf(entry(EngagementEntry.Kind.WATCHED, "2026-07-02 10:00:00")),
        )

        val result = repository.loadTimeline() as ApiResult.Success
        val timestamps = result.value.map { it.timestamp }
        assertEquals(
            listOf("2026-07-03 10:00:00", "2026-07-02 10:00:00", "2026-07-01 10:00:00"),
            timestamps,
        )
    }

    @Test
    fun `loadTimeline returns ApiResult Success with empty list when all three subcalls return empty lists`() = runTest {
        coEvery { favoritesRepository.listFavorites() } returns ApiResult.Success(emptyList())
        coEvery { watchLaterRepository.listWatchLater() } returns ApiResult.Success(emptyList())
        coEvery { watchedRepository.listWatched() } returns ApiResult.Success(emptyList())

        val result = repository.loadTimeline() as ApiResult.Success
        assertTrue(result.value.isEmpty())
    }

    @Test
    fun `loadTimeline preserves entries from all three sources, not just one`() = runTest {
        coEvery { favoritesRepository.listFavorites() } returns ApiResult.Success(
            listOf(entry(EngagementEntry.Kind.FAVORITED, "2026-07-01 09:00:00")),
        )
        coEvery { watchLaterRepository.listWatchLater() } returns ApiResult.Success(
            listOf(entry(EngagementEntry.Kind.WATCH_LATER, "2026-07-01 08:00:00")),
        )
        coEvery { watchedRepository.listWatched() } returns ApiResult.Success(
            listOf(entry(EngagementEntry.Kind.WATCHED, "2026-07-01 07:00:00")),
        )

        val result = repository.loadTimeline() as ApiResult.Success
        val kinds = result.value.map { it.kind }.toSet()
        assertEquals(
            setOf(EngagementEntry.Kind.FAVORITED, EngagementEntry.Kind.WATCH_LATER, EngagementEntry.Kind.WATCHED),
            kinds,
        )
    }

    // -----------------------------------------------------------------
    // Failure short-circuit
    // -----------------------------------------------------------------

    @Test
    fun `loadTimeline returns the first non-Success subcall when favorites fails`() = runTest {
        val favoritesError = ApiResult.Error(httpStatus = 500, message = "Favorites down")
        coEvery { favoritesRepository.listFavorites() } returns favoritesError
        coEvery { watchLaterRepository.listWatchLater() } returns ApiResult.Success(emptyList())
        coEvery { watchedRepository.listWatched() } returns ApiResult.Success(emptyList())

        val result = repository.loadTimeline()
        assertEquals(favoritesError, result)
    }

    @Test
    fun `loadTimeline returns a non-Success result when only watch-later fails`() = runTest {
        val watchLaterError = ApiResult.NetworkError(RuntimeException("offline"))
        coEvery { favoritesRepository.listFavorites() } returns ApiResult.Success(emptyList())
        coEvery { watchLaterRepository.listWatchLater() } returns watchLaterError
        coEvery { watchedRepository.listWatched() } returns ApiResult.Success(emptyList())

        val result = repository.loadTimeline()
        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `loadTimeline returns a non-Success result when only watched fails`() = runTest {
        val watchedError = ApiResult.ValidationError(emptyMap())
        coEvery { favoritesRepository.listFavorites() } returns ApiResult.Success(emptyList())
        coEvery { watchLaterRepository.listWatchLater() } returns ApiResult.Success(emptyList())
        coEvery { watchedRepository.listWatched() } returns watchedError

        val result = repository.loadTimeline()
        assertTrue(result is ApiResult.ValidationError)
    }

    @Test
    fun `loadTimeline still awaits all three calls even though only the first result is used on failure`() = runTest {
        coEvery { favoritesRepository.listFavorites() } returns ApiResult.Error(httpStatus = 500, message = "boom")
        coEvery { watchLaterRepository.listWatchLater() } returns ApiResult.Success(emptyList())
        coEvery { watchedRepository.listWatched() } returns ApiResult.Success(emptyList())

        repository.loadTimeline()

        // Structured concurrency means all three async{} blocks are
        // launched regardless of which one "wins" the failure check —
        // confirm none of the three was skipped.
        coVerify(exactly = 1) { favoritesRepository.listFavorites() }
        coVerify(exactly = 1) { watchLaterRepository.listWatchLater() }
        coVerify(exactly = 1) { watchedRepository.listWatched() }
    }

    // -----------------------------------------------------------------
    // Cancellation
    // -----------------------------------------------------------------

    @Test
    fun `loadTimeline propagates CancellationException without swallowing it`() = runTest {
        coEvery { favoritesRepository.listFavorites() } throws CancellationException("scope cancelled")
        coEvery { watchLaterRepository.listWatchLater() } returns ApiResult.Success(emptyList())
        coEvery { watchedRepository.listWatched() } returns ApiResult.Success(emptyList())

        var thrown: Throwable? = null
        try {
            repository.loadTimeline()
        } catch (e: CancellationException) {
            thrown = e
        }
        assertTrue(thrown is CancellationException)
    }
}