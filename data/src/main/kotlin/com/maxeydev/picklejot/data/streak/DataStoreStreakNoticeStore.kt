package com.maxeydev.picklejot.data.streak

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.maxeydev.picklejot.domain.streak.StreakNoticeState
import com.maxeydev.picklejot.domain.streak.StreakNoticeStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val ACKNOWLEDGED_SKIP_WEEK = longPreferencesKey("streak_notice_acknowledged_skip_week")
private val ACKNOWLEDGED_MISSED_WEEK = longPreferencesKey("streak_notice_acknowledged_missed_week")

class DataStoreStreakNoticeStore(
    private val dataStore: DataStore<Preferences>,
) : StreakNoticeStore {
    override fun observe(): Flow<StreakNoticeState> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences ->
                StreakNoticeState(
                    acknowledgedSkipWeek = preferences[ACKNOWLEDGED_SKIP_WEEK],
                    acknowledgedMissedWeek = preferences[ACKNOWLEDGED_MISSED_WEEK],
                )
            }.distinctUntilChanged()

    override suspend fun acknowledgeSkip(weekOrdinal: Long) {
        acknowledge(ACKNOWLEDGED_SKIP_WEEK, weekOrdinal)
    }

    override suspend fun acknowledgeMissed(weekOrdinal: Long) {
        acknowledge(ACKNOWLEDGED_MISSED_WEEK, weekOrdinal)
    }

    private suspend fun acknowledge(
        key: Preferences.Key<Long>,
        weekOrdinal: Long,
    ) {
        dataStore.edit { preferences ->
            val existing = preferences[key]
            if (existing == null || weekOrdinal > existing) {
                preferences[key] = weekOrdinal
            }
        }
    }
}
