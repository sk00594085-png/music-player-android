package com.musicplayer.app.db

import androidx.room.*

@Dao
interface PlayCountDao {
    @Query("SELECT * FROM play_counts ORDER BY count DESC LIMIT 20")
    suspend fun getTopPlayed(): List<PlayCountEntity>

    @Query("SELECT * FROM play_counts WHERE songId = :songId LIMIT 1")
    suspend fun getPlayCount(songId: Long): PlayCountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlayCountEntity)

    @Query("UPDATE play_counts SET count = count + 1, lastPlayedAt = :ts WHERE songId = :songId")
    suspend fun increment(songId: Long, ts: Long = System.currentTimeMillis())
}
