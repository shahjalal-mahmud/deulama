package com.appriyo.deulama.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Queued favorite toggle (`added` only — anonymous users can't un-favorite
 * since we never gave them a delete affordance). Replayed on login
 * against `POST /api/favorites`.
 *
 * `applied` flips to `true` once a row has been successfully POSTed to
 * the server during sync; the queue-prune step removes rows where
 * `applied = true`. Failed rows stay with `applied = false` so a
 * later retry can pick them up. (For Phase 4 we keep it simple — the
 * sync service retries on next login until success.)
 */
@Entity(
    tableName = "local_favorite",
    primaryKeys = ["drama_id"],
    indices = [Index(value = ["created_at"])],
)
data class LocalFavoriteEntity(
    val drama_id: Int,
    val created_at: Long,
    val applied: Boolean = false,
)
