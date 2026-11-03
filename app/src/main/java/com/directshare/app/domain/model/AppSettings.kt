package com.directshare.app.domain.model

import android.os.Build

data class AppSettings(
    val deviceDisplayName: String = defaultDeviceName(),
    val defaultSaveDestinationUri: String? = null,
    val themePreference: ThemePreference = ThemePreference.DARK,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoAcceptFromAuthorizedPeer: Boolean = false, // Strictly disabled by default
    val maxConcurrentTransfers: Int = 1,
    val chunkSizeBytes: Int = DEFAULT_CHUNK_SIZE_BYTES,
    val onboardingCompleted: Boolean = false,
    val customTurnUrl: String = "",
    val customTurnUsername: String = "",
    val customTurnCredential: String = ""
) {
    companion object {
        const val DEFAULT_CHUNK_SIZE_BYTES: Int = 64 * 1024 // 64 KB WebRTC safe frame chunk size
        const val MIN_CHUNK_SIZE_BYTES: Int = 16 * 1024     // 16 KB
        const val MAX_CHUNK_SIZE_BYTES: Int = 256 * 1024    // 256 KB

        fun defaultDeviceName(): String {
            val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
            val model = Build.MODEL ?: "Phone"
            return if (model.startsWith(manufacturer, ignoreCase = true)) {
                model.take(32)
            } else {
                "$manufacturer $model".take(32)
            }
        }
    }
}

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}
