package com.musicplayer.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_counts")
data class PlayCountEntity(
    @PrimaryKey val songId: Long,
    val songPath: String,
    val count: Int = 1,
    val lastPlayedAt: Long = System.currentTimeMillis()
)
