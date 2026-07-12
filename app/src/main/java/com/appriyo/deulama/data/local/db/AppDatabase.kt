package com.appriyo.deulama.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.appriyo.deulama.data.local.db.dao.LocalFavoriteDao
import com.appriyo.deulama.data.local.db.dao.LocalSwipeDao
import com.appriyo.deulama.data.local.db.dao.LocalWatchLaterDao
import com.appriyo.deulama.data.local.db.dao.LocalWatchedDao
import com.appriyo.deulama.data.local.db.entity.LocalFavoriteEntity
import com.appriyo.deulama.data.local.db.entity.LocalSwipeEntity
import com.appriyo.deulama.data.local.db.entity.LocalWatchLaterEntity
import com.appriyo.deulama.data.local.db.entity.LocalWatchedEntity

/**
 * Holds the four anon-mode engagement queues:
 *
 *  - `local_swipe`      — like / dislike gestures made while signed out
 *  - `local_favorite`   — favorite toggles made while signed out
 *  - `local_watch_later` — queue additions made while signed out
 *  - `local_watched`    — "mark watched" actions made while signed out
 *
 * Schema bumps: bumping `version` requires either a destructive
 * migration (fine for now — the queues only ever hold in-flight, not
 * long-lived data) or a Migration object. For Phase 4 we accept the
 * simplest path: install with `fallbackToDestructiveMigration`.
 */
@Database(
    entities = [
        LocalSwipeEntity::class,
        LocalFavoriteEntity::class,
        LocalWatchLaterEntity::class,
        LocalWatchedEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localSwipeDao(): LocalSwipeDao
    abstract fun localFavoriteDao(): LocalFavoriteDao
    abstract fun localWatchLaterDao(): LocalWatchLaterDao
    abstract fun localWatchedDao(): LocalWatchedDao
}
