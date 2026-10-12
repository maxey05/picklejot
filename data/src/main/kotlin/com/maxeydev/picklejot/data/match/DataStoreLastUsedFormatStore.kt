package com.maxeydev.picklejot.data.match

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.maxeydev.picklejot.domain.match.LastUsedFormatStore
import com.maxeydev.picklejot.domain.match.MatchFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

const val MATCH_PREFERENCES_NAME = "match-preferences"

private val LAST_USED_FORMAT = stringPreferencesKey("last_used_format")

fun createMatchPreferencesDataStore(
    context: Context,
    scope: CoroutineScope,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.applicationContext.preferencesDataStoreFile(MATCH_PREFERENCES_NAME) },
    )

private fun formatFrom(stored: String?): MatchFormat =
    MatchFormat.entries.firstOrNull { it.name == stored } ?: LastUsedFormatStore.FIRST_LAUNCH_FORMAT

class DataStoreLastUsedFormatStore(
    private val dataStore: DataStore<Preferences>,
    cacheScope: CoroutineScope,
) : LastUsedFormatStore {
    private val latest: StateFlow<MatchFormat?> =
        dataStore.data
            .map<Preferences, MatchFormat?> { preferences -> formatFrom(preferences[LAST_USED_FORMAT]) }
            .catch { error -> if (error is IOException) emit(null) else throw error }
            .stateIn(cacheScope, SharingStarted.Eagerly, null)

    override fun cachedLastUsedFormat(): MatchFormat? = latest.value

    override suspend fun lastUsedFormat(): MatchFormat = formatFrom(dataStore.data.first()[LAST_USED_FORMAT])

    override suspend fun recordLastUsedFormat(format: MatchFormat) {
        dataStore.edit { preferences -> preferences[LAST_USED_FORMAT] = format.name }
    }
}
