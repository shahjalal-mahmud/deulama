package com.appriyo.deulama.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Queued watch-later addition recorded while anonymous. Replayed on
 * login against `POST /api/watch-later`. Same shape as
 * [LocalFavoriteEntity] — only "added" is queueable from anon mode.
 */
@Entity(
    tableName = "local_watch_later",
    primaryKeys = ["drama_id"],
    indices = [Index(value = ["created_at"])],
)
data class LocalWatchLaterEntity(
    val drama_id: Int,
    val created_at: Long,
    val applied: Boolean = false,
)
