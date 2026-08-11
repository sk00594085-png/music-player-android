package com.musicplayer.app.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.model.Folder
import com.musicplayer.app.model.RepeatMode
import com.musicplayer.app.model.Song
import com.musicplayer.app.repository.SongRepository
import com.musicplayer.app.service.MusicService
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SongRepository(application)

    // ── Library ──────────────────────────────────────────────────────────
    private val _songs = MutableLiveData<List<Song>>(emptyList())
    val songs: LiveData<List<Song>> = _songs

    private val _folders = MutableLiveData<List<Folder>>(emptyList())
    val folders: LiveData<List<Folder>> = _folders

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // ── Playback state ────────────────────────────────────────────────────
    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _currentSong = MutableLiveData<Song?>(null)
    val currentSong: LiveData<Song?> = _currentSong

    private val _currentIndex = MutableLiveData(-1)
    val currentIndex: LiveData<Int> = _currentIndex

    private val _repeatMode = MutableLiveData(RepeatMode.NONE)
    val repeatMode: LiveData<RepeatMode> = _repeatMode

    private val _shuffleEnabled = MutableLiveData(false)
    val shuffleEnabled: LiveData<Boolean> = _shuffleEnabled

    private val _duration = MutableLiveData(0)
    val duration: LiveData<Int> = _duration

    private val _position = MutableLiveData(0)
    val position: LiveData<Int> = _position

    private val _currentQueue = MutableLiveData<List<Song>>(emptyList())
    val currentQueue: LiveData<List<Song>> = _currentQueue

    // ── Service binding ───────────────────────────────────────────────────
    var musicService: MusicService? = null
        private set

    private val _serviceBound = MutableLiveData(false)
    val serviceBound: LiveData<Boolean> = _serviceBound

    // Guard against double bind/unbind
    private var isBound = false
    private var receiverRegistered = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            musicService = (binder as MusicService.MusicBinder).getService()
            _serviceBound.value = true
            syncFromService()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            musicService = null
            _serviceBound.value = false
            isBound = false
        }
    }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            _isPlaying.value = intent.getBooleanExtra(MusicService.EXTRA_IS_PLAYING, false)
            _currentIndex.value = intent.getIntExtra(MusicService.EXTRA_CURRENT_INDEX, -1)
            _repeatMode.value = try {
                RepeatMode.valueOf(
                    intent.getStringExtra(MusicService.EXTRA_REPEAT_MODE) ?: RepeatMode.NONE.name
                )
            } catch (_: Exception) { RepeatMode.NONE }
            _shuffleEnabled.value = intent.getBooleanExtra(MusicService.EXTRA_IS_SHUFFLE, false)
            _duration.value = intent.getIntExtra(MusicService.EXTRA_DURATION, 0)
            _position.value = intent.getIntExtra(MusicService.EXTRA_POSITION, 0)

            val idx = _currentIndex.value ?: -1
            val queue = musicService?.getQueue() ?: emptyList()
            _currentSong.value = queue.getOrNull(idx)
            _currentQueue.value = queue
        }
    }

    init {
        loadLibrary()
    }

    // ── Library ───────────────────────────────────────────────────────────

    fun loadLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _songs.value = repo.getAllSongs()
                _folders.value = repo.getFolders()
            } catch (_: Exception) {
                _songs.value = emptyList()
                _folders.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    // ── Service binding ───────────────────────────────────────────────────

    fun bindService() {
        if (isBound) return   // prevent double-bind
        val ctx = getApplication<Application>()
        try {
            val intent = Intent(ctx, MusicService::class.java)
            ctx.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            isBound = true
        } catch (_: Exception) {}

        if (!receiverRegistered) {
            try {
                ctx.registerReceiver(stateReceiver, IntentFilter(MusicService.BROADCAST_STATE))
                receiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    fun unbindService() {
        val ctx = getApplication<Application>()
        if (receiverRegistered) {
            try { ctx.unregisterReceiver(stateReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
        if (isBound) {
            try { ctx.unbindService(serviceConnection) } catch (_: Exception) {}
            isBound = false
        }
        musicService = null
    }

    // ── Playback ──────────────────────────────────────────────────────────

    fun playSongsAt(songs: List<Song>, index: Int) {
        musicService?.setQueueAndPlay(ArrayList(songs), index)
    }

    fun togglePlayPause() { musicService?.togglePlayPause() }
    fun playNext() { musicService?.playNext() }
    fun playPrevious() { musicService?.playPrevious() }
    fun seekTo(position: Int) { musicService?.seekTo(position) }
    fun cycleRepeatMode() { musicService?.cycleRepeatMode() }
    fun toggleShuffle() { musicService?.toggleShuffle() }

    fun pollPosition() {
        musicService?.let {
            _position.value = it.getCurrentPosition()
        }
    }

    private fun syncFromService() {
        musicService?.let { svc ->
            _isPlaying.value = svc.isPlaying()
            _currentIndex.value = svc.getCurrentIndex()
            _repeatMode.value = svc.getRepeatMode()
            _shuffleEnabled.value = svc.isShuffleEnabled()
            _duration.value = svc.getDuration()
            _position.value = svc.getCurrentPosition()
            val q = svc.getQueue()
            _currentQueue.value = q
            _currentSong.value = q.getOrNull(svc.getCurrentIndex())
        }
    }

    override fun onCleared() {
        super.onCleared()
        unbindService()
    }
}
