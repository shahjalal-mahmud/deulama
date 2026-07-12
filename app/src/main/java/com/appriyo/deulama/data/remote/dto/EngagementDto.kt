package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable

/** Body of `POST /api/favorites`, `POST /api/watch-later`, `POST /api/watched`. */
@Serializable
data class EngagementRequestDto(val drama_id: Int)

/** Response payload the favorites / watch-later endpoints return.
 *  The API embeds a small `drama` object in `data.favorites[i]` /
 *  `data.watch_later[i]`. We don't model it yet — Phase 3 only
 *  needs the success/failure decision. */
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
// rows. Each row carries the engagement id + timestamp AND an
// embedded `drama` object so the client can render the timeline
// without a second round-trip per entry.
//
// We share [EngagementListItemDto] across all three — the wire fields
// differ only in the primary-key name (`favorite_id` vs
// `watch_later_id` vs `watched_id`) and the timestamp key
// (`created_at` vs `watched_at`). The repository mapper decides which
// of those fields is canonical.

/**
 * Shared shape of one row in `GET /api/favorites`,
 * `GET /api/watch-later`, `GET /api/watched`.
 *
 * Every primary-key field is defaulted so a single DTO parses all
 * three payloads. The timestamp field is named differently across
 * endpoints (`created_at` for favorites/watch-later, `watched_at`
 * for watched); we capture both defensively and let the mapper pick.
 */
@Serializable
data class EngagementListItemDto(
    val favorite_id: Int = 0,
    val watch_later_id: Int = 0,
    val watched_id: Int = 0,
    val user_id: Int = 0,
    val drama_id: Int,
    val created_at: String? = null,
    val watched_at: String? = null,
    /** Embedded drama — see [DramaDto]. Some older server payloads
     *  omit it; treat as optional. */
    val drama: DramaDto? = null,
)

/** `data` payload of `GET /api/favorites`. */
@Serializable
data class FavoritesListDto(
    val favorites: List<EngagementListItemDto> = emptyList(),
    val count: Int = 0,
)

/** `data` payload of `GET /api/watch-later`. */
@Serializable
data class WatchLaterListDto(
    val watch_later: List<EngagementListItemDto> = emptyList(),
    val count: Int = 0,
)

/** `data` payload of `GET /api/watched`. */
@Serializable
data class WatchedListDto(
    val watched: List<EngagementListItemDto> = emptyList(),
    val count: Int = 0,
)
