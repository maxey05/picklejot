@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.match

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.match.MatchFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val CACHE_WAIT_MILLIS = 5_000L
private const val CACHE_POLL_MILLIS = 10L

@RunWith(AndroidJUnit4::class)
class DataStoreLastUsedFormatStoreTest {
    private lateinit var directory: File
    private lateinit var preferencesFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "last-used-format-test-${Uuid.random()}").apply { mkdirs() }
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
    fun `the_first_ever_launch_defaults_to_doubles`() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreLastUsedFormatStore(openDataStore(job), CoroutineScope(job))

            assertEquals(MatchFormat.DOUBLES, store.lastUsedFormat())
            job.cancelAndJoin()
        }

    @Test
    fun `the_last_used_format_survives_an_app_restart`() =
        runBlocking {
            val firstLaunch = SupervisorJob()
            DataStoreLastUsedFormatStore(openDataStore(firstLaunch), CoroutineScope(firstLaunch))
                .recordLastUsedFormat(MatchFormat.SINGLES)
            firstLaunch.cancelAndJoin()

            val secondLaunch = SupervisorJob()
            val reopened = DataStoreLastUsedFormatStore(openDataStore(secondLaunch), CoroutineScope(secondLaunch))

            assertEquals(MatchFormat.SINGLES, reopened.lastUsedFormat())
            secondLaunch.cancelAndJoin()
        }

    @Test
    fun `recording_a_new_format_replaces_the_previous_one`() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreLastUsedFormatStore(openDataStore(job), CoroutineScope(job))

            store.recordLastUsedFormat(MatchFormat.SINGLES)
            store.recordLastUsedFormat(MatchFormat.DOUBLES)

            assertEquals(MatchFormat.DOUBLES, store.lastUsedFormat())
            job.cancelAndJoin()
        }

    @Test
    fun `the_cached_format_follows_the_stored_one_through_a_clear`() =
        runBlocking {
            val job = SupervisorJob()
            val dataStore = openDataStore(job)
            val store = DataStoreLastUsedFormatStore(dataStore, CoroutineScope(job))

            store.recordLastUsedFormat(MatchFormat.SINGLES)
            withTimeout(CACHE_WAIT_MILLIS) {
                while (store.cachedLastUsedFormat() != MatchFormat.SINGLES) {
                    delay(CACHE_POLL_MILLIS)
                }
            }
            dataStore.edit { it.clear() }
            withTimeout(CACHE_WAIT_MILLIS) {
                while (store.cachedLastUsedFormat() != MatchFormat.DOUBLES) {
                    delay(CACHE_POLL_MILLIS)
                }
            }

            assertEquals(MatchFormat.DOUBLES, store.cachedLastUsedFormat())
            job.cancelAndJoin()
        }
}
