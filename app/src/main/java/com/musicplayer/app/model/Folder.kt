package com.musicplayer.app.model

/**
 * Represents a folder that contains one or more audio files.
 */
data class Folder(
    val name: String,
    val path: String,
    val songs: List<Song>
) {
    val songCount: Int get() = songs.size
}
