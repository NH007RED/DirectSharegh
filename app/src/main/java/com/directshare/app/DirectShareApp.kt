package com.directshare.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.directshare.app.utils.AppLogger
import com.directshare.app.webrtc.transfer.TransferForegroundService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DirectShareApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        AppLogger.i("DirectShareApp", "DirectShare Application initialized")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TransferForegroundService.CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
