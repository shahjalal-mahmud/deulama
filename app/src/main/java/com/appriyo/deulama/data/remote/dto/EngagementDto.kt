package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable

/** Body of `POST /api/favorites`, `POST /api/watch-later`, `POST /api/watched`. */
@Serializable
data class EngagementRequestDto(val drama_id: Int)

/** Response payload the favorites / watch-later POST endpoints return. */
@Serializable
data class EngagementRecordDto(
    val favorite_id: Int = 0,
    val watch_later_id: Int = 0,
    val watched_id: Int = 0,
    val user_id: Int,
    val drama_id: Int,
    val created_at: String? = null,
    val watched_at: String? = null,
)

// ---- Phase 7 — Activity Timeline list DTOs ----
//
// The `GET /api/favorites` / `watch-later` / `watched` endpoints each
// return a single-key object whose value is the list of engagement
// rows.
//
// **Wire shape:** the PHP services flatten each row into a
// `Drama::publicItem(...)` payload — i.e. the row IS a full Drama
// object, not `{ drama_id, drama: {...} }`. There is no nested
// `drama` field, no `favorite_id`/`watch_later_id`/`watched_id`, and
// no engagement-specific timestamp (`favorited_at`/`queued_at`/
// `watched_at`). The repository mapper only has the drama fields to
// work with, so we use the drama's `created_at` (the row's creation
// timestamp in the dramas table) as the display timestamp.
//
// The early design tried to model a wrapped shape and got it wrong —
// every row came back with a null nested `drama` and the Activity
// screen showed "No activity yet" even after the user had favorited
// dramas. This DTO is the corrected, server-truth shape.

/** `data` payload of `GET /api/favorites`. */
@Serializable
data class FavoritesListDto(
    val favorites: List<DramaDto> = emptyList(),
    val count: Int = 0,
)

/** `data` payload of `GET /api/watch-later`. */
@Serializable
data class WatchLaterListDto(
    val watch_later: List<DramaDto> = emptyList(),
    val count: Int = 0,
)

/** `data` payload of `GET /api/watched`. */
@Serializable
data class WatchedListDto(
    val watched: List<DramaDto> = emptyList(),
    val count: Int = 0,
)