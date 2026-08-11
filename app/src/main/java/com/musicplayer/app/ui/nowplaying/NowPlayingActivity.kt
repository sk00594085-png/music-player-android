package com.musicplayer.app.ui.nowplaying

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.musicplayer.app.R
import com.musicplayer.app.databinding.ActivityNowPlayingBinding
import com.musicplayer.app.model.RepeatMode
import com.musicplayer.app.service.MusicService
import com.musicplayer.app.viewmodel.MusicViewModel
import android.graphics.Bitmap

class NowPlayingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNowPlayingBinding
    private lateinit var viewModel: MusicViewModel
    private val handler = Handler(Looper.getMainLooper())
    private var seeking = false

    private val positionRunnable = object : Runnable {
        override fun run() {
            if (!seeking) viewModel.pollPosition()
            handler.postDelayed(this, 500)
        }
    }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            // ViewModel already handles this via its own receiver; just trigger UI sync
            updateProgressBar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNowPlayingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        viewModel.bindService()

        setupToolbar()
        setupControls()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener { viewModel.togglePlayPause() }
        binding.btnNext.setOnClickListener { viewModel.playNext() }
        binding.btnPrevious.setOnClickListener { viewModel.playPrevious() }
        binding.btnRepeat.setOnClickListener { viewModel.cycleRepeatMode() }
        binding.btnShuffle.setOnClickListener { viewModel.toggleShuffle() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.tvCurrentTime.text = formatTime(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar) { seeking = true }
            override fun onStopTrackingTouch(sb: SeekBar) {
                seeking = false
                viewModel.seekTo(sb.progress)
            }
        })
    }

    private fun observeViewModel() {
        viewModel.currentSong.observe(this) { song ->
            if (song == null) return@observe
            binding.tvSongTitle.text = song.displayTitle
            binding.tvArtistName.text = song.displayArtist
            binding.tvAlbumName.text = song.album.ifBlank { "Unknown Album" }

            // Album art with palette-driven background
            Glide.with(this)
                .asBitmap()
                .load(song.albumArtUri)
                .apply(RequestOptions().error(R.drawable.ic_album_art_placeholder))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(bmp: Bitmap, t: Transition<in Bitmap>?) {
                        binding.albumArt.setImageBitmap(bmp)
                        Palette.from(bmp).generate { palette ->
                            palette?.let { applyPalette(it) }
                        }
                    }
                    override fun onLoadCleared(p: Drawable?) {
                        binding.albumArt.setImageResource(R.drawable.ic_album_art_placeholder)
                    }
                })
        }

        viewModel.isPlaying.observe(this) { playing ->
            binding.btnPlayPause.setImageResource(
                if (playing) R.drawable.ic_pause_circle else R.drawable.ic_play_circle
            )
        }

        viewModel.repeatMode.observe(this) { mode ->
            val (icon, alpha) = when (mode) {
                RepeatMode.NONE -> Pair(R.drawable.ic_repeat, 0.4f)
                RepeatMode.ALL -> Pair(R.drawable.ic_repeat, 1f)
                RepeatMode.ONE -> Pair(R.drawable.ic_repeat_one, 1f)
            }
            binding.btnRepeat.setImageResource(icon)
            binding.btnRepeat.alpha = alpha
        }

        viewModel.shuffleEnabled.observe(this) { shuffle ->
            binding.btnShuffle.alpha = if (shuffle) 1f else 0.4f
        }

        viewModel.duration.observe(this) { dur ->
            binding.seekBar.max = dur
            binding.tvTotalTime.text = formatTime(dur)
        }

        viewModel.position.observe(this) { pos ->
            if (!seeking) {
                binding.seekBar.progress = pos
                binding.tvCurrentTime.text = formatTime(pos)
            }
        }

        viewModel.currentQueue.observe(this) { queue ->
            val idx = viewModel.currentIndex.value ?: -1
            if (idx >= 0 && idx < queue.size) {
                binding.tvTrackInfo.text = "${idx + 1} / ${queue.size}"
            }
        }
    }

    private fun applyPalette(palette: Palette) {
        val swatch = palette.darkVibrantSwatch
            ?: palette.darkMutedSwatch
            ?: palette.dominantSwatch
        swatch?.let {
            binding.nowPlayingRoot.setBackgroundColor(it.rgb)
            binding.tvSongTitle.setTextColor(it.titleTextColor)
            binding.tvArtistName.setTextColor(it.bodyTextColor)
        }
    }

    private fun updateProgressBar() {
        if (!seeking) {
            val pos = viewModel.position.value ?: 0
            binding.seekBar.progress = pos
            binding.tvCurrentTime.text = formatTime(pos)
        }
    }

    private fun formatTime(ms: Int): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%d:%02d".format(mins, secs)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(stateReceiver, IntentFilter(MusicService.BROADCAST_STATE))
        handler.post(positionRunnable)
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(stateReceiver) } catch (_: Exception) {}
        handler.removeCallbacks(positionRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unbindService()
    }
}
