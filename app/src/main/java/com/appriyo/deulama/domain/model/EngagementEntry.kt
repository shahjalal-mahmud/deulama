package com.appriyo.deulama.domain.model

/**
 * One row in the Activity Timeline.
 *
 * The timeline merges three lists (favorites, watch-later, watched)
 * into a single reverse-chronological feed. Each entry pairs the
 * underlying engagement kind with its timestamp + the embedded
 * [Drama] the server returns alongside the row.
 *
 * We intentionally don't model "swipe" entries — there's no
 * `GET /api/swipes` endpoint (per api.md), and the swipe signal is
 * already surfaced through Recommendations + GenreBreakdown.
 */
data class EngagementEntry(
    val kind: Kind,
    val drama: Drama,
    /** ISO-ish timestamp string the API returned. Raw on purpose;
     *  the UI layer formats relative-time labels from it. */
    val timestamp: String,
) {
    /** What action created this row. Drives chip colour + label. */
    enum class Kind { FAVORITED, WATCH_LATER, WATCHED }
}