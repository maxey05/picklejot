@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.backup.ExportPromptReason
import com.maxeydev.picklejot.domain.backup.ExportPromptState
import com.maxeydev.picklejot.domain.datetime.AppInstant
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
class DataStoreExportPromptStoreTest {
    private lateinit var directory: File
    private lateinit var preferencesFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "export-prompt-test-${Uuid.random()}").apply { mkdirs() }
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
    fun `a_fresh_store_holds_the_empty_state`() =
        runBlocking {
            val job = SupervisorJob()

            val state = DataStoreExportPromptStore(openDataStore(job)).observe().first()

            assertEquals(ExportPromptState(), state)
            job.cancelAndJoin()
        }

    @Test
    fun `every_field_survives_an_app_restart`() =
        runBlocking {
            val saved =
                ExportPromptState(
                    trackingSince = AppInstant.fromEpochMilliseconds(1_000),
                    lastExportAt = AppInstant.fromEpochMilliseconds(2_000),
                    lastPromptAt = AppInstant.fromEpochMilliseconds(3_000),
                    anchorCount = 42,
                    pending = ExportPromptReason.MATCHES_ADDED,
                )
            val firstLaunch = SupervisorJob()
            DataStoreExportPromptStore(openDataStore(firstLaunch)).update { saved }
            firstLaunch.cancelAndJoin()

            val secondLaunch = SupervisorJob()
            val reopened = DataStoreExportPromptStore(openDataStore(secondLaunch)).observe().first()

            assertEquals(saved, reopened)
            secondLaunch.cancelAndJoin()
        }

    @Test
    fun `clearing_the_pending_prompt_removes_it_from_storage`() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreExportPromptStore(openDataStore(job))
            store.update { it.copy(pending = ExportPromptReason.PERIODIC, anchorCount = 7) }

            store.update { it.copy(pending = null) }

            val state = store.observe().first()
            assertEquals(null, state.pending)
            assertEquals(7, state.anchorCount)
            job.cancelAndJoin()
        }

    @Test
    fun `update_receives_the_stored_state_so_changes_compose`() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreExportPromptStore(openDataStore(job))

            store.update { it.copy(anchorCount = 10) }
            store.update { it.copy(anchorCount = it.anchorCount + 5) }

            assertEquals(15, store.observe().first().anchorCount)
            job.cancelAndJoin()
        }
}
