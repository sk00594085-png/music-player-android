package com.musicplayer.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val songId: Long,
    val songPath: String,
    val addedAt: Long = System.currentTimeMillis()
)
