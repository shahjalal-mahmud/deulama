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
