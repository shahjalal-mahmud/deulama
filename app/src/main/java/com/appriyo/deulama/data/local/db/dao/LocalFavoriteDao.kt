package com.appriyo.deulama.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appriyo.deulama.data.local.db.entity.LocalFavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalFavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalFavoriteEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM local_favorite WHERE drama_id = :dramaId)")
    fun isFavoritedFlow(dramaId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM local_favorite WHERE drama_id = :dramaId)")
    suspend fun isFavorited(dramaId: Int): Boolean

    @Query("SELECT * FROM local_favorite ORDER BY created_at ASC")
    suspend fun allOrdered(): List<LocalFavoriteEntity>

    @Query("DELETE FROM local_favorite WHERE drama_id = :dramaId")
    suspend fun deleteByDrama(dramaId: Int)

    @Query("DELETE FROM local_favorite")
    suspend fun clear()
}
