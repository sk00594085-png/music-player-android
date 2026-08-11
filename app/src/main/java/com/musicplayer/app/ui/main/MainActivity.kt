package com.musicplayer.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.musicplayer.app.MusicPlayerApp
import com.musicplayer.app.R
import com.musicplayer.app.databinding.ActivityMainBinding
import com.musicplayer.app.ui.carmode.CarModeActivity
import com.musicplayer.app.ui.settings.SettingsActivity
import com.musicplayer.app.ui.sleeptimer.SleepTimerDialog
import com.musicplayer.app.utils.AppPreferences
import com.musicplayer.app.viewmodel.MusicViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Use the single app-scoped ViewModel so NowPlayingActivity shares the same instance
    val viewModel: MusicViewModel
        get() = (application as MusicPlayerApp).musicViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            viewModel.loadLibrary()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before super.onCreate
        AppCompatDelegate.setDefaultNightMode(AppPreferences.getThemeMode(this))

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupNavigation()
        requestPermissions()
        setupMiniPlayer()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sleep_timer -> {
                SleepTimerDialog().show(supportFragmentManager, "sleep_timer")
                true
            }
            R.id.action_car_mode -> {
                startActivity(Intent(this, CarModeActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
            offerResumeLastPosition()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun offerResumeLastPosition() {
        val (lastIndex, lastPos) = viewModel.getLastPlaybackState()
        if (lastIndex >= 0 && lastPos > 0) {
            viewModel.songs.observe(this) { songs ->
                if (songs.isNotEmpty() && lastIndex < songs.size) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.resume_playback)
                        .setMessage(R.string.resume_playback_message)
                        .setPositiveButton(R.string.resume) { _, _ ->
                            viewModel.resumeLastPosition()
                        }
                        .setNegativeButton(R.string.start_fresh, null)
                        .show()
                }
            }
        }
    }

    private fun setupMiniPlayer() {
        viewModel.currentSong.observe(this) { song ->
            if (song != null) {
                binding.miniPlayer.visibility = View.VISIBLE
                binding.miniPlayerTitle.text = song.displayTitle
                binding.miniPlayerArtist.text = song.displayArtist
                Glide.with(this)
                    .load(song.albumArtUri)
                    .placeholder(R.drawable.ic_album_art_placeholder)
                    .error(R.drawable.ic_album_art_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .into(binding.miniPlayerArt)
            } else {
                binding.miniPlayer.visibility = View.GONE
            }
        }

        viewModel.isPlaying.observe(this) { playing ->
            binding.miniPlayerPlayPause.setImageResource(
                if (playing) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        binding.miniPlayerPlayPause.setOnClickListener { viewModel.togglePlayPause() }
        binding.miniPlayerNext.setOnClickListener { viewModel.playNext() }

        // Tap anywhere on mini player (except buttons) → open Now Playing
        binding.miniPlayer.setOnClickListener {
            startActivity(
                Intent(this, com.musicplayer.app.ui.nowplaying.NowPlayingActivity::class.java)
            )
        }
    }

    // NOTE: Service binding is managed by MusicPlayerApp — never unbind here.
}
