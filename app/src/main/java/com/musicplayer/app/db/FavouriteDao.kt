package com.musicplayer.app.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FavouriteDao {
    @Query("SELECT * FROM favourites ORDER BY addedAt DESC")
    fun getAllFavourites(): LiveData<List<FavouriteEntity>>

    @Query("SELECT * FROM favourites ORDER BY addedAt DESC")
    suspend fun getAllFavouritesSync(): List<FavouriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavourite(fav: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE songId = :songId")
    suspend fun removeFavourite(songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE songId = :songId)")
    suspend fun isFavourite(songId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE songId = :songId)")
    fun isFavouriteLive(songId: Long): LiveData<Boolean>
}
