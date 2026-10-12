package com.maxeydev.picklejot.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.maxeydev.picklejot.domain.settings.AppSettings
import com.maxeydev.picklejot.domain.settings.AppSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

const val APP_SETTINGS_NAME = "app-settings"

private val DARK_THEME = booleanPreferencesKey("dark_theme")
private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
private val SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")

fun createAppSettingsDataStore(
    context: Context,
    scope: CoroutineScope,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.applicationContext.preferencesDataStoreFile(APP_SETTINGS_NAME) },
    )

class DataStoreAppSettingsStore(
    private val dataStore: DataStore<Preferences>,
) : AppSettingsStore {
    override fun observe(): Flow<AppSettings> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences ->
                AppSettings(
                    darkTheme = preferences[DARK_THEME],
                    onboardingComplete = preferences[ONBOARDING_COMPLETE] ?: false,
                    soundEffectsEnabled = preferences[SOUND_EFFECTS_ENABLED] ?: true,
                )
            }.distinctUntilChanged()

    override suspend fun setDarkTheme(dark: Boolean) {
        dataStore.edit { preferences -> preferences[DARK_THEME] = dark }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { preferences -> preferences[ONBOARDING_COMPLETE] = complete }
    }

    override suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[SOUND_EFFECTS_ENABLED] = enabled }
    }
}
