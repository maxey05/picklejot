@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.match

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.data.person.RoomPersonRepository
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class MatchPersistenceTest {
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
        photoRoot = File(context.cacheDir, "match-persistence-test-${Uuid.random()}").apply { mkdirs() }
        matches = RoomMatchRepository(database, PhotoStore(photoRoot), Dispatchers.IO)
        people = RoomPersonRepository(database, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        database.close()
        photoRoot.deleteRecursively()
    }

    private fun match(
        id: Uuid,
        format: MatchFormat,
        opponents: List<Person> = emptyList(),
        partner: Person? = null,
        games: List<GameScore> = emptyList(),
        photos: List<PhotoRef> = emptyList(),
    ): Match =
        Match(
            id = id,
            format = format,
            date = AppDate.parse("2026-09-23"),
            result = MatchResult.WIN,
            createdAt = AppInstant.fromEpochMilliseconds(1_000),
            updatedAt = AppInstant.fromEpochMilliseconds(2_000),
            startTime = AppTime.parse("18:30"),
            endTime = AppTime.parse("19:45"),
            location = "Ayala Triangle",
            opponents = opponents,
            partner = partner,
            games = games,
            paddle = "Selkirk",
            notes = "Windy.",
            photos = photos,
        )

    @Test
    fun `a_match_round_trips_through_a_Flow_with_opponents_partner_games_and_photos_intact`() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")
            val ben = people.findOrCreatePerson("Ben")
            val cy = people.findOrCreatePerson("Cy")
            val id = Uuid.random()
            val saved =
                match(
                    id = id,
                    format = MatchFormat.DOUBLES,
                    opponents = listOf(ana, ben),
                    partner = cy,
                    games = listOf(GameScore(2, 8, 11), GameScore(1, 11, 9)),
                    photos =
                        listOf(
                            PhotoRef(Uuid.random(), "photos/b.jpg", 800, 600, 4_096, 1),
                            PhotoRef(Uuid.random(), "photos/a.jpg", 640, 480, 2_048, 0),
                        ),
                )
            matches.saveMatch(saved)

            val loaded = matches.observeById(id).first()
            assertNotNull(loaded)
            requireNotNull(loaded)

            assertEquals(saved.format, loaded.format)
            assertEquals(saved.date, loaded.date)
            assertEquals(saved.result, loaded.result)
            assertEquals(saved.startTime, loaded.startTime)
            assertEquals(saved.endTime, loaded.endTime)
            assertEquals(saved.location, loaded.location)
            assertEquals(saved.paddle, loaded.paddle)
            assertEquals(saved.notes, loaded.notes)
            assertEquals(saved.createdAt, loaded.createdAt)
            assertEquals(saved.updatedAt, loaded.updatedAt)
            assertEquals(listOf(ana, ben), loaded.opponents)
            assertEquals(cy, loaded.partner)
            assertEquals(listOf(1, 2), loaded.games.map { it.gameNumber })
            assertEquals(listOf(11, 8), loaded.games.map { it.myScore })
            assertEquals(listOf("photos/a.jpg", "photos/b.jpg"), loaded.photos.map { it.relativePath })
        }

    @Test
    fun `a_singles_match_carrying_a_partner_is_rejected_before_anything_is_written`() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")
            val id = Uuid.random()
            val invalid = match(id = id, format = MatchFormat.SINGLES, partner = ana)

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { matches.saveMatch(invalid) }
            }
            assertNull(matches.observeById(id).first())
        }

    @Test
    fun `the_same_person_in_two_slots_is_rejected_before_anything_is_written`() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")
            val ben = people.findOrCreatePerson("Ben")
            val twiceOpponent = Uuid.random()
            val partnerAndOpponent = Uuid.random()

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    matches.saveMatch(match(twiceOpponent, MatchFormat.DOUBLES, opponents = listOf(ana, ana)))
                }
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    matches.saveMatch(
                        match(partnerAndOpponent, MatchFormat.DOUBLES, opponents = listOf(ana, ben), partner = ben),
                    )
                }
            }
            assertNull(matches.observeById(twiceOpponent).first())
            assertNull(matches.observeById(partnerAndOpponent).first())
            assertEquals(0, countMatchPersonRows())
        }

    @Test
    fun `a_match_with_zero_opponents_saves_on_either_format`() =
        runBlocking {
            val singles = Uuid.random()
            val doubles = Uuid.random()
            matches.saveMatch(match(singles, MatchFormat.SINGLES))
            matches.saveMatch(match(doubles, MatchFormat.DOUBLES))

            assertEquals(emptyList<Person>(), matches.observeById(singles).first()?.opponents)
            assertEquals(emptyList<Person>(), matches.observeById(doubles).first()?.opponents)
        }

    @Test
    fun `deleting_a_match_cascades_to_its_rows_and_leaves_every_person_intact`() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")
            val id = Uuid.random()
            matches.saveMatch(
                match(
                    id = id,
                    format = MatchFormat.SINGLES,
                    opponents = listOf(ana),
                    games = listOf(GameScore(1, 11, 4)),
                    photos = listOf(PhotoRef(Uuid.random(), "photos/a.jpg", 1, 1, 1, 0)),
                ),
            )

            matches.deleteMatch(id)

            assertNull(matches.observeById(id).first())
            assertEquals(0, countMatchPersonRows())
            assertEquals(0, countGameScoreRows())
            assertEquals(0, countPhotoRows())
            assertEquals(listOf(ana), people.observeAll().first())
        }

    @Test
    fun `a_write_that_fails_part_way_leaves_no_partial_match_behind`() =
        runBlocking {
            val id = Uuid.random()
            val duplicateGameNumbers = listOf(GameScore(1, 11, 9), GameScore(1, 9, 11))

            assertThrows(SQLiteConstraintException::class.java) {
                runBlocking { matches.saveMatch(match(id, MatchFormat.SINGLES, games = duplicateGameNumbers)) }
            }
            assertNull(matches.observeById(id).first())
            assertEquals(0, countGameScoreRows())
        }

    private fun countMatchPersonRows(): Int = countWith("SELECT COUNT(*) FROM match_person")

    private fun countGameScoreRows(): Int = countWith("SELECT COUNT(*) FROM game_score")

    private fun countPhotoRows(): Int = countWith("SELECT COUNT(*) FROM photo")

    private fun countWith(query: String): Int =
        database.openHelper.readableDatabase
            .query(query)
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
}
