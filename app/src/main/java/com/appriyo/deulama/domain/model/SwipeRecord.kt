package com.appriyo.deulama.domain.model

/**
 * Result echoed back by `POST /api/swipe`. The server's `data` payload
 * is intentionally small — it only confirms which `(user_id, drama_id)`
 * row now carries which [swipeType], plus a server-side [outcome]
 * marker (`inserted` / `updated`) useful for debug logs.
 *
 * We deliberately don't model `swipe_id`, `created_at`, or `updated_at`
 * because the controller does not return them. Those columns exist in
 * the `swipes` table but the response is trimmed before going over
 * the wire. Mirroring that minimal shape here keeps the deserializer
 * happy and prevents a `MissingFieldException` from being wrapped as
 * a misleading `ApiResult.NetworkError` (the "Can't reach the server"
 * symptom fixed alongside this).
 */
data class SwipeRecord(
    val dramaId: Int,
    val swipeType: SwipeType,
    val outcome: SwipeOutcome? = null,
)

/** Server-side upsert result for `/api/swipe`. */
enum class SwipeOutcome(val wire: String) {
    INSERTED("inserted"),
    UPDATED("updated");

    companion object {
        fun fromWire(value: String?): SwipeOutcome? = when (value) {
            INSERTED.wire -> INSERTED
            UPDATED.wire -> UPDATED
            else -> null
        }
    }
}

/**
 * Domain-level swipe action. The wire enum is hardcoded as
 * `like`/`dislike`; we mirror those two values here so call sites
 * can't typo a free-text string (anything else is a 422 from the API).
 */
enum class SwipeType(val wire: String) {
    LIKE("like"),
    DISLIKE("dislike"),
}
