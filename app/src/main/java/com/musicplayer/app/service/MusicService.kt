package com.musicplayer.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.musicplayer.app.R
import com.musicplayer.app.model.RepeatMode
import com.musicplayer.app.model.Song
import com.musicplayer.app.ui.main.MainActivity
import java.io.IOException

/**
 * Foreground service that drives audio playback.
 *
 * Responsibilities:
 * - MediaPlayer lifecycle (prepare, play, pause, seek, next, previous)
 * - AudioFocus management (pauses on call, ducks on notification)
 * - Bluetooth SCO / A2DP awareness (auto-pause when headset disconnects)
 * - Foreground notification with transport controls
 * - MediaSession for lock-screen / Bluetooth controls
 */
class MusicService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    companion object {
        private const val TAG = "MusicService"

        const val ACTION_PLAY = "com.musicplayer.ACTION_PLAY"
        const val ACTION_PAUSE = "com.musicplayer.ACTION_PAUSE"
        const val ACTION_TOGGLE_PLAY = "com.musicplayer.ACTION_TOGGLE_PLAY"
        const val ACTION_NEXT = "com.musicplayer.ACTION_NEXT"
        const val ACTION_PREV = "com.musicplayer.ACTION_PREV"
        const val ACTION_SEEK = "com.musicplayer.ACTION_SEEK"
        const val ACTION_SET_QUEUE = "com.musicplayer.ACTION_SET_QUEUE"
        const val ACTION_STOP = "com.musicplayer.ACTION_STOP"
        const val ACTION_CYCLE_REPEAT = "com.musicplayer.ACTION_CYCLE_REPEAT"
        const val ACTION_TOGGLE_SHUFFLE = "com.musicplayer.ACTION_TOGGLE_SHUFFLE"

        const val EXTRA_SONG_INDEX = "extra_song_index"
        const val EXTRA_SEEK_POSITION = "extra_seek_position"
        const val EXTRA_QUEUE = "extra_queue"

        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 101

        // Broadcast sent to UI so bound + unbound clients can observe state
        const val BROADCAST_STATE = "com.musicplayer.BROADCAST_STATE"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_CURRENT_INDEX = "extra_current_index"
        const val EXTRA_REPEAT_MODE = "extra_repeat_mode"
        const val EXTRA_IS_SHUFFLE = "extra_is_shuffle"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_POSITION = "extra_position"
    }

    // ──────────────────────────────────────────────────────────────────────
    // State
    // ──────────────────────────────────────────────────────────────────────

    private var mediaPlayer: MediaPlayer? = null
    private var queue: ArrayList<Song> = ArrayList()
    private var shuffleQueue: ArrayList<Song> = ArrayList()
    private var currentIndex: Int = -1
    private var repeatMode: RepeatMode = RepeatMode.NONE
    private var shuffleEnabled: Boolean = false
    private var isPrepared: Boolean = false
    private var pendingPlay: Boolean = false   // play as soon as prepared

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false

    private lateinit var mediaSession: MediaSessionCompat

    private val binder = MusicBinder()

    // ──────────────────────────────────────────────────────────────────────
    // Bluetooth receiver – pause when audio output is disconnected
    // ──────────────────────────────────────────────────────────────────────

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    if (isPlaying()) pausePlayback()
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        setupMediaSession()
        registerBtReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY

        when (intent.action) {
            ACTION_SET_QUEUE -> {
                @Suppress("UNCHECKED_CAST")
                val songs = intent.getSerializableExtra(EXTRA_QUEUE) as? ArrayList<Song>
                val index = intent.getIntExtra(EXTRA_SONG_INDEX, 0)
                if (songs != null) {
                    setQueueAndPlay(songs, index)
                }
            }
            ACTION_TOGGLE_PLAY -> togglePlayPause()
            ACTION_PLAY -> resumePlayback()
            ACTION_PAUSE -> pausePlayback()
            ACTION_NEXT -> playNext()
            ACTION_PREV -> playPrevious()
            ACTION_SEEK -> {
                val pos = intent.getIntExtra(EXTRA_SEEK_POSITION, 0)
                seekTo(pos)
            }
            ACTION_STOP -> stopSelf()
            ACTION_CYCLE_REPEAT -> cycleRepeatMode()
            ACTION_TOGGLE_SHUFFLE -> toggleShuffle()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        abandonAudioFocus()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession.release()
        unregisterReceiver(bluetoothReceiver)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Public API (used by bound activities/viewmodels)
    // ──────────────────────────────────────────────────────────────────────

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    fun getCurrentSong(): Song? = effectiveQueue().getOrNull(currentIndex)

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentPosition(): Int = if (isPrepared) mediaPlayer?.currentPosition ?: 0 else 0

    fun getDuration(): Int = if (isPrepared) mediaPlayer?.duration ?: 0 else 0

    fun getRepeatMode(): RepeatMode = repeatMode

    fun isShuffleEnabled(): Boolean = shuffleEnabled

    fun getQueue(): List<Song> = effectiveQueue().toList()

    fun getCurrentIndex(): Int = currentIndex

    // ──────────────────────────────────────────────────────────────────────
    // Playback control
    // ──────────────────────────────────────────────────────────────────────

    fun setQueueAndPlay(songs: ArrayList<Song>, index: Int) {
        queue = songs
        buildShuffleQueue()
        currentIndex = index
        prepareSong(effectiveQueue()[currentIndex])
    }

    fun togglePlayPause() {
        if (isPlaying()) pausePlayback() else resumePlayback()
    }

    fun resumePlayback() {
        if (!isPrepared) return
        if (requestAudioFocus()) {
            mediaPlayer?.start()
            updateNotification()
            broadcastState()
            updateMediaSessionState()
        }
    }

    fun pausePlayback() {
        mediaPlayer?.pause()
        abandonAudioFocus()
        updateNotification()
        broadcastState()
        updateMediaSessionState()
    }

    fun playNext() {
        val q = effectiveQueue()
        if (q.isEmpty()) return
        currentIndex = when {
            repeatMode == RepeatMode.ONE -> currentIndex       // stay on same
            currentIndex < q.size - 1 -> currentIndex + 1
            repeatMode == RepeatMode.ALL -> 0
            else -> return                                      // end of queue, stop
        }
        prepareSong(q[currentIndex])
    }

    fun playPrevious() {
        val q = effectiveQueue()
        if (q.isEmpty()) return
        // If more than 3 s have passed, restart current track
        if (getCurrentPosition() > 3000) {
            seekTo(0)
            return
        }
        currentIndex = when {
            currentIndex > 0 -> currentIndex - 1
            repeatMode == RepeatMode.ALL -> q.size - 1
            else -> 0
        }
        prepareSong(q[currentIndex])
    }

    fun seekTo(position: Int) {
        if (isPrepared) {
            mediaPlayer?.seekTo(position)
            broadcastState()
        }
    }

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        broadcastState()
        updateNotification()
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        buildShuffleQueue()
        // Re-map currentIndex to the new effective queue
        val currentSong = getCurrentSong()
        currentIndex = effectiveQueue().indexOf(currentSong).takeIf { it >= 0 } ?: 0
        broadcastState()
    }

    // ──────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────

    private fun effectiveQueue(): ArrayList<Song> =
        if (shuffleEnabled) shuffleQueue else queue

    private fun buildShuffleQueue() {
        shuffleQueue = ArrayList(queue.shuffled())
    }

    private fun prepareSong(song: Song) {
        isPrepared = false
        pendingPlay = true

        mediaPlayer?.reset() ?: run {
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener(this@MusicService)
                setOnCompletionListener(this@MusicService)
                setOnErrorListener(this@MusicService)
            }
        }

        try {
            mediaPlayer!!.apply {
                reset()
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener(this@MusicService)
                setOnCompletionListener(this@MusicService)
                setOnErrorListener(this@MusicService)
                setDataSource(song.path)
                prepareAsync()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to set data source: ${e.message}")
        }

        broadcastState()
    }

    // MediaPlayer.OnPreparedListener
    override fun onPrepared(mp: MediaPlayer) {
        isPrepared = true
        if (pendingPlay) {
            pendingPlay = false
            if (requestAudioFocus()) {
                mp.start()
            }
        }
        updateNotification()
        broadcastState()
        updateMediaSessionState()
    }

    // MediaPlayer.OnCompletionListener
    override fun onCompletion(mp: MediaPlayer) {
        when (repeatMode) {
            RepeatMode.ONE -> {
                mp.seekTo(0)
                mp.start()
            }
            RepeatMode.ALL -> playNext()
            RepeatMode.NONE -> {
                val q = effectiveQueue()
                if (currentIndex < q.size - 1) {
                    playNext()
                } else {
                    // End of queue – stay on last song but paused
                    mp.seekTo(0)
                    broadcastState()
                    updateNotification()
                }
            }
        }
    }

    // MediaPlayer.OnErrorListener
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
        isPrepared = false
        return false
    }

    // ──────────────────────────────────────────────────────────────────────
    // Audio Focus
    // ──────────────────────────────────────────────────────────────────────

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                mediaPlayer?.setVolume(1f, 1f)
                if (!isPlaying()) resumePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                pausePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPlaying()) pausePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setAcceptsDelayedFocusGain(false)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }

    // ──────────────────────────────────────────────────────────────────────
    // Bluetooth / Becoming Noisy receiver
    // ──────────────────────────────────────────────────────────────────────

    private fun registerBtReceiver() {
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    // ──────────────────────────────────────────────────────────────────────
    // MediaSession
    // ──────────────────────────────────────────────────────────────────────

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, TAG).apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = resumePlayback()
                override fun onPause() = pausePlayback()
                override fun onSkipToNext() = playNext()
                override fun onSkipToPrevious() = playPrevious()
                override fun onSeekTo(pos: Long) = seekTo(pos.toInt())
                override fun onStop() = stopSelf()
            })
            isActive = true
        }
    }

    private fun updateMediaSessionState() {
        val state = if (isPlaying()) PlaybackStateCompat.STATE_PLAYING
        else PlaybackStateCompat.STATE_PAUSED

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO or
                            PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, getCurrentPosition().toLong(), 1f)
                .build()
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // Broadcast state to UI
    // ──────────────────────────────────────────────────────────────────────

    fun broadcastState() {
        val intent = Intent(BROADCAST_STATE).apply {
            putExtra(EXTRA_IS_PLAYING, isPlaying())
            putExtra(EXTRA_CURRENT_INDEX, currentIndex)
            putExtra(EXTRA_REPEAT_MODE, repeatMode.name)
            putExtra(EXTRA_IS_SHUFFLE, shuffleEnabled)
            putExtra(EXTRA_DURATION, getDuration())
            putExtra(EXTRA_POSITION, getCurrentPosition())
        }
        sendBroadcast(intent)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Notification
    // ──────────────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val song = getCurrentSong()
        val contentTitle = song?.displayTitle ?: "Music Player"
        val contentText = song?.displayArtist ?: "No track"

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionPendingIntent(action: String, reqCode: Int): PendingIntent =
            PendingIntent.getService(
                this, reqCode,
                Intent(this, MusicService::class.java).apply { this.action = action },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(
                R.drawable.ic_skip_previous, "Previous",
                actionPendingIntent(ACTION_PREV, 1)
            )
            .addAction(
                if (isPlaying()) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying()) "Pause" else "Play",
                actionPendingIntent(ACTION_TOGGLE_PLAY, 2)
            )
            .addAction(
                R.drawable.ic_skip_next, "Next",
                actionPendingIntent(ACTION_NEXT, 3)
            )
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        if (!isPlaying()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        }
    }
}
