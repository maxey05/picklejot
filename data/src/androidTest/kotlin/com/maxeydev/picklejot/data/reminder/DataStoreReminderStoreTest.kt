@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.reminder

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.reminder.ReminderSchedule
import com.maxeydev.picklejot.domain.reminder.ReminderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class DataStoreReminderStoreTest {
    private lateinit var directory: File
    private lateinit var preferencesFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "reminder-test-${Uuid.random()}").apply { mkdirs() }
        preferencesFile = File(directory, "match-preferences.preferences_pb")
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun openDataStore(job: Job): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { preferencesFile },
        )

    @Test
    fun a_fresh_store_has_the_reminder_off_and_nothing_notified() =
        runBlocking {
            val job = SupervisorJob()

            val state = DataStoreReminderStore(openDataStore(job)).observe().first()

            assertEquals(ReminderState(enabled = false, lastNotifiedWeek = null), state)
            job.cancelAndJoin()
        }

    @Test
    fun the_choice_and_the_last_notified_week_survive_an_app_restart() =
        runBlocking {
            val firstLaunch = SupervisorJob()
            val store = DataStoreReminderStore(openDataStore(firstLaunch))
            store.setEnabled(true)
            store.markNotified(2_900)
            firstLaunch.cancelAndJoin()

            val secondLaunch = SupervisorJob()
            val reopened = DataStoreReminderStore(openDataStore(secondLaunch)).observe().first()

            assertEquals(ReminderState(enabled = true, lastNotifiedWeek = 2_900), reopened)
            secondLaunch.cancelAndJoin()
        }

    @Test
    fun clearing_the_notified_week_removes_it_and_keeps_the_choice() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreReminderStore(openDataStore(job))
            store.setEnabled(true)
            store.markNotified(2_900)

            store.markNotified(null)

            assertEquals(ReminderState(enabled = true, lastNotifiedWeek = null), store.observe().first())
            job.cancelAndJoin()
        }

    @Test
    fun turning_the_reminder_off_is_stored() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreReminderStore(openDataStore(job))
            store.setEnabled(true)

            store.setEnabled(false)

            assertEquals(false, store.observe().first().enabled)
            job.cancelAndJoin()
        }

    @Test
    fun a_fresh_store_uses_the_default_friday_evening_time() =
        runBlocking {
            val job = SupervisorJob()

            val state = DataStoreReminderStore(openDataStore(job)).observe().first()

            assertEquals(ReminderSchedule.DEFAULT_FIRE_TIME, state.fireTime)
            job.cancelAndJoin()
        }

    @Test
    fun a_chosen_time_survives_an_app_restart() =
        runBlocking {
            val firstLaunch = SupervisorJob()
            DataStoreReminderStore(openDataStore(firstLaunch)).setFireTime(AppTime(7, 5))
            firstLaunch.cancelAndJoin()

            val secondLaunch = SupervisorJob()
            val reopened = DataStoreReminderStore(openDataStore(secondLaunch)).observe().first()

            assertEquals(AppTime(7, 5), reopened.fireTime)
            secondLaunch.cancelAndJoin()
        }

    @Test
    fun the_time_can_be_midnight_and_the_last_minute_of_the_day() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreReminderStore(openDataStore(job))

            store.setFireTime(AppTime(0, 0))
            assertEquals(AppTime(0, 0), store.observe().first().fireTime)

            store.setFireTime(AppTime(23, 59))
            assertEquals(AppTime(23, 59), store.observe().first().fireTime)
            job.cancelAndJoin()
        }

    @Test
    fun an_unreadable_stored_time_falls_back_to_the_default() =
        runBlocking {
            val job = SupervisorJob()
            val dataStore = openDataStore(job)
            dataStore.edit { it[intPreferencesKey("streak_reminder_fire_minute_of_day")] = 5_000 }

            val state = DataStoreReminderStore(dataStore).observe().first()

            assertEquals(ReminderSchedule.DEFAULT_FIRE_TIME, state.fireTime)
            job.cancelAndJoin()
        }

    @Test
    fun choosing_a_time_keeps_the_enabled_choice_and_notified_week() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreReminderStore(openDataStore(job))
            store.setEnabled(true)
            store.markNotified(2_900)

            store.setFireTime(AppTime(9, 0))

            assertEquals(
                ReminderState(enabled = true, lastNotifiedWeek = 2_900, fireTime = AppTime(9, 0)),
                store.observe().first(),
            )
            job.cancelAndJoin()
        }
}
