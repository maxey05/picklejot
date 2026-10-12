@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.photo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.data.match.RoomMatchRepository
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.photo.PhotoImportState
import com.maxeydev.picklejot.domain.photo.PhotoSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val WAIT_MILLIS = 10_000L

@RunWith(AndroidJUnit4::class)
class QueuedPhotoImporterTest {
    private lateinit var root: File
    private lateinit var database: PicklejotDatabase
    private lateinit var matches: RoomMatchRepository
    private lateinit var scope: CoroutineScope
    private lateinit var importer: QueuedPhotoImporter
    private val deletedCaptures = mutableListOf<String>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.cacheDir, "importer-test-${Uuid.random()}").apply { mkdirs() }
        database = Room.inMemoryDatabaseBuilder(context, PicklejotDatabase::class.java).build()
        val store = PhotoStore(root)
        matches = RoomMatchRepository(database, store, Dispatchers.IO)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        importer =
            QueuedPhotoImporter(
                scope = scope,
                ioDispatcher = Dispatchers.IO,
                compressor = ImageCompressor(openSource = { FileInputStream(it) }),
                photoStore = store,
                matchRepository = matches,
                deleteTemporaryCapture = { path ->
                    deletedCaptures += path
                    File(path).delete()
                },
            )
    }

    @After
    fun tearDown() {
        scope.cancel()
        database.close()
        root.deleteRecursively()
    }

    private fun source(name: String): String = writeTestJpeg(File(root, "gallery/$name.jpg"), 3000, 2000).path

    private suspend fun finished(key: String): PhotoImportState =
        withTimeout(WAIT_MILLIS) { importer.observe(key).first { it !is PhotoImportState.Importing } }

    private fun newMatch(): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.SINGLES,
            date = AppDate.parse("2026-09-20"),
            result = MatchResult.WIN,
            createdAt = AppInstant.fromEpochMilliseconds(1),
            updatedAt = AppInstant.fromEpochMilliseconds(1),
        )

    @Test
    fun an_import_produces_a_compressed_copy_inside_the_photo_directory() =
        runBlocking {
            importer.ensureStarted("k", PhotoSource(source("a"), isTemporaryCapture = false))

            val ready = finished("k") as PhotoImportState.Ready
            assertTrue(ready.photo.relativePath.startsWith("photos/"))
            assertTrue(File(root, ready.photo.relativePath).exists())
            assertEquals(2048, ready.photo.width)
        }

    @Test
    fun starting_the_same_key_twice_imports_once() =
        runBlocking {
            val path = source("a")
            importer.ensureStarted("k", PhotoSource(path, isTemporaryCapture = false))
            importer.ensureStarted("k", PhotoSource(path, isTemporaryCapture = false))

            finished("k")
            assertEquals(1, File(root, "photos").listFiles().orEmpty().count { it.extension == "jpg" })
        }

    @Test
    fun a_camera_capture_is_deleted_once_it_has_been_copied() =
        runBlocking {
            val capture = source("capture")
            importer.ensureStarted("k", PhotoSource(capture, isTemporaryCapture = true))

            assertTrue(finished("k") is PhotoImportState.Ready)
            assertEquals(listOf(capture), deletedCaptures)
            assertFalse(File(capture).exists())
        }

    @Test
    fun an_unreadable_source_fails() =
        runBlocking {
            importer.ensureStarted("k", PhotoSource(File(root, "missing.jpg").path, isTemporaryCapture = false))

            assertEquals(PhotoImportState.Failed, finished("k"))
        }

    @Test
    fun pending_photos_are_attached_after_the_match_is_saved() =
        runBlocking {
            val match = newMatch()
            importer.ensureStarted("a", PhotoSource(source("a"), isTemporaryCapture = false))
            importer.ensureStarted("b", PhotoSource(source("b"), isTemporaryCapture = false))
            matches.saveMatch(match)

            importer.attachWhenReady(match.id, listOf("a", "b"))

            val attached =
                withTimeout(WAIT_MILLIS) {
                    matches.observeById(match.id).first { it != null && it.photos.size == 2 }!!
                }
            assertEquals(listOf(0, 1), attached.photos.map { it.sortIndex })
        }

    @Test
    fun a_photo_finishing_after_its_match_was_deleted_is_thrown_away() =
        runBlocking {
            val match = newMatch()
            matches.saveMatch(match)
            importer.ensureStarted("a", PhotoSource(source("a"), isTemporaryCapture = false))
            val ready = finished("a") as PhotoImportState.Ready
            matches.deleteMatch(match.id)

            importer.attachWhenReady(match.id, listOf("a"))

            withTimeout(WAIT_MILLIS) {
                while (File(root, ready.photo.relativePath).exists()) {
                    delay(20)
                }
            }
        }

    @Test
    fun discarding_an_import_removes_its_file() =
        runBlocking {
            importer.ensureStarted("a", PhotoSource(source("a"), isTemporaryCapture = false))
            val ready = finished("a") as PhotoImportState.Ready

            importer.discard("a")

            withTimeout(WAIT_MILLIS) {
                while (File(root, ready.photo.relativePath).exists()) {
                    delay(20)
                }
            }
            assertEquals(PhotoImportState.Failed, importer.observe("a").first())
        }
}
