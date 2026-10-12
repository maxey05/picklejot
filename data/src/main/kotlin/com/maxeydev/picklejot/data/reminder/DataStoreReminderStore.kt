package com.maxeydev.picklejot.data.reminder

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.reminder.ReminderSchedule
import com.maxeydev.picklejot.domain.reminder.ReminderState
import com.maxeydev.picklejot.domain.reminder.ReminderStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val REMINDER_ENABLED = booleanPreferencesKey("streak_reminder_enabled")
private val REMINDER_LAST_NOTIFIED_WEEK = longPreferencesKey("streak_reminder_last_notified_week")
private val REMINDER_FIRE_MINUTE_OF_DAY = intPreferencesKey("streak_reminder_fire_minute_of_day")

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

class DataStoreReminderStore(
    private val dataStore: DataStore<Preferences>,
) : ReminderStore {
    override fun observe(): Flow<ReminderState> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences ->
                ReminderState(
                    enabled = preferences[REMINDER_ENABLED] ?: false,
                    lastNotifiedWeek = preferences[REMINDER_LAST_NOTIFIED_WEEK],
                    fireTime = preferences[REMINDER_FIRE_MINUTE_OF_DAY].toFireTime(),
                )
            }.distinctUntilChanged()

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[REMINDER_ENABLED] = enabled }
    }

    override suspend fun setFireTime(time: AppTime) {
        dataStore.edit { preferences ->
            preferences[REMINDER_FIRE_MINUTE_OF_DAY] = time.hour * MINUTES_PER_HOUR + time.minute
        }
    }

    override suspend fun markNotified(weekOrdinal: Long?) {
        dataStore.edit { preferences ->
            if (weekOrdinal == null) {
                preferences.remove(REMINDER_LAST_NOTIFIED_WEEK)
            } else {
                preferences[REMINDER_LAST_NOTIFIED_WEEK] = weekOrdinal
            }
        }
    }
}

private fun Int?.toFireTime(): AppTime =
    if (this == null || this !in 0 until MINUTES_PER_DAY) {
        ReminderSchedule.DEFAULT_FIRE_TIME
    } else {
        AppTime(this / MINUTES_PER_HOUR, this % MINUTES_PER_HOUR)
    }
