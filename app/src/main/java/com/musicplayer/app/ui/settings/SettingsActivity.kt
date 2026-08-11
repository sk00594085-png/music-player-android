package com.musicplayer.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import com.musicplayer.app.R
import com.musicplayer.app.databinding.ActivitySettingsBinding
import com.musicplayer.app.utils.AppPreferences
import com.musicplayer.app.viewmodel.MusicViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.musicplayer.app.db.PlaylistEntity
import com.musicplayer.app.db.PlaylistSongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: MusicViewModel

    data class PlaylistBackup(
        val playlists: List<PlaylistEntity>,
        val songs: List<PlaylistSongEntity>
    )

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importPlaylists(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MusicViewModel::class.java]

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupPlaybackSection()
        setupLibrarySection()
        setupAppearanceSection()
        setupBackupSection()
    }

    private fun setupPlaybackSection() {
        // Crossfade
        val crossfade = AppPreferences.getCrossfadeSeconds(this)
        binding.seekCrossfade.max = 10
        binding.seekCrossfade.progress = crossfade
        binding.tvCrossfadeValue.text = getString(R.string.seconds_value, crossfade)
        binding.seekCrossfade.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar, p: Int, fromUser: Boolean) {
                AppPreferences.setCrossfadeSeconds(this@SettingsActivity, p)
                binding.tvCrossfadeValue.text = getString(R.string.seconds_value, p)
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar) {}
        })

        // Auto-play Bluetooth
        binding.switchAutoPlayBt.isChecked = AppPreferences.isAutoPlayBluetooth(this)
        binding.switchAutoPlayBt.setOnCheckedChangeListener { _, checked ->
            AppPreferences.setAutoPlayBluetooth(this, checked)
        }
    }

    private fun setupLibrarySection() {
        // Min duration
        val minDur = AppPreferences.getMinDurationSeconds(this)
        binding.seekMinDuration.max = 120
        binding.seekMinDuration.progress = minDur
        binding.tvMinDurationValue.text = getString(R.string.seconds_value, minDur)
        binding.seekMinDuration.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar, p: Int, fromUser: Boolean) {
                AppPreferences.setMinDurationSeconds(this@SettingsActivity, p)
                binding.tvMinDurationValue.text = getString(R.string.seconds_value, p)
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar) {}
        })

        // Rescan library button
        binding.btnRescanLibrary.setOnClickListener {
            viewModel.loadLibrary()
            Toast.makeText(this, R.string.library_rescanning, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAppearanceSection() {
        val currentTheme = AppPreferences.getThemeMode(this)
        val idx = when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> 0
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            else -> 2
        }
        binding.rgTheme.check(
            when (idx) {
                0 -> R.id.rb_theme_light
                1 -> R.id.rb_theme_dark
                else -> R.id.rb_theme_system
            }
        )
        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rb_theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rb_theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppPreferences.setThemeMode(this, mode)
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun setupBackupSection() {
        binding.btnExportPlaylists.setOnClickListener { exportPlaylists() }
        binding.btnImportPlaylists.setOnClickListener {
            importLauncher.launch("application/json")
        }
    }

    private fun exportPlaylists() {
        CoroutineScope(Dispatchers.IO).launch {
            val playlists = viewModel.getAllPlaylistsForBackup()
            val songEntries = mutableListOf<PlaylistSongEntity>()
            for (pl in playlists) {
                songEntries.addAll(viewModel.getPlaylistSongsForBackup(pl.id))
            }
            val backup = PlaylistBackup(playlists, songEntries)
            val json = Gson().toJson(backup)
            try {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, "music_playlists_backup.json")
                file.writeText(json)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity,
                        getString(R.string.export_success, file.absolutePath),
                        Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, R.string.export_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun importPlaylists(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@launch
                val type = object : TypeToken<PlaylistBackup>() {}.type
                val backup: PlaylistBackup = Gson().fromJson(json, type)
                viewModel.restorePlaylists(backup.playlists, backup.songs)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, R.string.import_success, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, R.string.import_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
