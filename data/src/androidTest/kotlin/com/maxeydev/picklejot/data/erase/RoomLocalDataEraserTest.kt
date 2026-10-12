package com.maxeydev.picklejot.data.erase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.data.person.PersonEntity
import com.maxeydev.picklejot.data.photo.PHOTO_DIRECTORY
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.data.profile.DataStoreEntitlementRepository
import com.maxeydev.picklejot.data.profile.DataStoreProfileRepository
import com.maxeydev.picklejot.data.profile.UserProfileSerializer
import com.maxeydev.picklejot.data.reminder.DataStoreReminderStore
import com.maxeydev.picklejot.data.settings.DataStoreAppSettingsStore
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.profile.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Instant

private const val TOKEN = "purchase-token"

@RunWith(AndroidJUnit4::class)
class RoomLocalDataEraserTest {
    private lateinit var directory: File
    private lateinit var database: PicklejotDatabase
    private lateinit var exportDirectory: File
    private lateinit var backupDirectory: File
    private lateinit var profileStore: DataStore<UserProfile>
    private lateinit var preferences: DataStore<Preferences>
    private lateinit var appSettings: DataStoreAppSettingsStore
    private lateinit var profileRepository: DataStoreProfileRepository
    private lateinit var eraser: RoomLocalDataEraser
    private val job = SupervisorJob()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "eraser-test-${System.nanoTime()}").apply { mkdirs() }
        database = Room.inMemoryDatabaseBuilder(context, PicklejotDatabase::class.java).build()
        exportDirectory = File(directory, "exports").apply { mkdirs() }
        backupDirectory = File(directory, "db-backups").apply { mkdirs() }
        val scope = CoroutineScope(Dispatchers.IO + job)
        profileStore =
            DataStoreFactory.create(
                serializer = UserProfileSerializer(),
                scope = scope,
                produceFile = { File(directory, "user-profile.json") },
            )
        preferences =
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { File(directory, "match-preferences.preferences_pb") },
            )
        appSettings =
            DataStoreAppSettingsStore(
                PreferenceDataStoreFactory.create(
                    scope = scope,
                    produceFile = { File(directory, "app-settings.preferences_pb") },
                ),
            )
        profileRepository = DataStoreProfileRepository(profileStore)
        eraser =
            RoomLocalDataEraser(
                database = database,
                photoStore = PhotoStore(directory),
                exportDirectory = exportDirectory,
                databaseBackupDirectory = backupDirectory,
                preferences = preferences,
                profileRepository = profileRepository,
                appSettings = appSettings,
                ioDispatcher = Dispatchers.IO,
            )
    }

    @After
    fun tearDown() =
        runBlocking {
            database.close()
            job.cancelAndJoin()
            directory.deleteRecursively()
            Unit
        }

    private fun personCount(): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM person").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun writePhoto(name: String) {
        val file = File(directory, "$PHOTO_DIRECTORY/$name")
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(1, 2, 3))
    }

    private suspend fun seed() {
        database.personDao().insertIgnoringDuplicate(
            PersonEntity(
                id = "person-1",
                displayName = "Sam",
                normalizedName = "sam",
                createdAt = Instant.fromEpochMilliseconds(0),
            ),
        )
        writePhoto("a.jpg")
        writePhoto("b.jpg")
        File(exportDirectory, "picklejot-export.json").writeText("{}")
        File(backupDirectory, "schema-1").apply { mkdirs() }.resolve("picklejot.db").writeBytes(byteArrayOf(9))
        DataStoreReminderStore(preferences).setEnabled(true)
        profileRepository.updateDisplayName("Matthew")
        appSettings.setOnboardingComplete(true)
        appSettings.setDarkTheme(true)
    }

    @Test
    fun erasing_removes_matches_photos_exports_backups_name_and_reminder_settings() =
        runBlocking {
            seed()
            assertEquals(1, personCount())

            eraser.erase()

            assertEquals(0, personCount())
            assertEquals(0, File(directory, PHOTO_DIRECTORY).listFiles().orEmpty().size)
            assertFalse(exportDirectory.exists())
            assertFalse(backupDirectory.exists())
            assertFalse(DataStoreReminderStore(preferences).observe().first().enabled)
            assertEquals("", profileRepository.observeProfile().first().displayName)
        }

    @Test
    fun erasing_sends_the_user_back_through_onboarding_but_keeps_their_theme() =
        runBlocking {
            seed()

            eraser.erase()

            val settings = appSettings.observe().first()
            assertFalse(settings.onboardingComplete)
            assertEquals(true, settings.darkTheme)
        }

    @Test
    fun erasing_never_revokes_a_pro_purchase() =
        runBlocking {
            seed()
            val entitlements = DataStoreEntitlementRepository(profileStore)
            entitlements.record(EntitlementSignal.PurchaseVerified(TOKEN, Instant.fromEpochMilliseconds(1_000)))

            eraser.erase()

            val entitlement = entitlements.observeEntitlement().first()
            assertTrue(entitlement.isPro)
            assertEquals(TOKEN, entitlement.purchaseToken)
        }

    @Test
    fun erasing_an_already_empty_install_is_harmless() =
        runBlocking {
            eraser.erase()

            assertEquals(0, personCount())
            assertFalse(appSettings.observe().first().onboardingComplete)
        }
}
