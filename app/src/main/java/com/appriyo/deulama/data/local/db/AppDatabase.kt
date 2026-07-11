package com.appriyo.deulama.data.local.db

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

// TEMPORARY — delete this once you add your first real entity
// (local_swipe, local_favorite, local_watch_later, local_watched).
@Entity(tableName = "_placeholder")
internal data class PlaceholderEntity(
    @PrimaryKey val id: Long = 0,
)

@Database(entities = [PlaceholderEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase()