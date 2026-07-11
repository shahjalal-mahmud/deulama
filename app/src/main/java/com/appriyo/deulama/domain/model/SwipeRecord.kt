package com.appriyo.deulama.domain.model

/**
 * Record returned by `POST /api/swipe`. We don't need the row id or
 * `user_id` on the client; we just keep the `swipeType` echo so the
 * UI / debug logs can confirm what the server stored.
 *
 * `createdAt` / `updatedAt` stay as raw API strings — same convention
 * as [Drama.releaseYear] and [com.appriyo.deulama.domain.model.User.createdAt].
 * Parse lazily when something actually needs to render or compare
 * them.
 */
data class SwipeRecord(
    val dramaId: Int,
    val swipeType: SwipeType,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * Domain-level swipe action. The wire enum is hardcoded as
 * `like`/`dislike`; we mirror those two values here so call sites
 * can't typo a free-text string (anything else is a 422 from the API).
 */
enum class SwipeType(val wire: String) {
    LIKE("like"),
    DISLIKE("dislike"),
}