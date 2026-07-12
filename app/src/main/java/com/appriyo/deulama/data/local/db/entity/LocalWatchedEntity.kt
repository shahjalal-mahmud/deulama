package com.appriyo.deulama.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Queued "marked watched" recorded while anonymous. Replayed on login
 * against `POST /api/watched`. Per api.md there is intentionally **no**
 * DELETE endpoint for watched; the queue only ever carries additions.
 */
@Entity(
    tableName = "local_watched",
    primaryKeys = ["drama_id"],
    indices = [Index(value = ["created_at"])],
)
data class LocalWatchedEntity(
    val drama_id: Int,
    val created_at: Long,
    val applied: Boolean = false,
)
