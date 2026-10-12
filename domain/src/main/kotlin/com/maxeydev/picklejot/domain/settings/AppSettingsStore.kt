package com.maxeydev.picklejot.domain.settings

import kotlinx.coroutines.flow.Flow

interface AppSettingsStore {
    fun observe(): Flow<AppSettings>

    suspend fun setDarkTheme(dark: Boolean)

    suspend fun setOnboardingComplete(complete: Boolean)

    suspend fun setSoundEffectsEnabled(enabled: Boolean)
}
