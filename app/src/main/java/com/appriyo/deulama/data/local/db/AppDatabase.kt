package com.appriyo.deulama.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Empty stub for now. Section 7 of the plan calls for Room tables:
 * local_swipe, local_favorite, local_watch_later, local_watched — add
 * their @Entity/@Dao pairs here when the anonymous-mode sync feature
 * phase starts.
 *
 * version = 1 and entities = [] is a placeholder; Room requires at
 * least one @Entity to actually compile a schema, so this class isn't
 * wired into the Koin graph yet (see di/AppModules.kt).
 */
@Database(entities = [], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase()