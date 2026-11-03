package com.directshare.app.webrtc.transfer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.directshare.app.MainActivity
import com.directshare.app.R
import com.directshare.app.permissions.PermissionHelper
import com.directshare.app.utils.AppLogger

/**
 * Keeps the process prioritized during active peer-to-peer WebRTC transfers and displays
 * real-time progress in the system notification shade.
 */
class TransferForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "DirectShare Transfer Active"
        val subtitle = intent?.getStringExtra(EXTRA_SUBTITLE) ?: "Keep DirectShare open until completion"
        val progress = intent?.getIntExtra(EXTRA_PROGRESS, 0) ?: 0

        val notification = buildNotification(title, subtitle, progress)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            AppLogger.w("TransferForegroundService", "Could not start foreground service", e)
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(title: String, subtitle: String, progress: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_directshare_logo)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setProgress(100, progress.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "directshare_transfer_channel"
        private const val NOTIFICATION_ID = 4102
        private const val ACTION_START_OR_UPDATE = "com.directshare.app.action.START_OR_UPDATE"
        private const val ACTION_STOP = "com.directshare.app.action.STOP"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_SUBTITLE = "extra_subtitle"
        private const val EXTRA_PROGRESS = "extra_progress"

        fun startOrUpdate(context: Context, title: String, subtitle: String, progressPercent: Int) {
            if (!PermissionHelper.hasNotificationPermission(context)) return
            val intent = Intent(context, TransferForegroundService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SUBTITLE, subtitle)
                putExtra(EXTRA_PROGRESS, progressPercent)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // If app is in background restrictions, update via NotificationManager if already running
                val nm = context.getSystemService(NotificationManager::class.java)
                AppLogger.w("TransferForegroundService", "Foreground service start skipped: ${e.message} (nm=${nm != null})")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, TransferForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
                context.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
