package com.musicplayer.app.ui.carmode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.musicplayer.app.R
import com.musicplayer.app.databinding.ActivityCarModeBinding
import com.musicplayer.app.service.MusicService
import com.musicplayer.app.viewmodel.MusicViewModel

class CarModeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarModeBinding
    private lateinit var viewModel: MusicViewModel
    private var receiverRegistered = false

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            syncUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        viewModel.bindService()

        binding.btnCarBack.setOnClickListener { finish() }
        binding.btnCarPlayPause.setOnClickListener { viewModel.togglePlayPause() }
        binding.btnCarNext.setOnClickListener { viewModel.playNext() }
        binding.btnCarPrev.setOnClickListener { viewModel.playPrevious() }
        binding.btnCarShuffle.setOnClickListener { viewModel.toggleShuffle() }
        binding.btnCarRepeat.setOnClickListener { viewModel.cycleRepeatMode() }

        viewModel.currentSong.observe(this) { song ->
            binding.tvCarTitle.text = song?.displayTitle ?: getString(R.string.not_playing)
            binding.tvCarArtist.text = song?.displayArtist ?: ""
        }

        viewModel.isPlaying.observe(this) { playing ->
            binding.btnCarPlayPause.setImageResource(
                if (playing) R.drawable.ic_pause_circle else R.drawable.ic_play_circle
            )
        }

        viewModel.shuffleEnabled.observe(this) { shuffle ->
            binding.btnCarShuffle.alpha = if (shuffle) 1f else 0.4f
        }
    }

    private fun syncUI() {
        // Triggered by broadcast — LiveData handles the rest
    }

    override fun onResume() {
        super.onResume()
        if (!receiverRegistered) {
            registerReceiver(stateReceiver, IntentFilter(MusicService.BROADCAST_STATE))
            receiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (receiverRegistered) {
            try { unregisterReceiver(stateReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unbindService()
    }
}
