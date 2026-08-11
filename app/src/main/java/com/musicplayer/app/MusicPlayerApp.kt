package com.musicplayer.app

import android.app.Application
import com.musicplayer.app.viewmodel.MusicViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewModelProvider

/**
 * Application class that owns a single MusicViewModel for the lifetime of the process.
 * Both MainActivity and NowPlayingActivity retrieve this same instance, so the service
 * connection is never torn down when switching between them.
 */
class MusicPlayerApp : Application(), ViewModelStoreOwner {

    private val appViewModelStore: ViewModelStore by lazy { ViewModelStore() }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    val musicViewModel: MusicViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this)
        )[MusicViewModel::class.java]
    }

    override fun onCreate() {
        super.onCreate()
        // Eagerly bind the service so it is ready before any Activity opens
        musicViewModel.bindService()
    }
}
