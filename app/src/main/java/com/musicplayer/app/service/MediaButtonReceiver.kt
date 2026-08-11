package com.musicplayer.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import com.musicplayer.app.utils.ServiceUtils

/**
 * Receives hardware media button events (headset buttons, Bluetooth remote).
 * Routes them to [MusicService].
 */
class MediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return
        val event = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        } ?: return

        if (event.action != KeyEvent.ACTION_DOWN) return

        val action = when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK ->
                MusicService.ACTION_TOGGLE_PLAY
            KeyEvent.KEYCODE_MEDIA_NEXT -> MusicService.ACTION_NEXT
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MusicService.ACTION_PREV
            KeyEvent.KEYCODE_MEDIA_STOP -> MusicService.ACTION_STOP
            else -> return
        }
        ServiceUtils.startService(context, action)
    }
}
