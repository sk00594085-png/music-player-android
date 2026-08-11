package com.musicplayer.app.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import com.musicplayer.app.service.MusicService

/** Helper to build intents targeting [MusicService]. */
object ServiceUtils {

    fun startService(context: Context, action: String, extras: Intent.() -> Unit = {}) {
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
            extras()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
