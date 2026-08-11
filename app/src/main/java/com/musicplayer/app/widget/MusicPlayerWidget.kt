package com.musicplayer.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.musicplayer.app.R
import com.musicplayer.app.service.MusicService
import com.musicplayer.app.ui.main.MainActivity

class MusicPlayerWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_PREV  = "com.musicplayer.widget.PREV"
        const val ACTION_WIDGET_PLAY  = "com.musicplayer.widget.PLAY"
        const val ACTION_WIDGET_NEXT  = "com.musicplayer.widget.NEXT"

        fun updateWidget(context: Context, title: String, artist: String, isPlaying: Boolean) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, MusicPlayerWidget::class.java)
            )
            for (id in ids) {
                val views = buildRemoteViews(context, title, artist, isPlaying)
                manager.updateAppWidget(id, views)
            }
        }

        private fun buildRemoteViews(
            context: Context, title: String, artist: String, isPlaying: Boolean
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_music_player)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )

            fun actionPI(action: String, req: Int): PendingIntent =
                PendingIntent.getService(
                    context, req,
                    Intent(context, MusicService::class.java).apply { this.action = action },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

            views.setOnClickPendingIntent(R.id.widget_prev, actionPI(MusicService.ACTION_PREV, 10))
            views.setOnClickPendingIntent(R.id.widget_play_pause, actionPI(MusicService.ACTION_TOGGLE_PLAY, 11))
            views.setOnClickPendingIntent(R.id.widget_next, actionPI(MusicService.ACTION_NEXT, 12))

            val openIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, openIntent)

            return views
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            val views = buildRemoteViews(context, "Music Player", "", false)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
