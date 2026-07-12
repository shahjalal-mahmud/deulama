package com.appriyo.deulama.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appriyo.deulama.data.local.db.entity.LocalWatchLaterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalWatchLaterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalWatchLaterEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM local_watch_later WHERE drama_id = :dramaId)")
    fun isQueuedFlow(dramaId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM local_watch_later WHERE drama_id = :dramaId)")
    suspend fun isQueued(dramaId: Int): Boolean

    @Query("SELECT * FROM local_watch_later ORDER BY created_at ASC")
    suspend fun allOrdered(): List<LocalWatchLaterEntity>

    @Query("DELETE FROM local_watch_later WHERE drama_id = :dramaId")
    suspend fun deleteByDrama(dramaId: Int)

    @Query("DELETE FROM local_watch_later")
    suspend fun clear()
}
