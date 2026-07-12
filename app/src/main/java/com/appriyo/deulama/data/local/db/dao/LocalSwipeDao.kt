package com.appriyo.deulama.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appriyo.deulama.data.local.db.entity.LocalSwipeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Anon-mode swipe queue. We intentionally keep only one row per drama:
 * a re-swipe (like → dislike) just replaces the queued row, matching
 * the server's upsert behaviour.
 */
@Dao
interface LocalSwipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalSwipeEntity)

    @Query("SELECT * FROM local_swipe ORDER BY created_at ASC")
    suspend fun allOrdered(): List<LocalSwipeEntity>

    @Query("SELECT * FROM local_swipe ORDER BY created_at ASC")
    fun allOrderedFlow(): Flow<List<LocalSwipeEntity>>

    @Query("DELETE FROM local_swipe WHERE drama_id = :dramaId")
    suspend fun deleteByDrama(dramaId: Int)

    @Query("DELETE FROM local_swipe")
    suspend fun clear()
}
