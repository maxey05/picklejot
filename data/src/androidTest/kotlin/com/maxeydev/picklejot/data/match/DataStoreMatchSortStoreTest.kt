@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.match

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchSort
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
class DataStoreMatchSortStoreTest {
    private lateinit var directory: File
    private lateinit var preferencesFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "match-sort-test-${Uuid.random()}").apply { mkdirs() }
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
    fun `a_user_who_never_chose_a_sort_gets_newest_first`() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreMatchSortStore(openDataStore(job))

            assertEquals(MatchSort.DATE_NEWEST, store.observeSort().first())
            job.cancelAndJoin()
        }

    @Test
    fun `the_chosen_sort_survives_an_app_restart`() =
        runBlocking {
            val firstLaunch = SupervisorJob()
            DataStoreMatchSortStore(openDataStore(firstLaunch)).saveSort(MatchSort.OPPONENT_A_TO_Z)
            firstLaunch.cancelAndJoin()

            val secondLaunch = SupervisorJob()
            val reopened = DataStoreMatchSortStore(openDataStore(secondLaunch))

            assertEquals(MatchSort.OPPONENT_A_TO_Z, reopened.observeSort().first())
            secondLaunch.cancelAndJoin()
        }

    @Test
    fun `an_unrecognised_stored_sort_falls_back_to_newest_first_instead_of_crashing`() =
        runBlocking {
            val job = SupervisorJob()
            val dataStore = openDataStore(job)
            dataStore.edit { it[stringPreferencesKey("match_list_sort")] = "SORT_FROM_A_FUTURE_VERSION" }

            assertEquals(MatchSort.DATE_NEWEST, DataStoreMatchSortStore(dataStore).observeSort().first())
            job.cancelAndJoin()
        }

    @Test
    fun `the_sort_shares_the_match_preferences_file_without_disturbing_the_last_used_format`() =
        runBlocking {
            val job = SupervisorJob()
            val dataStore = openDataStore(job)
            val formats = DataStoreLastUsedFormatStore(dataStore, CoroutineScope(job))
            val sorts = DataStoreMatchSortStore(dataStore)
            formats.recordLastUsedFormat(MatchFormat.SINGLES)

            sorts.saveSort(MatchSort.RESULT_LOSSES_FIRST)

            assertEquals(MatchFormat.SINGLES, formats.lastUsedFormat())
            assertEquals(MatchSort.RESULT_LOSSES_FIRST, sorts.observeSort().first())
            job.cancelAndJoin()
        }
}
