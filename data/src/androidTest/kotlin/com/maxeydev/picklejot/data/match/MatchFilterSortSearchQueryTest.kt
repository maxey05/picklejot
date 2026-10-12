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
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.match.SearchTerm
import com.maxeydev.picklejot.domain.person.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random
import kotlin.time.measureTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val SEARCH_BUDGET_MILLIS = 300L

@RunWith(AndroidJUnit4::class)
class MatchFilterSortSearchQueryTest {
    private lateinit var database: PicklejotDatabase
    private lateinit var photoRoot: File
    private lateinit var matches: RoomMatchRepository
    private lateinit var people: RoomPersonRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PicklejotDatabase::class.java).build()
        photoRoot = File(context.cacheDir, "filter-sort-search-test-${Uuid.random()}").apply { mkdirs() }
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
        format: MatchFormat = MatchFormat.DOUBLES,
        result: MatchResult = MatchResult.WIN,
        start: String? = null,
        end: String? = null,
        location: String? = null,
        paddle: String? = null,
        notes: String? = null,
        opponents: List<Person> = emptyList(),
        partner: Person? = null,
    ): Match =
        Match(
            id = Uuid.random(),
            format = format,
            date = AppDate.parse(date),
            result = result,
            createdAt = AppInstant.fromEpochMilliseconds(createdAtMillis),
            updatedAt = AppInstant.fromEpochMilliseconds(createdAtMillis),
            startTime = start?.let(AppTime::parse),
            endTime = end?.let(AppTime::parse),
            location = location,
            paddle = paddle,
            notes = notes,
            opponents = opponents,
            partner = partner,
        )

    private suspend fun page(
        sort: MatchSort = MatchSort.DATE_NEWEST,
        filter: FilterState = FilterState.NONE,
        search: String? = null,
        limit: Int = 100,
    ): List<Uuid> =
        matches
            .observeListPage(sort, limit, filter, search?.let { SearchTerm.of(it) })
            .first()
            .map { it.id }

    private suspend fun saveAll(vararg stored: Match) {
        stored.forEach { matches.saveMatch(it) }
    }

    @Test
    fun `shortest_first_puts_matches_without_a_duration_last_rather_than_first`() =
        runBlocking {
            val long = match("2026-01-01", start = "18:00", end = "20:30")
            val short = match("2026-01-02", start = "18:00", end = "18:45")
            val pastMidnight = match("2026-01-03", start = "23:30", end = "00:40")
            val noEnd = match("2026-01-04", start = "18:00")
            val noTimes = match("2026-01-05")
            saveAll(noTimes, long, noEnd, pastMidnight, short)

            assertEquals(
                listOf(short.id, pastMidnight.id, long.id, noTimes.id, noEnd.id),
                page(MatchSort.DURATION_SHORTEST),
            )
        }

    @Test
    fun `longest_first_also_puts_matches_without_a_duration_last`() =
        runBlocking {
            val long = match("2026-01-01", start = "18:00", end = "20:30")
            val short = match("2026-01-02", start = "18:00", end = "18:45")
            val pastMidnight = match("2026-01-03", start = "23:30", end = "00:40")
            val noEnd = match("2026-01-04", start = "18:00")
            val noTimes = match("2026-01-05")
            saveAll(noTimes, long, noEnd, pastMidnight, short)

            assertEquals(
                listOf(long.id, pastMidnight.id, short.id, noTimes.id, noEnd.id),
                page(MatchSort.DURATION_LONGEST),
            )
        }

    @Test
    fun `a_zero_length_match_is_a_real_duration_and_sorts_before_a_missing_one`() =
        runBlocking {
            val zero = match("2026-01-01", start = "10:00", end = "10:00")
            val missing = match("2026-01-02")
            saveAll(missing, zero)

            assertEquals(listOf(zero.id, missing.id), page(MatchSort.DURATION_SHORTEST))
            assertEquals(listOf(zero.id, missing.id), page(MatchSort.DURATION_LONGEST))
        }

    @Test
    fun `equal_durations_fall_back_to_newest_first`() =
        runBlocking {
            val older = match("2026-01-01", start = "08:00", end = "09:00")
            val newer = match("2026-01-02", start = "19:00", end = "20:00")
            saveAll(older, newer)

            assertEquals(listOf(newer.id, older.id), page(MatchSort.DURATION_SHORTEST))
        }

    @Test
    fun `location_a_to_z_ignores_case_and_puts_missing_locations_last_newest_first`() =
        runBlocking {
            val bgc = match("2026-01-01", location = "bgc Courts")
            val ayala = match("2026-01-02", location = "Ayala Triangle")
            val alabang = match("2026-01-03", location = "alabang")
            val noLocationOlder = match("2026-01-04")
            val blankLocationNewer = match("2026-01-05", location = "")
            saveAll(bgc, noLocationOlder, ayala, blankLocationNewer, alabang)

            assertEquals(
                listOf(alabang.id, ayala.id, bgc.id, blankLocationNewer.id, noLocationOlder.id),
                page(MatchSort.LOCATION_A_TO_Z),
            )
        }

    @Test
    fun `each_filter_on_its_own_keeps_only_the_matching_matches`() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")
            val ben = people.findOrCreatePerson("Ben")
            val singlesWin =
                match("2026-01-10", format = MatchFormat.SINGLES, location = "BGC", opponents = listOf(ana))
            val doublesLoss =
                match("2026-02-10", result = MatchResult.LOSS, location = "Ayala", opponents = listOf(ben, ana))
            val doublesWin = match("2026-03-10", location = "BGC", opponents = listOf(ben))
            saveAll(singlesWin, doublesLoss, doublesWin)

            assertEquals(listOf(singlesWin.id), page(filter = FilterState(format = MatchFormat.SINGLES)))
            assertEquals(listOf(doublesLoss.id), page(filter = FilterState(result = MatchResult.LOSS)))
            assertEquals(
                listOf(doublesWin.id, doublesLoss.id),
                page(
                    filter = FilterState(fromDate = AppDate.parse("2026-02-10"), toDate = AppDate.parse("2026-03-10")),
                ),
            )
            assertEquals(
                listOf(singlesWin.id),
                page(filter = FilterState(toDate = AppDate.parse("2026-01-10"))),
            )
            assertEquals(listOf(doublesWin.id, singlesWin.id), page(filter = FilterState(location = "BGC")))
        }

    @Test
    fun `the_opponent_filter_matches_by_person_id_in_either_opponent_slot_but_not_as_partner`() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")
            val ben = people.findOrCreatePerson("Ben")
            val cara = people.findOrCreatePerson("Cara")
            val anaFirst = match("2026-01-01", opponents = listOf(ana, ben))
            val anaSecond = match("2026-01-02", opponents = listOf(ben, ana))
            val anaAsPartner = match("2026-01-03", opponents = listOf(cara), partner = ana)
            saveAll(anaFirst, anaSecond, anaAsPartner)

            assertEquals(listOf(anaSecond.id, anaFirst.id), page(filter = FilterState(opponentId = ana.id)))
        }

    @Test
    fun `filters_combine_with_and_and_keep_the_chosen_sort`() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")
            val winShort = match("2026-01-01", opponents = listOf(ana), start = "18:00", end = "18:30")
            val winLong = match("2026-01-02", opponents = listOf(ana), start = "18:00", end = "20:00")
            val lossWithAna = match("2026-01-03", result = MatchResult.LOSS, opponents = listOf(ana))
            val winWithoutAna = match("2026-01-04")
            saveAll(winShort, winLong, lossWithAna, winWithoutAna)
            val winsAgainstAna = FilterState(result = MatchResult.WIN, opponentId = ana.id)

            assertEquals(listOf(winLong.id, winShort.id), page(MatchSort.DATE_NEWEST, winsAgainstAna))
            assertEquals(listOf(winShort.id, winLong.id), page(MatchSort.DATE_OLDEST, winsAgainstAna))
            assertEquals(listOf(winShort.id, winLong.id), page(MatchSort.DURATION_SHORTEST, winsAgainstAna))
            assertEquals(listOf(winLong.id, winShort.id), page(MatchSort.DURATION_LONGEST, winsAgainstAna))
        }

    @Test
    fun `search_finds_opponent_partner_location_paddle_and_notes_ignoring_case`() =
        runBlocking {
            val ben = people.findOrCreatePerson("Ben")
            val cara = people.findOrCreatePerson("Cara")
            val byOpponent = match("2026-01-01", opponents = listOf(ben))
            val byPartner = match("2026-01-02", opponents = listOf(ben), partner = cara)
            val byLocation = match("2026-01-03", location = "Ayala Triangle")
            val byPaddle = match("2026-01-04", paddle = "Selkirk AMPED")
            val byNotes = match("2026-01-05", notes = "Great DINKS today")
            saveAll(byOpponent, byPartner, byLocation, byPaddle, byNotes)

            assertEquals(listOf(byPartner.id, byOpponent.id), page(search = "BEN"))
            assertEquals(listOf(byPartner.id), page(search = "car"))
            assertEquals(listOf(byLocation.id), page(search = "triang"))
            assertEquals(listOf(byPaddle.id), page(search = "amped"))
            assertEquals(listOf(byNotes.id), page(search = "dinks"))
        }

    @Test
    fun `search_folds_case_for_non_ascii_names`() =
        runBlocking {
            val pena = people.findOrCreatePerson("Peña")
            val stored = match("2026-01-01", opponents = listOf(pena))
            saveAll(stored)

            assertEquals(listOf(stored.id), page(search = "PEÑA"))
        }

    @Test
    fun `percent_and_underscore_typed_into_search_are_matched_literally`() =
        runBlocking {
            val percent = match("2026-01-01", notes = "100% effort")
            val underscore = match("2026-01-02", notes = "windy_day")
            val neither = match("2026-01-03", notes = "windy day, 1000 effort")
            saveAll(percent, underscore, neither)

            assertEquals(listOf(percent.id), page(search = "100%"))
            assertEquals(listOf(underscore.id), page(search = "y_d"))
        }

    @Test
    fun `search_respects_the_active_filters_and_sort`() =
        runBlocking {
            val singlesBgc = match("2026-01-01", format = MatchFormat.SINGLES, location = "BGC")
            val doublesBgcOld = match("2026-01-02", location = "BGC North")
            val doublesBgcNew = match("2026-01-03", location = "bgc south")
            val doublesElsewhere = match("2026-01-04", location = "Ayala")
            saveAll(singlesBgc, doublesBgcOld, doublesBgcNew, doublesElsewhere)

            assertEquals(
                listOf(doublesBgcOld.id, doublesBgcNew.id),
                page(MatchSort.DATE_OLDEST, FilterState(format = MatchFormat.DOUBLES), search = "bgc"),
            )
        }

    @Test
    fun `a_blank_search_filters_nothing`() =
        runBlocking {
            saveAll(match("2026-01-01"), match("2026-01-02"))

            assertEquals(2, page(search = "   ").size)
        }

    @Test
    fun `search_over_1000_matches_with_long_notes_returns_within_300_ms`() =
        runBlocking {
            val random = Random(6)
            val words = "dink drive lob serve kitchen erne volley reset third shot drop stack poach windy".split(" ")
            val roster = List(200) { people.findOrCreatePerson("Player $it") }
            val seeded =
                List(1_000) { index ->
                    val pair = roster.shuffled(random).take(3)
                    match(
                        date = AppDate.parse("2025-01-01").plus(index % 365, DateTimeUnit.DAY).toString(),
                        createdAtMillis = index.toLong(),
                        location = "Court ${index % 30}",
                        paddle = "Paddle ${index % 10}",
                        notes = List(330) { words.random(random) }.joinToString(" "),
                        opponents = pair.take(2),
                        partner = pair[2],
                        start = "18:00",
                        end = "19:%02d".format(index % 60),
                    )
                }
            seeded.forEach { matches.saveMatch(it) }
            page(search = "warm up")

            listOf("zzz-no-hit", "Player 17", "kitchen", "court 2").forEach { term ->
                listOf(MatchSort.DATE_NEWEST, MatchSort.DURATION_LONGEST).forEach { sort ->
                    val elapsed = measureTime { page(sort, search = term, limit = 50) }
                    assertTrue(
                        "search '$term' sorted $sort took $elapsed, over the ${SEARCH_BUDGET_MILLIS}ms budget",
                        elapsed.inWholeMilliseconds < SEARCH_BUDGET_MILLIS,
                    )
                }
            }
        }
}
