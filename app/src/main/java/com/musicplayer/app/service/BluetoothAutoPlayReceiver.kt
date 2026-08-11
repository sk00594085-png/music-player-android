package com.musicplayer.app.service

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.musicplayer.app.utils.AppPreferences

class BluetoothAutoPlayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return
        if (!AppPreferences.isAutoPlayBluetooth(context)) return

        // Only auto-play if it's a headset/A2DP device
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        if (device != null) {
            val svc = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_PLAY
            }
            context.startService(svc)
        }
    }
}
