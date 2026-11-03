package com.directshare.app.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {

    /**
     * DirectShare uses Android Storage Access Framework (SAF) and Photo Picker for selecting
     * and saving files, so broad READ_EXTERNAL_STORAGE / MANAGE_EXTERNAL_STORAGE permissions
     * are NOT requested. Only POST_NOTIFICATIONS is needed on Android 13+ (API 33+) to show
     * the Foreground Service notification during active transfers.
     */
    fun requiredRuntimePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
