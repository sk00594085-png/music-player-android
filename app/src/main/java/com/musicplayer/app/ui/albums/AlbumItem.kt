package com.musicplayer.app.ui.albums

import android.net.Uri

data class AlbumItem(
    val name: String,
    val artist: String,
    val songCount: Int,
    val artUri: Uri?
)
