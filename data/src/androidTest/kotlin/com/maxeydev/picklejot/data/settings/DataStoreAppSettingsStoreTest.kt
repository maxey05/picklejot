package com.maxeydev.picklejot.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.domain.settings.AppSettings
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

@RunWith(AndroidJUnit4::class)
class DataStoreAppSettingsStoreTest {
    private lateinit var directory: File
    private lateinit var settingsFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "app-settings-test-${System.nanoTime()}").apply { mkdirs() }
        settingsFile = File(directory, "app-settings.preferences_pb")
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun openDataStore(job: Job): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { settingsFile },
        )

    @Test
    fun a_fresh_store_follows_the_system_theme_and_has_not_finished_onboarding() =
        runBlocking {
            val job = SupervisorJob()

            val settings = DataStoreAppSettingsStore(openDataStore(job)).observe().first()

            assertEquals(AppSettings(darkTheme = null, onboardingComplete = false), settings)
            job.cancelAndJoin()
        }

    @Test
    fun the_theme_choice_and_onboarding_survive_an_app_restart() =
        runBlocking {
            val firstLaunch = SupervisorJob()
            val store = DataStoreAppSettingsStore(openDataStore(firstLaunch))
            store.setDarkTheme(true)
            store.setOnboardingComplete(true)
            firstLaunch.cancelAndJoin()

            val secondLaunch = SupervisorJob()
            val reopened = DataStoreAppSettingsStore(openDataStore(secondLaunch)).observe().first()

            assertEquals(AppSettings(darkTheme = true, onboardingComplete = true), reopened)
            secondLaunch.cancelAndJoin()
        }

    @Test
    fun the_theme_can_be_switched_back_to_light() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreAppSettingsStore(openDataStore(job))
            store.setDarkTheme(true)

            store.setDarkTheme(false)

            assertEquals(false, store.observe().first().darkTheme)
            job.cancelAndJoin()
        }

    @Test
    fun onboarding_can_be_marked_incomplete_again_without_touching_the_theme() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreAppSettingsStore(openDataStore(job))
            store.setDarkTheme(true)
            store.setOnboardingComplete(true)

            store.setOnboardingComplete(false)

            assertEquals(AppSettings(darkTheme = true, onboardingComplete = false), store.observe().first())
            job.cancelAndJoin()
        }

    @Test
    fun sound_effects_default_to_on_and_the_choice_survives_an_app_restart() =
        runBlocking {
            val firstLaunch = SupervisorJob()
            val store = DataStoreAppSettingsStore(openDataStore(firstLaunch))
            assertEquals(true, store.observe().first().soundEffectsEnabled)
            store.setSoundEffectsEnabled(false)
            firstLaunch.cancelAndJoin()

            val secondLaunch = SupervisorJob()
            val reopened = DataStoreAppSettingsStore(openDataStore(secondLaunch)).observe().first()

            assertEquals(false, reopened.soundEffectsEnabled)
            secondLaunch.cancelAndJoin()
        }
}
