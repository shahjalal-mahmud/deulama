package com.appriyo.deulama.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Queued swipe (`like` / `dislike`) recorded while the user is
 * anonymous. On login, [com.appriyo.deulama.data.repository.EngagementSyncService]
 * replays these against `POST /api/swipe` in insertion order.
 *
 * Because the server upserts on `(user_id, drama_id)`, the most-recent
 * swipe per drama wins — re-queuing the same drama with a different
 * `swipe_type` simply replaces the queued row.
 */
@Entity(
    tableName = "local_swipe",
    primaryKeys = ["drama_id"],
    indices = [Index(value = ["created_at"])],
)
data class LocalSwipeEntity(
    val drama_id: Int,
    val swipe_type: String, // "like" | "dislike"
    val created_at: Long,   // epoch millis (insertion time, used for replay order)
)
