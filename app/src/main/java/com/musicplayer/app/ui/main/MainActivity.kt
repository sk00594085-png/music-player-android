package com.musicplayer.app.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.musicplayer.app.R
import com.musicplayer.app.databinding.ActivityMainBinding
import com.musicplayer.app.viewmodel.MusicViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: MusicViewModel
        private set

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            viewModel.loadLibrary()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        viewModel.bindService()

        setupNavigation()
        requestPermissions()
        setupMiniPlayer()
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController: NavController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)
    }

    private fun requestPermissions() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            viewModel.loadLibrary()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun setupMiniPlayer() {
        viewModel.currentSong.observe(this) { song ->
            if (song != null) {
                binding.miniPlayer.root.visibility = View.VISIBLE
                binding.miniPlayerTitle.text = song.displayTitle
                binding.miniPlayerArtist.text = song.displayArtist
            } else {
                binding.miniPlayer.root.visibility = View.GONE
            }
        }

        viewModel.isPlaying.observe(this) { playing ->
            binding.miniPlayerPlayPause.setImageResource(
                if (playing) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        binding.miniPlayerPlayPause.setOnClickListener { viewModel.togglePlayPause() }
        binding.miniPlayerNext.setOnClickListener { viewModel.playNext() }

        binding.miniPlayer.root.setOnClickListener {
            startActivity(
                android.content.Intent(this, com.musicplayer.app.ui.nowplaying.NowPlayingActivity::class.java)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unbindService()
    }
}
