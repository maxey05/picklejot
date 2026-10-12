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
import com.maxeydev.picklejot.domain.match.FreeTextField
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.match.suggestFreeText
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.person.suggestPeople
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.TimeSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val SCALE_MATCHES = 1_000
private const val SCALE_PEOPLE = 200
private const val SCALE_BUDGET_MILLIS = 500L

@RunWith(AndroidJUnit4::class)
class SuggestionQueryTest {
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
        photoRoot = File(context.cacheDir, "suggestion-query-test-${Uuid.random()}").apply { mkdirs() }
        matches = RoomMatchRepository(database, PhotoStore(photoRoot), Dispatchers.IO)
        people = RoomPersonRepository(database, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        database.close()
        photoRoot.deleteRecursively()
    }

    private fun match(
        date: String,
        createdAtMillis: Long = 1_000,
        opponents: List<Person> = emptyList(),
        partner: Person? = null,
        location: String? = null,
        paddle: String? = null,
    ): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.DOUBLES,
            date = AppDate.parse(date),
            result = MatchResult.WIN,
            createdAt = AppInstant.fromEpochMilliseconds(createdAtMillis),
            updatedAt = AppInstant.fromEpochMilliseconds(createdAtMillis),
            location = location,
            opponents = opponents,
            partner = partner,
            paddle = paddle,
        )

    @Test
    fun people_are_ranked_by_the_date_they_were_last_played() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")
            val ben = people.findOrCreatePerson("Ben")
            val cara = people.findOrCreatePerson("Cara")
            matches.saveMatch(match("2026-09-01", opponents = listOf(ana)))
            matches.saveMatch(match("2026-09-20", opponents = listOf(ben)))
            matches.saveMatch(match("2026-08-01", opponents = listOf(cara)))
            matches.saveMatch(match("2026-09-10", opponents = listOf(cara)))
            matches.saveMatch(match("2026-09-15", opponents = listOf(ben), partner = ana))

            val usage = people.observeRecentlyUsed().first()

            assertEquals(listOf("Ben", "Ana", "Cara"), usage.map { it.person.displayName })
            assertEquals(AppDate.parse("2026-09-15"), usage.first { it.person.id == ana.id }.lastPlayedOn)
        }

    @Test
    fun a_person_left_with_no_matches_is_not_offered() =
        runBlocking {
            val ghost = people.findOrCreatePerson("Ghost")
            val kept = people.findOrCreatePerson("Kept")
            val deleted = match("2026-09-01", opponents = listOf(ghost))
            matches.saveMatch(deleted)
            matches.saveMatch(match("2026-09-02", opponents = listOf(kept)))
            matches.deleteMatch(deleted.id)

            assertEquals(listOf("Kept"), people.observeRecentlyUsed().first().map { it.person.displayName })
        }

    @Test
    fun find_by_id_returns_the_stored_person() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")

            assertEquals(ana, people.findById(ana.id))
            assertEquals(null, people.findById(Uuid.random()))
        }

    @Test
    fun prior_locations_and_paddles_are_distinct_and_most_recent_first() =
        runBlocking {
            matches.saveMatch(match("2026-09-01", location = "Ayala Triangle", paddle = "Joola"))
            matches.saveMatch(match("2026-09-05", location = "Ayala Triangle", paddle = "Selkirk"))
            matches.saveMatch(match("2026-09-03", location = "BGC Courts"))
            matches.saveMatch(match("2026-09-04"))

            val locations = matches.observePriorValues(FreeTextField.LOCATION).first()
            val paddles = matches.observePriorValues(FreeTextField.PADDLE).first()

            assertEquals(listOf("Ayala Triangle", "BGC Courts"), locations.map { it.value })
            assertEquals(AppDate.parse("2026-09-05"), locations.first().lastPlayedOn)
            assertEquals(listOf("Selkirk", "Joola"), paddles.map { it.value })
        }

    @Test
    fun suggestion_queries_stay_fast_with_1000_matches_and_200_people() =
        runBlocking {
            val roster = (0 until SCALE_PEOPLE).map { people.findOrCreatePerson("Player ${it + 1}") }
            val firstDay = AppDate.parse("2023-01-01")
            (0 until SCALE_MATCHES).forEach { index ->
                matches.saveMatch(
                    match(
                        date = firstDay.plus(DatePeriod(days = index)).toString(),
                        createdAtMillis = index.toLong(),
                        opponents = listOf(roster[index % SCALE_PEOPLE], roster[(index + 1) % SCALE_PEOPLE]),
                        partner = roster[(index + 2) % SCALE_PEOPLE],
                        location = "Court ${index % 25}",
                        paddle = "Paddle ${index % 10}",
                    ),
                )
            }
            people.observeRecentlyUsed().first()
            matches.observePriorValues(FreeTextField.LOCATION).first()

            val started = TimeSource.Monotonic.markNow()
            val usage = people.observeRecentlyUsed().first()
            val suggestedPeople = suggestPeople("player 1", usage)
            val locations = matches.observePriorValues(FreeTextField.LOCATION).first()
            val suggestedLocations = suggestFreeText("court", locations)
            val elapsed = started.elapsedNow().inWholeMilliseconds

            assertEquals(SCALE_PEOPLE, usage.size)
            assertEquals(5, suggestedPeople.size)
            assertEquals(5, suggestedLocations.size)
            assertTrue(
                "suggestion lookup took ${elapsed}ms, budget ${SCALE_BUDGET_MILLIS}ms",
                elapsed < SCALE_BUDGET_MILLIS,
            )
        }
}
