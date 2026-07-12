package com.appriyo.deulama.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appriyo.deulama.data.local.db.entity.LocalWatchedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalWatchedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalWatchedEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM local_watched WHERE drama_id = :dramaId)")
    fun isMarkedWatchedFlow(dramaId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM local_watched WHERE drama_id = :dramaId)")
    suspend fun isMarkedWatched(dramaId: Int): Boolean

    @Query("SELECT * FROM local_watched ORDER BY created_at ASC")
    suspend fun allOrdered(): List<LocalWatchedEntity>

    @Query("DELETE FROM local_watched WHERE drama_id = :dramaId")
    suspend fun deleteByDrama(dramaId: Int)

    @Query("DELETE FROM local_watched")
    suspend fun clear()
}
