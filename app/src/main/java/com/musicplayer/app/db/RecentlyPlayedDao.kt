package com.musicplayer.app.db

import androidx.room.*

@Dao
interface RecentlyPlayedDao {
    @Query("SELECT * FROM recently_played ORDER BY playedAt DESC LIMIT 50")
    suspend fun getRecentlyPlayed(): List<RecentlyPlayedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: RecentlyPlayedEntity)

    @Query("DELETE FROM recently_played WHERE songId NOT IN (SELECT songId FROM recently_played ORDER BY playedAt DESC LIMIT 50)")
    suspend fun trimToLimit()
}
