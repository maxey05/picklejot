@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.match

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.data.person.RoomPersonRepository
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.photo.PhotoRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class MatchDeletionTest {
    private lateinit var database: PicklejotDatabase
    private lateinit var photoRoot: File
    private lateinit var matches: RoomMatchRepository
    private lateinit var people: RoomPersonRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, PicklejotDatabase::class.java)
                .build()
        photoRoot = File(context.cacheDir, "match-deletion-test-${Uuid.random()}").apply { mkdirs() }
        matches = RoomMatchRepository(database, PhotoStore(photoRoot), Dispatchers.IO)
        people = RoomPersonRepository(database, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        database.close()
        photoRoot.deleteRecursively()
    }

    private fun writePhoto(
        relativePath: String,
        sortIndex: Int,
    ): PhotoRef {
        val file = File(photoRoot, relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(1, 2, 3))
        return PhotoRef(Uuid.random(), relativePath, 1, 1, 3, sortIndex)
    }

    private fun match(
        opponents: List<Person> = emptyList(),
        partner: Person? = null,
        photos: List<PhotoRef> = emptyList(),
    ): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.DOUBLES,
            date = AppDate.parse("2026-09-24"),
            result = MatchResult.LOSS,
            createdAt = AppInstant.fromEpochMilliseconds(1_000),
            updatedAt = AppInstant.fromEpochMilliseconds(1_000),
            opponents = opponents,
            partner = partner,
            games = listOf(GameScore(1, 9, 11), GameScore(2, 11, 8)),
            photos = photos,
        )

    @Test
    fun `a_confirmed_delete_removes_the_match_its_rows_and_its_photo_files`() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")
            val cy = people.findOrCreatePerson("Cy")
            val photos = listOf(writePhoto("photos/a.jpg", 0), writePhoto("photos/b.jpg", 1))
            val doomed = match(opponents = listOf(ana), partner = cy, photos = photos)
            matches.saveMatch(doomed)

            matches.deleteMatch(doomed.id)

            assertNull(matches.observeById(doomed.id).first())
            assertEquals(0, countWith("SELECT COUNT(*) FROM match_person"))
            assertEquals(0, countWith("SELECT COUNT(*) FROM game_score"))
            assertEquals(0, countWith("SELECT COUNT(*) FROM photo"))
            photos.forEach { photo -> assertFalse(File(photoRoot, photo.relativePath).exists()) }
        }

    @Test
    fun `deleting_one_match_leaves_another_matches_rows_and_photo_files_alone`() =
        runBlocking {
            val doomed = match(photos = listOf(writePhoto("photos/doomed.jpg", 0)))
            val kept = match(photos = listOf(writePhoto("photos/kept.jpg", 0)))
            matches.saveMatch(doomed)
            matches.saveMatch(kept)

            matches.deleteMatch(doomed.id)

            val survivor = matches.observeById(kept.id).first()
            assertNotNull(survivor)
            assertEquals(2, survivor?.games?.size)
            assertEquals(listOf("photos/kept.jpg"), survivor?.photos?.map { it.relativePath })
            assertTrue(File(photoRoot, "photos/kept.jpg").exists())
            assertFalse(File(photoRoot, "photos/doomed.jpg").exists())
        }

    @Test
    fun `deleting_a_match_whose_photo_file_is_already_gone_still_succeeds`() =
        runBlocking {
            val photo = writePhoto("photos/vanished.jpg", 0)
            File(photoRoot, photo.relativePath).delete()
            val doomed = match(photos = listOf(photo))
            matches.saveMatch(doomed)

            matches.deleteMatch(doomed.id)

            assertNull(matches.observeById(doomed.id).first())
        }

    @Test
    fun `every_person_on_a_deleted_match_survives_the_delete`() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")
            val ben = people.findOrCreatePerson("Ben")
            val cy = people.findOrCreatePerson("Cy")
            val doomed = match(opponents = listOf(ana, ben), partner = cy)
            matches.saveMatch(doomed)

            matches.deleteMatch(doomed.id)

            assertEquals(listOf(ana, ben, cy), people.observeAll().first())
        }

    private fun countWith(query: String): Int =
        database.openHelper.readableDatabase
            .query(query)
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
}
