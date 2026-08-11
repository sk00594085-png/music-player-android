package com.musicplayer.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object AppPreferences {
    private const val PREFS_NAME = "music_player_prefs"

    // Keys
    const val KEY_PLAYBACK_SPEED = "playback_speed"
    const val KEY_CROSSFADE_SECONDS = "crossfade_seconds"
    const val KEY_EQ_PRESET = "eq_preset"
    const val KEY_EQ_ENABLED = "eq_enabled"
    const val KEY_EXCLUDED_FOLDERS = "excluded_folders"
    const val KEY_MIN_DURATION = "min_duration_seconds"
    const val KEY_THEME = "theme_mode"
    const val KEY_AUTO_PLAY_BT = "auto_play_bluetooth"
    const val KEY_LAST_SONG_INDEX = "last_song_index"
    const val KEY_LAST_SONG_POSITION = "last_song_position"
    const val KEY_LAST_QUEUE = "last_queue"
    const val KEY_SORT_BY = "sort_by"
    const val KEY_SORT_ASC = "sort_ascending"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPlaybackSpeed(context: Context): Float =
        prefs(context).getFloat(KEY_PLAYBACK_SPEED, 1.0f)

    fun setPlaybackSpeed(context: Context, speed: Float) =
        prefs(context).edit().putFloat(KEY_PLAYBACK_SPEED, speed).apply()

    fun getCrossfadeSeconds(context: Context): Int =
        prefs(context).getInt(KEY_CROSSFADE_SECONDS, 0)

    fun setCrossfadeSeconds(context: Context, seconds: Int) =
        prefs(context).edit().putInt(KEY_CROSSFADE_SECONDS, seconds).apply()

    fun getEqPreset(context: Context): String =
        prefs(context).getString(KEY_EQ_PRESET, "Normal") ?: "Normal"

    fun setEqPreset(context: Context, preset: String) =
        prefs(context).edit().putString(KEY_EQ_PRESET, preset).apply()

    fun isEqEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_EQ_ENABLED, false)

    fun setEqEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_EQ_ENABLED, enabled).apply()

    fun getExcludedFolders(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_EXCLUDED_FOLDERS, emptySet()) ?: emptySet()

    fun setExcludedFolders(context: Context, folders: Set<String>) =
        prefs(context).edit().putStringSet(KEY_EXCLUDED_FOLDERS, folders).apply()

    fun getMinDurationSeconds(context: Context): Int =
        prefs(context).getInt(KEY_MIN_DURATION, 10)

    fun setMinDurationSeconds(context: Context, seconds: Int) =
        prefs(context).edit().putInt(KEY_MIN_DURATION, seconds).apply()

    fun getThemeMode(context: Context): Int =
        prefs(context).getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun setThemeMode(context: Context, mode: Int) =
        prefs(context).edit().putInt(KEY_THEME, mode).apply()

    fun isAutoPlayBluetooth(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_PLAY_BT, false)

    fun setAutoPlayBluetooth(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_AUTO_PLAY_BT, enabled).apply()

    fun getLastSongIndex(context: Context): Int =
        prefs(context).getInt(KEY_LAST_SONG_INDEX, -1)

    fun setLastSongIndex(context: Context, index: Int) =
        prefs(context).edit().putInt(KEY_LAST_SONG_INDEX, index).apply()

    fun getLastSongPosition(context: Context): Int =
        prefs(context).getInt(KEY_LAST_SONG_POSITION, 0)

    fun setLastSongPosition(context: Context, position: Int) =
        prefs(context).edit().putInt(KEY_LAST_SONG_POSITION, position).apply()

    fun getSortBy(context: Context): String =
        prefs(context).getString(KEY_SORT_BY, "title") ?: "title"

    fun setSortBy(context: Context, by: String) =
        prefs(context).edit().putString(KEY_SORT_BY, by).apply()

    fun isSortAscending(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SORT_ASC, true)

    fun setSortAscending(context: Context, asc: Boolean) =
        prefs(context).edit().putBoolean(KEY_SORT_ASC, asc).apply()
}
