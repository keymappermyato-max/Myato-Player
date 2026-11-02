package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    fun isFavorite(songId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    suspend fun isFavoriteSync(songId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorites(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)

    @Query("DELETE FROM favorites WHERE songId IN (:songIds)")
    suspend fun removeFavorites(songIds: List<Long>)

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    @Query("""
        SELECT songs.* FROM songs
        INNER JOIN favorites ON songs.id = favorites.songId
        ORDER BY favorites.addedAt DESC
    """)
    fun getFavoriteSongs(): Flow<List<SongEntity>>
}
