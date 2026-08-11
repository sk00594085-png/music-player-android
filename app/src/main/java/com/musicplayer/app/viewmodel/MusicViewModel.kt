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
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.db.AppDatabase
import com.musicplayer.app.db.FavouriteEntity
import com.musicplayer.app.db.PlayCountEntity
import com.musicplayer.app.db.PlaylistEntity
import com.musicplayer.app.db.PlaylistSongEntity
import com.musicplayer.app.db.RecentlyPlayedEntity
import com.musicplayer.app.model.Folder
import com.musicplayer.app.model.RepeatMode
import com.musicplayer.app.model.Song
import com.musicplayer.app.repository.SongRepository
import com.musicplayer.app.service.MusicService
import com.musicplayer.app.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SongRepository(application)
    private val db = AppDatabase.getInstance(application)

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

    private val _playbackSpeed = MutableLiveData(1.0f)
    val playbackSpeed: LiveData<Float> = _playbackSpeed

    // ── Room – Playlists ──────────────────────────────────────────────────
    val playlists: LiveData<List<PlaylistEntity>> = db.playlistDao().getAllPlaylists()

    // ── Room – Favourites ─────────────────────────────────────────────────
    private val _favouriteSongs = MutableLiveData<List<Song>>(emptyList())
    val favouriteSongs: LiveData<List<Song>> = _favouriteSongs

    private val _isFavourite = MutableLiveData(false)
    val isFavourite: LiveData<Boolean> = _isFavourite

    // ── Room – Recently Played ────────────────────────────────────────────
    private val _recentlyPlayedSongs = MutableLiveData<List<Song>>(emptyList())
    val recentlyPlayedSongs: LiveData<List<Song>> = _recentlyPlayedSongs

    // ── Room – Most Played ────────────────────────────────────────────────
    private val _mostPlayedSongs = MutableLiveData<List<Song>>(emptyList())
    val mostPlayedSongs: LiveData<List<Song>> = _mostPlayedSongs

    // ── Service binding ───────────────────────────────────────────────────
    var musicService: MusicService? = null
        private set

    private val _serviceBound = MutableLiveData(false)
    val serviceBound: LiveData<Boolean> = _serviceBound

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
            _playbackSpeed.value = intent.getFloatExtra(MusicService.EXTRA_PLAYBACK_SPEED, 1.0f)

            val idx = _currentIndex.value ?: -1
            val queue = musicService?.getQueue() ?: emptyList()
            val prevSong = _currentSong.value
            val newSong = queue.getOrNull(idx)
            _currentSong.value = newSong
            _currentQueue.value = queue

            // When a new song starts playing, track recently played and play count
            if (newSong != null && newSong.id != prevSong?.id &&
                intent.getBooleanExtra(MusicService.EXTRA_IS_PLAYING, false)) {
                trackSongPlayed(newSong)
            }

            // Refresh favourite status for current song
            newSong?.let { checkFavouriteStatus(it.id) }
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
                refreshFavourites()
                refreshRecentlyPlayed()
                refreshMostPlayed()
            } catch (_: Exception) {
                _songs.value = emptyList()
                _folders.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    // ── Service binding ───────────────────────────────────────────────────

    fun bindService() {
        if (isBound) return
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

    fun setPlaybackSpeed(speed: Float) {
        musicService?.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }

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
            _playbackSpeed.value = svc.getPlaybackSpeed()
            val q = svc.getQueue()
            _currentQueue.value = q
            _currentSong.value = q.getOrNull(svc.getCurrentIndex())
            _currentSong.value?.let { checkFavouriteStatus(it.id) }
        }
    }

    // ── Playlists ─────────────────────────────────────────────────────────

    fun createPlaylist(name: String) = viewModelScope.launch {
        db.playlistDao().insertPlaylist(PlaylistEntity(name = name))
    }

    fun renamePlaylist(playlist: PlaylistEntity, newName: String) = viewModelScope.launch {
        db.playlistDao().updatePlaylist(playlist.copy(name = newName))
    }

    fun deletePlaylist(playlist: PlaylistEntity) = viewModelScope.launch {
        db.playlistDao().deletePlaylist(playlist)
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) = viewModelScope.launch {
        db.playlistDao().addSongToPlaylist(
            PlaylistSongEntity(playlistId = playlistId, songId = song.id, songPath = song.path)
        )
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) = viewModelScope.launch {
        db.playlistDao().removeSongFromPlaylist(playlistId, songId)
    }

    fun getPlaylistSongs(playlistId: Long): LiveData<List<Song>> = liveData(Dispatchers.IO) {
        val entries = db.playlistDao().getSongsInPlaylist(playlistId)
        val allSongs = _songs.value ?: emptyList()
        val songMap = allSongs.associateBy { it.id }
        emit(entries.mapNotNull { songMap[it.songId] })
    }

    suspend fun getAllPlaylistsForBackup(): List<PlaylistEntity> =
        db.playlistDao().getAllPlaylistsSync()

    suspend fun getPlaylistSongsForBackup(playlistId: Long): List<PlaylistSongEntity> =
        db.playlistDao().getSongsInPlaylist(playlistId)

    fun restorePlaylists(
        playlists: List<PlaylistEntity>,
        songs: List<PlaylistSongEntity>
    ) = viewModelScope.launch {
        for (pl in playlists) {
            val newId = db.playlistDao().insertPlaylist(pl.copy(id = 0))
            val matching = songs.filter { it.playlistId == pl.id }
            for (s in matching) {
                db.playlistDao().addSongToPlaylist(s.copy(playlistId = newId))
            }
        }
    }

    // ── Favourites ────────────────────────────────────────────────────────

    fun toggleFavourite(song: Song) = viewModelScope.launch {
        val isFav = db.favouriteDao().isFavourite(song.id)
        if (isFav) {
            db.favouriteDao().removeFavourite(song.id)
        } else {
            db.favouriteDao().addFavourite(FavouriteEntity(songId = song.id, songPath = song.path))
        }
        _isFavourite.postValue(!isFav)
        refreshFavourites()
    }

    private fun checkFavouriteStatus(songId: Long) = viewModelScope.launch {
        _isFavourite.postValue(db.favouriteDao().isFavourite(songId))
    }

    private fun refreshFavourites() = viewModelScope.launch {
        val favs = db.favouriteDao().getAllFavouritesSync()
        val allSongs = _songs.value ?: emptyList()
        val songMap = allSongs.associateBy { it.id }
        _favouriteSongs.postValue(favs.mapNotNull { songMap[it.songId] })
    }

    // ── Recently Played ───────────────────────────────────────────────────

    private fun trackSongPlayed(song: Song) = viewModelScope.launch {
        db.recentlyPlayedDao().insertOrUpdate(
            RecentlyPlayedEntity(songId = song.id, songPath = song.path)
        )
        db.recentlyPlayedDao().trimToLimit()

        // Increment play count
        val existing = db.playCountDao().getPlayCount(song.id)
        if (existing == null) {
            db.playCountDao().upsert(PlayCountEntity(songId = song.id, songPath = song.path))
        } else {
            db.playCountDao().increment(song.id)
        }

        refreshRecentlyPlayed()
        refreshMostPlayed()
    }

    private fun refreshRecentlyPlayed() = viewModelScope.launch {
        val recent = db.recentlyPlayedDao().getRecentlyPlayed()
        val allSongs = _songs.value ?: emptyList()
        val songMap = allSongs.associateBy { it.id }
        _recentlyPlayedSongs.postValue(recent.mapNotNull { songMap[it.songId] })
    }

    // ── Most Played ───────────────────────────────────────────────────────

    private fun refreshMostPlayed() = viewModelScope.launch {
        val top = db.playCountDao().getTopPlayed()
        val allSongs = _songs.value ?: emptyList()
        val songMap = allSongs.associateBy { it.id }
        _mostPlayedSongs.postValue(top.mapNotNull { songMap[it.songId] })
    }

    // ── Last position (remember & restore) ───────────────────────────────

    fun getLastPlaybackState(): Pair<Int, Int> {
        val ctx = getApplication<Application>()
        return Pair(
            AppPreferences.getLastSongIndex(ctx),
            AppPreferences.getLastSongPosition(ctx)
        )
    }

    fun resumeLastPosition() {
        val (index, position) = getLastPlaybackState()
        val songs = _songs.value ?: return
        if (index >= 0 && index < songs.size) {
            musicService?.setQueueAndPlay(ArrayList(songs), index)
            musicService?.seekTo(position)
        }
    }

    override fun onCleared() {
        super.onCleared()
        unbindService()
    }
}
