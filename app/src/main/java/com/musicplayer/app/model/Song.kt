package com.musicplayer.app.model

import android.net.Uri
import java.io.Serializable

/**
 * Represents a single audio track discovered on the device.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,          // milliseconds
    val path: String,            // absolute file path
    val folderName: String,      // parent folder name
    val folderPath: String,      // parent folder absolute path
    val size: Long,              // bytes
    val dateAdded: Long,         // epoch seconds
    val albumArtUri: Uri?        // content URI for album art (may be null)
) : Serializable {

    /** Human-readable duration like "3:45" */
    val durationFormatted: String get() {
        val totalSecs = duration / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%d:%02d".format(mins, secs)
    }

    /** Display name: prefers title, falls back to filename */
    val displayTitle: String get() = title.ifBlank {
        path.substringAfterLast('/').substringBeforeLast('.')
    }

    /** Display artist: falls back to "Unknown Artist" */
    val displayArtist: String get() = artist.ifBlank { "Unknown Artist" }
}
