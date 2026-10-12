package com.maxeydev.picklejot.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.maxeydev.picklejot.domain.backup.ExportPromptReason
import com.maxeydev.picklejot.domain.backup.ExportPromptState
import com.maxeydev.picklejot.domain.backup.ExportPromptStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlin.time.Instant

private val TRACKING_SINCE = longPreferencesKey("export_prompt_tracking_since")
private val LAST_EXPORT_AT = longPreferencesKey("export_prompt_last_export_at")
private val LAST_PROMPT_AT = longPreferencesKey("export_prompt_last_prompt_at")
private val ANCHOR_COUNT = intPreferencesKey("export_prompt_anchor_count")
private val PENDING = stringPreferencesKey("export_prompt_pending")

class DataStoreExportPromptStore(
    private val dataStore: DataStore<Preferences>,
) : ExportPromptStore {
    override fun observe(): Flow<ExportPromptState> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences -> preferences.toState() }
            .distinctUntilChanged()

    override suspend fun update(transform: (ExportPromptState) -> ExportPromptState) {
        dataStore.edit { preferences -> preferences.write(transform(preferences.toState())) }
    }
}

private fun Preferences.toState(): ExportPromptState =
    ExportPromptState(
        trackingSince = this[TRACKING_SINCE]?.let(Instant::fromEpochMilliseconds),
        lastExportAt = this[LAST_EXPORT_AT]?.let(Instant::fromEpochMilliseconds),
        lastPromptAt = this[LAST_PROMPT_AT]?.let(Instant::fromEpochMilliseconds),
        anchorCount = this[ANCHOR_COUNT] ?: 0,
        pending = ExportPromptReason.entries.firstOrNull { it.name == this[PENDING] },
    )

private fun MutablePreferences.write(state: ExportPromptState) {
    setOrRemove(TRACKING_SINCE, state.trackingSince?.toEpochMilliseconds())
    setOrRemove(LAST_EXPORT_AT, state.lastExportAt?.toEpochMilliseconds())
    setOrRemove(LAST_PROMPT_AT, state.lastPromptAt?.toEpochMilliseconds())
    this[ANCHOR_COUNT] = state.anchorCount
    setOrRemove(PENDING, state.pending?.name)
}

private fun <T> MutablePreferences.setOrRemove(
    key: Preferences.Key<T>,
    value: T?,
) {
    if (value == null) {
        remove(key)
    } else {
        this[key] = value
    }
}
