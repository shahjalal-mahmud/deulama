package com.appriyo.deulama.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Body of `POST /api/swipe`. The server expects `drama_id` as a
 * positive int and `swipe_type` to be exactly `like` or `dislike`
 * (case-sensitive — see api.md). Exported as a request DTO rather than
 * a domain model so the enum string is enforced at the wire edge.
 */
@Serializable
data class SwipeRequestDto(
    val drama_id: Int,
    val swipe_type: String,
)

/**
 * Wire shape of a single recorded swipe. Returned as `Envelope.data`
 * with HTTP `201` on first record and `200` on subsequent upserts —
 * both are success; the API treats them identically client-side.
 */
@Serializable
data class SwipeRecordDto(
    val swipe_id: Int,
    val user_id: Int,
    val drama_id: Int,
    val swipe_type: String,
    val created_at: String,
    val updated_at: String,
)
