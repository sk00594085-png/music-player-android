package com.musicplayer.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        FavouriteEntity::class,
        RecentlyPlayedEntity::class,
        PlayCountEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun playCountDao(): PlayCountDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "music_player.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
