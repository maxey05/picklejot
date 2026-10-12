@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.data.match

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.data.person.RoomPersonRepository
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.photo.PhotoRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class MatchListQueryTest {
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
        photoRoot = File(context.cacheDir, "match-list-query-test-${Uuid.random()}").apply { mkdirs() }
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
        result: MatchResult = MatchResult.WIN,
        format: MatchFormat = MatchFormat.DOUBLES,
        opponents: List<Person> = emptyList(),
        games: List<GameScore> = emptyList(),
        photos: List<PhotoRef> = emptyList(),
    ): Match =
        Match(
            id = Uuid.random(),
            format = format,
            date = AppDate.parse(date),
            result = result,
            createdAt = AppInstant.fromEpochMilliseconds(createdAtMillis),
            updatedAt = AppInstant.fromEpochMilliseconds(createdAtMillis),
            opponents = opponents,
            games = games,
            photos = photos,
        )

    private fun photo(
        path: String,
        sortIndex: Int,
    ): PhotoRef = PhotoRef(Uuid.random(), path, width = 2048, height = 1536, byteSize = 400_000, sortIndex = sortIndex)

    private suspend fun page(
        sort: MatchSort,
        limit: Int = 100,
    ): List<Uuid> = matches.observeListPage(sort, limit).first().map { it.id }

    @Test
    fun `a_page_holds_at_most_the_requested_limit_so_the_whole_table_is_never_loaded`() =
        runBlocking {
            repeat(120) { index ->
                matches.saveMatch(match(date = "2026-01-01", createdAtMillis = index.toLong()))
            }

            assertEquals(50, matches.observeListPage(MatchSort.DATE_NEWEST, 50).first().size)
            assertEquals(1, matches.observeListPage(MatchSort.OPPONENT_A_TO_Z, 1).first().size)
            assertEquals(120, matches.observeListPage(MatchSort.DATE_OLDEST, 500).first().size)
        }

    @Test
    fun `growing_the_limit_extends_the_same_ordering_rather_than_reshuffling_it`() =
        runBlocking {
            repeat(30) { index ->
                matches.saveMatch(match(date = "2026-01-01", createdAtMillis = (index % 3).toLong()))
            }

            val firstPage = page(MatchSort.DATE_NEWEST, limit = 10)
            val widerPage = page(MatchSort.DATE_NEWEST, limit = 20)

            assertEquals(firstPage, widerPage.take(10))
        }

    @Test
    fun `a_non_positive_limit_is_rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            matches.observeListPage(MatchSort.DATE_NEWEST, 0)
        }
    }

    @Test
    fun `newest_first_orders_by_date_then_by_when_the_match_was_logged`() =
        runBlocking {
            val old = match(date = "2026-03-01")
            val sameDayEarlier = match(date = "2026-09-20", createdAtMillis = 1_000)
            val sameDayLater = match(date = "2026-09-20", createdAtMillis = 2_000)
            listOf(sameDayEarlier, old, sameDayLater).forEach { matches.saveMatch(it) }

            assertEquals(listOf(sameDayLater.id, sameDayEarlier.id, old.id), page(MatchSort.DATE_NEWEST))
        }

    @Test
    fun `oldest_first_is_the_exact_reverse_of_newest_first`() =
        runBlocking {
            val old = match(date = "2026-03-01")
            val sameDayEarlier = match(date = "2026-09-20", createdAtMillis = 1_000)
            val sameDayLater = match(date = "2026-09-20", createdAtMillis = 2_000)
            listOf(sameDayLater, old, sameDayEarlier).forEach { matches.saveMatch(it) }

            assertEquals(listOf(old.id, sameDayEarlier.id, sameDayLater.id), page(MatchSort.DATE_OLDEST))
        }

    @Test
    fun `wins_first_puts_every_win_before_any_loss_and_keeps_each_group_newest_first`() =
        runBlocking {
            val oldWin = match(date = "2026-01-01", result = MatchResult.WIN)
            val newWin = match(date = "2026-05-01", result = MatchResult.WIN)
            val oldLoss = match(date = "2026-02-01", result = MatchResult.LOSS)
            val newLoss = match(date = "2026-09-01", result = MatchResult.LOSS)
            listOf(oldLoss, newWin, newLoss, oldWin).forEach { matches.saveMatch(it) }

            assertEquals(listOf(newWin.id, oldWin.id, newLoss.id, oldLoss.id), page(MatchSort.RESULT_WINS_FIRST))
        }

    @Test
    fun `losses_first_puts_every_loss_before_any_win_and_keeps_each_group_newest_first`() =
        runBlocking {
            val oldWin = match(date = "2026-01-01", result = MatchResult.WIN)
            val newWin = match(date = "2026-05-01", result = MatchResult.WIN)
            val oldLoss = match(date = "2026-02-01", result = MatchResult.LOSS)
            val newLoss = match(date = "2026-09-01", result = MatchResult.LOSS)
            listOf(oldWin, newLoss, newWin, oldLoss).forEach { matches.saveMatch(it) }

            assertEquals(listOf(newLoss.id, oldLoss.id, newWin.id, oldWin.id), page(MatchSort.RESULT_LOSSES_FIRST))
        }

    @Test
    fun `opponent_a_to_z_uses_the_first_slot_ignores_case_and_puts_matches_without_an_opponent_last`() =
        runBlocking {
            val zoe = people.findOrCreatePerson("Zoe")
            val aaron = people.findOrCreatePerson("Aaron")
            val bea = people.findOrCreatePerson("bea")
            val carl = people.findOrCreatePerson("Carl")
            val zoeThenAaron = match(date = "2026-09-01", opponents = listOf(zoe, aaron))
            val lowercaseBea = match(date = "2026-09-02", opponents = listOf(bea))
            val carlAlone = match(date = "2026-09-03", format = MatchFormat.SINGLES, opponents = listOf(carl))
            val nobodyNewer = match(date = "2026-09-10")
            val nobodyOlder = match(date = "2026-08-10")
            val scrambled = listOf(nobodyNewer, zoeThenAaron, nobodyOlder, carlAlone, lowercaseBea)
            scrambled.forEach { matches.saveMatch(it) }

            assertEquals(
                listOf(lowercaseBea.id, carlAlone.id, zoeThenAaron.id, nobodyNewer.id, nobodyOlder.id),
                page(MatchSort.OPPONENT_A_TO_Z),
            )
        }

    @Test
    fun `a_row_carries_both_opponents_in_slot_order_ordered_games_and_the_primary_photo`() =
        runBlocking {
            val ana = people.findOrCreatePerson("Ana")
            val ben = people.findOrCreatePerson("Ben")
            val secondPhoto = photo("photos/second.jpg", sortIndex = 1)
            val firstPhoto = photo("photos/first.jpg", sortIndex = 0)
            val stored =
                match(
                    date = "2026-09-20",
                    opponents = listOf(ana, ben),
                    games = listOf(GameScore(2, 9, 11), GameScore(1, 11, 7)),
                    photos = listOf(secondPhoto, firstPhoto),
                )
            matches.saveMatch(stored)

            val row = matches.observeListPage(MatchSort.DATE_NEWEST, 10).first().single()

            assertEquals(stored.id, row.id)
            assertEquals(AppDate.parse("2026-09-20"), row.date)
            assertEquals(MatchFormat.DOUBLES, row.format)
            assertEquals(MatchResult.WIN, row.result)
            assertEquals(listOf("Ana", "Ben"), row.opponentNames)
            assertEquals(listOf(GameScore(1, 11, 7), GameScore(2, 9, 11)), row.games)
            assertEquals("photos/first.jpg", row.primaryPhotoPath)
        }

    @Test
    fun `a_bare_match_has_no_opponents_no_games_and_no_photo`() =
        runBlocking {
            matches.saveMatch(match(date = "2026-09-20", format = MatchFormat.SINGLES))

            val row = matches.observeListPage(MatchSort.DATE_NEWEST, 10).first().single()

            assertEquals(emptyList<String>(), row.opponentNames)
            assertEquals(emptyList<GameScore>(), row.games)
            assertNull(row.primaryPhotoPath)
        }

    @Test
    fun `the_page_re_emits_when_a_match_is_saved_or_deleted`() =
        runBlocking {
            val stored = match(date = "2026-09-20")

            matches.observeListPage(MatchSort.DATE_NEWEST, 10).test {
                assertEquals(0, awaitItem().size)
                matches.saveMatch(stored)
                assertEquals(listOf(stored.id), awaitItem().map { it.id })
                matches.deleteMatch(stored.id)
                assertEquals(0, awaitItem().size)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
