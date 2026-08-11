package com.musicplayer.app.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.musicplayer.app.model.Folder
import com.musicplayer.app.model.Song
import com.musicplayer.app.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Queries the Android MediaStore to discover all audio files on the device.
 * Returns a flat list of [Song] objects and a grouped list of [Folder] objects.
 * Respects exclusion folders and minimum duration from SharedPreferences.
 */
class SongRepository(private val context: Context) {

    private val albumArtBaseUri: Uri =
        Uri.parse("content://media/external/audio/albumart")

    /**
     * Scans the MediaStore and returns all audio tracks meeting the minimum duration.
     * Runs on the IO dispatcher.
     */
    suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        val minDurationMs = AppPreferences.getMinDurationSeconds(context) * 1000L
        val excludedFolders = AppPreferences.getExcludedFolders(context)
        val songs = mutableListOf<Song>()

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor: Cursor? = context.contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (it.moveToNext()) {
                val path = it.getString(dataCol) ?: continue
                val file = File(path)
                val folderPath = file.parentFile?.absolutePath ?: ""

                // Skip excluded folders
                if (excludedFolders.any { excl -> folderPath == excl || folderPath.startsWith("$excl/") }) {
                    continue
                }

                val albumId = it.getLong(albumIdCol)
                val albumArtUri = ContentUris.withAppendedId(albumArtBaseUri, albumId)

                songs.add(
                    Song(
                        id = it.getLong(idCol),
                        title = it.getString(titleCol) ?: file.nameWithoutExtension,
                        artist = it.getString(artistCol) ?: "",
                        album = it.getString(albumCol) ?: "",
                        duration = it.getLong(durationCol),
                        path = path,
                        folderName = file.parentFile?.name ?: "Unknown",
                        folderPath = folderPath,
                        size = it.getLong(sizeCol),
                        dateAdded = it.getLong(dateCol),
                        albumArtUri = albumArtUri
                    )
                )
            }
        }

        songs
    }

    /**
     * Groups songs by their parent folder.
     */
    suspend fun getFolders(): List<Folder> = withContext(Dispatchers.IO) {
        getAllSongs()
            .groupBy { it.folderPath }
            .map { (path, songs) ->
                Folder(
                    name = songs.first().folderName,
                    path = path,
                    songs = songs.sortedBy { it.displayTitle }
                )
            }
            .sortedBy { it.name.lowercase() }
    }
}
