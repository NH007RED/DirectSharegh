package com.directshare.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.directshare.app.domain.model.AppSettings
import com.directshare.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "directshare_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val DEVICE_NAME = stringPreferencesKey("device_display_name")
        val DEFAULT_SAVE_URI = stringPreferencesKey("default_save_destination_uri")
        val THEME = stringPreferencesKey("theme_preference")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val AUTO_ACCEPT = booleanPreferencesKey("auto_accept_enabled")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent_transfers")
        val CHUNK_SIZE_BYTES = intPreferencesKey("chunk_size_bytes")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CUSTOM_TURN_URL = stringPreferencesKey("custom_turn_url")
        val CUSTOM_TURN_USERNAME = stringPreferencesKey("custom_turn_username")
        val CUSTOM_TURN_CREDENTIAL = stringPreferencesKey("custom_turn_credential")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME] ?: ThemePreference.DARK.name
        val theme = runCatching { ThemePreference.valueOf(themeStr) }.getOrDefault(ThemePreference.DARK)
        val chunkSize = (prefs[Keys.CHUNK_SIZE_BYTES] ?: AppSettings.DEFAULT_CHUNK_SIZE_BYTES)
            .coerceIn(AppSettings.MIN_CHUNK_SIZE_BYTES, AppSettings.MAX_CHUNK_SIZE_BYTES)

        AppSettings(
            deviceDisplayName = prefs[Keys.DEVICE_NAME] ?: AppSettings.defaultDeviceName(),
            defaultSaveDestinationUri = prefs[Keys.DEFAULT_SAVE_URI]?.takeIf { it.isNotBlank() },
            themePreference = theme,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            autoAcceptFromAuthorizedPeer = prefs[Keys.AUTO_ACCEPT] ?: false, // Default false
            maxConcurrentTransfers = (prefs[Keys.MAX_CONCURRENT] ?: 1).coerceIn(1, 3),
            chunkSizeBytes = chunkSize,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            customTurnUrl = prefs[Keys.CUSTOM_TURN_URL] ?: "",
            customTurnUsername = prefs[Keys.CUSTOM_TURN_USERNAME] ?: "",
            customTurnCredential = prefs[Keys.CUSTOM_TURN_CREDENTIAL] ?: ""
        )
    }

    suspend fun updateDeviceName(name: String) {
        context.dataStore.edit { it[Keys.DEVICE_NAME] = name.trim().take(64) }
    }

    suspend fun updateDefaultSaveDestination(uriString: String?) {
        context.dataStore.edit {
            if (uriString.isNullOrBlank()) {
                it.remove(Keys.DEFAULT_SAVE_URI)
            } else {
                it[Keys.DEFAULT_SAVE_URI] = uriString
            }
        }
    }

    suspend fun updateTheme(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun updateVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun updateAutoAccept(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ACCEPT] = enabled }
    }

    suspend fun updateMaxConcurrentTransfers(count: Int) {
        context.dataStore.edit { it[Keys.MAX_CONCURRENT] = count.coerceIn(1, 3) }
    }

    suspend fun updateChunkSizeBytes(chunkSizeBytes: Int) {
        context.dataStore.edit {
            it[Keys.CHUNK_SIZE_BYTES] = chunkSizeBytes.coerceIn(
                AppSettings.MIN_CHUNK_SIZE_BYTES,
                AppSettings.MAX_CHUNK_SIZE_BYTES
            )
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun updateTurnConfiguration(url: String, username: String, credential: String) {
        context.dataStore.edit {
            it[Keys.CUSTOM_TURN_URL] = url.trim()
            it[Keys.CUSTOM_TURN_USERNAME] = username.trim()
            it[Keys.CUSTOM_TURN_CREDENTIAL] = credential
        }
    }
}
