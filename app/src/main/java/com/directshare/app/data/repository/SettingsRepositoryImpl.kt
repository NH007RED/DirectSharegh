package com.directshare.app.data.repository

import com.directshare.app.data.local.SettingsDataStore
import com.directshare.app.domain.model.AppSettings
import com.directshare.app.domain.model.ThemePreference
import com.directshare.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings> = dataStore.settingsFlow

    override suspend fun updateDeviceName(name: String) = dataStore.updateDeviceName(name)

    override suspend fun updateDefaultSaveDestination(uriString: String?) =
        dataStore.updateDefaultSaveDestination(uriString)

    override suspend fun updateTheme(theme: ThemePreference) = dataStore.updateTheme(theme)

    override suspend fun updateSoundEnabled(enabled: Boolean) = dataStore.updateSoundEnabled(enabled)

    override suspend fun updateVibrationEnabled(enabled: Boolean) =
        dataStore.updateVibrationEnabled(enabled)

    override suspend fun updateAutoAccept(enabled: Boolean) = dataStore.updateAutoAccept(enabled)

    override suspend fun updateMaxConcurrentTransfers(count: Int) =
        dataStore.updateMaxConcurrentTransfers(count)

    override suspend fun updateChunkSizeBytes(chunkSizeBytes: Int) =
        dataStore.updateChunkSizeBytes(chunkSizeBytes)

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        dataStore.setOnboardingCompleted(completed)

    override suspend fun updateTurnConfiguration(
        url: String,
        username: String,
        credential: String
    ) = dataStore.updateTurnConfiguration(url, username, credential)
}
