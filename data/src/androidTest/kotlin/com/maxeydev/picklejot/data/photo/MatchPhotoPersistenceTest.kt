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
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.photo.ImportedPhoto
import com.maxeydev.picklejot.domain.photo.PhotoRef
import kotlinx.coroutines.Dispatchers
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class MatchPhotoPersistenceTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var photoRoot: File
    private lateinit var database: PicklejotDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "photo-persistence-${Uuid.random()}.db"
        photoRoot = File(context.cacheDir, "photo-persistence-${Uuid.random()}").apply { mkdirs() }
        database = open()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
        photoRoot.deleteRecursively()
    }

    private fun open(): PicklejotDatabase =
        Room.databaseBuilder(context, PicklejotDatabase::class.java, databaseName).build()

    private fun repository(): RoomMatchRepository = RoomMatchRepository(database, PhotoStore(photoRoot), Dispatchers.IO)

    private fun photoFile(name: String): PhotoRef {
        val relativePath = "photos/$name.jpg"
        File(photoRoot, relativePath).apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        return PhotoRef(Uuid.random(), relativePath, 100, 80, 3, 0)
    }

    private fun match(photos: List<PhotoRef>): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.SINGLES,
            date = AppDate.parse("2026-09-20"),
            result = MatchResult.WIN,
            createdAt = AppInstant.fromEpochMilliseconds(1),
            updatedAt = AppInstant.fromEpochMilliseconds(1),
            photos = photos,
        )

    @Test
    fun reordered_photos_keep_their_order_after_the_database_is_reopened() =
        runBlocking {
            val a = photoFile("a")
            val b = photoFile("b").copy(sortIndex = 1)
            val c = photoFile("c").copy(sortIndex = 2)
            val stored = match(listOf(a, b, c))
            repository().saveMatch(stored)

            val reordered = listOf(c.copy(sortIndex = 0), a.copy(sortIndex = 1), b.copy(sortIndex = 2))
            repository().saveMatch(stored.copy(photos = reordered))
            database.close()
            database = open()

            val reloaded = repository().observeById(stored.id).first()!!
            assertEquals(listOf(c.id, a.id, b.id), reloaded.photos.map { it.id })
            val listed = repository().observeListPage(MatchSort.DATE_NEWEST, 10).first().single()
            assertEquals("photos/c.jpg", listed.primaryPhotoPath)
        }

    @Test
    fun removing_a_photo_deletes_its_row_and_its_file() =
        runBlocking {
            val keep = photoFile("keep")
            val drop = photoFile("drop").copy(sortIndex = 1)
            val stored = match(listOf(keep, drop))
            repository().saveMatch(stored)

            repository().saveMatch(stored.copy(photos = listOf(keep)), removedPhotoIds = setOf(drop.id))

            assertEquals(listOf(keep.id), repository().observeById(stored.id).first()!!.photos.map { it.id })
            assertFalse(File(photoRoot, drop.relativePath).exists())
            assertTrue(File(photoRoot, keep.relativePath).exists())
        }

    @Test
    fun an_appended_photo_goes_to_the_end_and_survives_a_later_save_that_did_not_know_it() =
        runBlocking {
            val first = photoFile("first")
            val stored = match(listOf(first))
            repository().saveMatch(stored)
            File(photoRoot, "photos/late.jpg").writeBytes(byteArrayOf(9))

            val attached = repository().appendPhoto(stored.id, ImportedPhoto("photos/late.jpg", 10, 10, 1))
            repository().saveMatch(stored.copy(result = MatchResult.LOSS))

            assertTrue(attached)
            val reloaded = repository().observeById(stored.id).first()!!
            assertEquals(listOf("photos/first.jpg", "photos/late.jpg"), reloaded.photos.map { it.relativePath })
            assertEquals(listOf(0, 1), reloaded.photos.map { it.sortIndex })
            assertTrue(File(photoRoot, "photos/late.jpg").exists())
        }

    @Test
    fun appending_to_a_deleted_match_is_refused() =
        runBlocking {
            val stored = match(emptyList())
            repository().saveMatch(stored)
            repository().deleteMatch(stored.id)

            assertFalse(repository().appendPhoto(stored.id, ImportedPhoto("photos/x.jpg", 10, 10, 1)))
        }
}
