package com.directshare.app.domain.repository

import com.directshare.app.domain.model.AppSettings
import com.directshare.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun updateDeviceName(name: String)
    suspend fun updateDefaultSaveDestination(uriString: String?)
    suspend fun updateTheme(theme: ThemePreference)
    suspend fun updateSoundEnabled(enabled: Boolean)
    suspend fun updateVibrationEnabled(enabled: Boolean)
    suspend fun updateAutoAccept(enabled: Boolean)
    suspend fun updateMaxConcurrentTransfers(count: Int)
    suspend fun updateChunkSizeBytes(chunkSizeBytes: Int)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun updateTurnConfiguration(url: String, username: String, credential: String)
}
