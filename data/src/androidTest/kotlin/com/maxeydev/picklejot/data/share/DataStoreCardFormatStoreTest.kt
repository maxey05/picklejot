@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.share

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.data.match.DataStoreMatchSortStore
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.share.CardFormat
import com.maxeydev.picklejot.domain.share.CardLayout
import com.maxeydev.picklejot.domain.share.CardRatio
import com.maxeydev.picklejot.domain.share.CardTheme
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
class DataStoreCardFormatStoreTest {
    private lateinit var directory: File
    private lateinit var preferencesFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "card-format-test-${Uuid.random()}").apply { mkdirs() }
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
    fun a_user_who_never_chose_gets_the_tall_dark_card_with_automatic_layout() =
        runBlocking {
            val job = SupervisorJob()

            assertEquals(CardFormat.DEFAULT, DataStoreCardFormatStore(openDataStore(job)).observeFormat().first())
            job.cancelAndJoin()
        }

    @Test
    fun the_chosen_ratio_and_variant_survive_an_app_restart() =
        runBlocking {
            val chosen = CardFormat(CardRatio.SQUARE, CardTheme.LIGHT, CardLayout.NO_PHOTO)
            val firstLaunch = SupervisorJob()
            DataStoreCardFormatStore(openDataStore(firstLaunch)).saveFormat(chosen)
            firstLaunch.cancelAndJoin()

            val secondLaunch = SupervisorJob()

            assertEquals(chosen, DataStoreCardFormatStore(openDataStore(secondLaunch)).observeFormat().first())
            secondLaunch.cancelAndJoin()
        }

    @Test
    fun clearing_the_override_returns_to_automatic_layout() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreCardFormatStore(openDataStore(job))
            store.saveFormat(CardFormat(layoutOverride = CardLayout.NO_PHOTO))

            store.saveFormat(CardFormat(layoutOverride = null))

            assertEquals(null, store.observeFormat().first().layoutOverride)
            job.cancelAndJoin()
        }

    @Test
    fun an_unrecognised_stored_value_falls_back_instead_of_crashing() =
        runBlocking {
            val job = SupervisorJob()
            val dataStore = openDataStore(job)
            dataStore.edit { it[stringPreferencesKey("card_ratio")] = "PANORAMA_FROM_A_FUTURE_VERSION" }

            assertEquals(CardRatio.TALL, DataStoreCardFormatStore(dataStore).observeFormat().first().ratio)
            job.cancelAndJoin()
        }

    @Test
    fun the_card_format_shares_the_preferences_file_without_disturbing_the_sort() =
        runBlocking {
            val job = SupervisorJob()
            val dataStore = openDataStore(job)
            val sorts = DataStoreMatchSortStore(dataStore)
            sorts.saveSort(MatchSort.LOCATION_A_TO_Z)

            DataStoreCardFormatStore(dataStore).saveFormat(CardFormat(ratio = CardRatio.SQUARE))

            assertEquals(MatchSort.LOCATION_A_TO_Z, sorts.observeSort().first())
            job.cancelAndJoin()
        }
}
