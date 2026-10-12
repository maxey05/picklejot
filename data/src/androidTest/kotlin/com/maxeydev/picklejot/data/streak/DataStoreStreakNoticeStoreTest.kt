@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.streak

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.streak.StreakNoticeState
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
class DataStoreStreakNoticeStoreTest {
    private lateinit var directory: File
    private lateinit var preferencesFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "streak-notice-test-${Uuid.random()}").apply { mkdirs() }
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
    fun `a_fresh_store_has_acknowledged_nothing`() =
        runBlocking {
            val job = SupervisorJob()

            val state = DataStoreStreakNoticeStore(openDataStore(job)).observe().first()

            assertEquals(StreakNoticeState(), state)
            job.cancelAndJoin()
        }

    @Test
    fun `acknowledgements_survive_an_app_restart`() =
        runBlocking {
            val firstLaunch = SupervisorJob()
            val store = DataStoreStreakNoticeStore(openDataStore(firstLaunch))
            store.acknowledgeSkip(2_900)
            store.acknowledgeMissed(2_895)
            firstLaunch.cancelAndJoin()

            val secondLaunch = SupervisorJob()
            val reopened = DataStoreStreakNoticeStore(openDataStore(secondLaunch)).observe().first()

            assertEquals(StreakNoticeState(acknowledgedSkipWeek = 2_900, acknowledgedMissedWeek = 2_895), reopened)
            secondLaunch.cancelAndJoin()
        }

    @Test
    fun `an_older_acknowledgement_never_overwrites_a_newer_one`() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreStreakNoticeStore(openDataStore(job))

            store.acknowledgeSkip(2_900)
            store.acknowledgeSkip(2_880)
            store.acknowledgeMissed(2_890)
            store.acknowledgeMissed(2_870)

            assertEquals(
                StreakNoticeState(acknowledgedSkipWeek = 2_900, acknowledgedMissedWeek = 2_890),
                store.observe().first(),
            )
            job.cancelAndJoin()
        }
}
