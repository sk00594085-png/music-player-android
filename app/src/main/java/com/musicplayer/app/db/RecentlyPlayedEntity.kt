package com.musicplayer.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val songId: Long,
    val songPath: String,
    val playedAt: Long = System.currentTimeMillis()
)
