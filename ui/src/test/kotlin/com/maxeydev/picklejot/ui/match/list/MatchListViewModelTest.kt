@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.maxeydev.picklejot.ui.match.list

import androidx.lifecycle.SavedStateHandle
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.entitlement.CapWarning
import com.maxeydev.picklejot.domain.match.FilterKind
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.match.SearchTerm
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.domain.photo.PhotoRef
import com.maxeydev.picklejot.ui.fakes.FakeEntitlementRepository
import com.maxeydev.picklejot.ui.fakes.FakeMatchRepository
import com.maxeydev.picklejot.ui.fakes.FakeMatchSortStore
import com.maxeydev.picklejot.ui.fakes.FakePersonRepository
import com.maxeydev.picklejot.ui.fakes.ListPageRequest
import com.maxeydev.picklejot.ui.navigation.JUST_SAVED_MATCH_ID_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MatchListViewModelTest {
    private val photoRoot = File("/data/user/0/picklejot/files")
    private lateinit var matches: FakeMatchRepository
    private lateinit var sorts: FakeMatchSortStore
    private lateinit var handle: SavedStateHandle
    private lateinit var people: FakePersonRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        sorts = FakeMatchSortStore()
        handle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun match(
        day: Int,
        result: MatchResult = MatchResult.WIN,
        format: MatchFormat = MatchFormat.DOUBLES,
        photos: List<PhotoRef> = emptyList(),
        location: String? = null,
        notes: String? = null,
        opponents: List<Person> = emptyList(),
        partner: Person? = null,
    ): Match =
        Match(
            id = Uuid.random(),
            format = format,
            date = AppDate.parse("2026-01-01").plus(day, DateTimeUnit.DAY),
            result = result,
            createdAt = AppInstant.fromEpochMilliseconds(1_000),
            updatedAt = AppInstant.fromEpochMilliseconds(1_000),
            photos = photos,
            location = location,
            notes = notes,
            opponents = opponents,
            partner = partner,
        )

    private fun person(name: String): Person = Person(Uuid.random(), name, AppInstant.fromEpochMilliseconds(0))

    private fun TestScope.subscribedViewModel(
        stored: List<Match>,
        knownPeople: List<Person> = emptyList(),
        entitlements: FakeEntitlementRepository = FakeEntitlementRepository(),
    ): MatchListViewModel {
        matches = FakeMatchRepository(stored)
        people = FakePersonRepository(initial = knownPeople)
        val viewModel =
            MatchListViewModel(
                savedStateHandle = handle,
                matchRepository = matches,
                personRepository = people,
                matchSortStore = sorts,
                entitlementRepository = entitlements,
                photoFile = { relativePath -> File(photoRoot, relativePath) },
                defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `the list opens newest first and asks the repository for exactly one page`() =
        runTest {
            val older = match(day = 1)
            val newer = match(day = 2)

            val state = subscribedViewModel(listOf(older, newer)).uiState.value

            assertEquals(MatchSort.DATE_NEWEST, state.sort)
            assertEquals(listOf(newer.id.toString(), older.id.toString()), state.matches.map { it.id })
            assertEquals(listOf(MatchSort.DATE_NEWEST to MATCH_LIST_PAGE_SIZE), matches.requestedPages)
            assertFalse(state.isLoading)
        }

    @Test
    fun `a full first page holds exactly one page of rows even when far more exist`() =
        runTest {
            val state = subscribedViewModel(List(1_000) { match(day = it) }).uiState.value

            assertEquals(MATCH_LIST_PAGE_SIZE, state.matches.size)
            assertTrue(state.canLoadMore)
        }

    @Test
    fun `scrolling near the end of a full page asks for the next page`() =
        runTest {
            val viewModel = subscribedViewModel(List(120) { match(day = it) })

            viewModel.loadMoreIfNeeded(MATCH_LIST_PAGE_SIZE - MATCH_LIST_PREFETCH_DISTANCE)

            assertEquals(2 * MATCH_LIST_PAGE_SIZE, matches.requestedPages.last().second)
            assertEquals(2 * MATCH_LIST_PAGE_SIZE, viewModel.uiState.value.matches.size)
        }

    @Test
    fun `scrolling well above the end loads nothing more`() =
        runTest {
            val viewModel = subscribedViewModel(List(120) { match(day = it) })

            viewModel.loadMoreIfNeeded(10)

            assertEquals(1, matches.requestedPages.size)
        }

    @Test
    fun `the same scroll position never asks for another page twice`() =
        runTest {
            val viewModel = subscribedViewModel(List(300) { match(day = it) })
            val nearEndOfFirstPage = MATCH_LIST_PAGE_SIZE - 1

            viewModel.loadMoreIfNeeded(nearEndOfFirstPage)
            viewModel.loadMoreIfNeeded(nearEndOfFirstPage)

            val requestedLimits = matches.requestedPages.map { it.second }
            assertEquals(listOf(MATCH_LIST_PAGE_SIZE, 2 * MATCH_LIST_PAGE_SIZE), requestedLimits)
        }

    @Test
    fun `reaching the end of a short list asks for nothing more`() =
        runTest {
            val viewModel = subscribedViewModel(List(10) { match(day = it) })

            viewModel.loadMoreIfNeeded(9)

            assertFalse(viewModel.uiState.value.canLoadMore)
            assertEquals(1, matches.requestedPages.size)
        }

    @Test
    fun `choosing a sort saves it re_queries with that sort and starts again from one page`() =
        runTest {
            val loss = match(day = 1, result = MatchResult.LOSS)
            val win = match(day = 2, result = MatchResult.WIN)
            val viewModel = subscribedViewModel(listOf(loss, win) + List(118) { match(day = 10 + it) })
            viewModel.loadMoreIfNeeded(MATCH_LIST_PAGE_SIZE - 1)

            viewModel.selectSort(MatchSort.RESULT_LOSSES_FIRST)

            val state = viewModel.uiState.value
            assertEquals(listOf(MatchSort.RESULT_LOSSES_FIRST), sorts.saved)
            assertEquals(MatchSort.RESULT_LOSSES_FIRST, state.sort)
            assertEquals(MatchSort.RESULT_LOSSES_FIRST to MATCH_LIST_PAGE_SIZE, matches.requestedPages.last())
            assertEquals(loss.id.toString(), state.matches.first().id)
        }

    @Test
    fun `a sort chosen in an earlier session is the one the list opens with`() =
        runTest {
            sorts = FakeMatchSortStore(initial = MatchSort.OPPONENT_A_TO_Z)

            val state = subscribedViewModel(listOf(match(day = 1))).uiState.value

            assertEquals(MatchSort.OPPONENT_A_TO_Z, state.sort)
            assertEquals(listOf(MatchSort.OPPONENT_A_TO_Z to MATCH_LIST_PAGE_SIZE), matches.requestedPages)
        }

    @Test
    fun `a stored photo path resolves to a file in app storage and a match without one has no thumbnail`() =
        runTest {
            val withPhoto =
                match(
                    day = 2,
                    photos = listOf(PhotoRef(Uuid.random(), "photos/a.jpg", 2048, 1536, 400_000, 0)),
                )
            val withoutPhoto = match(day = 1)

            val rows = subscribedViewModel(listOf(withPhoto, withoutPhoto)).uiState.value.matches

            assertEquals(File(photoRoot, "photos/a.jpg").path, rows[0].thumbnailPath)
            assertNull(rows[1].thumbnailPath)
        }

    @Test
    fun `an empty log is an empty state rather than a loading state`() =
        runTest {
            val state = subscribedViewModel(emptyList()).uiState.value

            assertTrue(state.isEmpty)
            assertFalse(state.canLoadMore)
        }

    @Test
    fun `a just-saved match id handed back by the edit screen offers log another`() =
        runTest {
            val saved = match(day = 1)
            val viewModel = subscribedViewModel(listOf(saved))
            assertNull(viewModel.uiState.value.savedMatchId)

            handle[JUST_SAVED_MATCH_ID_KEY] = saved.id.toString()

            assertEquals(saved.id.toString(), viewModel.uiState.value.savedMatchId)
        }

    @Test
    fun `dismissing the saved confirmation clears the offer so it is not shown again`() =
        runTest {
            val saved = match(day = 1)
            handle[JUST_SAVED_MATCH_ID_KEY] = saved.id.toString()
            val viewModel = subscribedViewModel(listOf(saved))

            viewModel.dismissSavedConfirmation()

            assertNull(viewModel.uiState.value.savedMatchId)
            assertNull(handle.get<String>(JUST_SAVED_MATCH_ID_KEY))
            assertEquals(1, viewModel.uiState.value.matches.size)
        }

    @Test
    fun `a filter change re_queries from one page and is the same filter the screen state exposes`() =
        runTest {
            val loss = match(day = 1, result = MatchResult.LOSS)
            val viewModel = subscribedViewModel(listOf(loss) + List(119) { match(day = 10 + it) })
            viewModel.loadMoreIfNeeded(MATCH_LIST_PAGE_SIZE - 1)
            val lossesOnly = FilterState(result = MatchResult.LOSS)

            viewModel.changeFilter(lossesOnly)

            val state = viewModel.uiState.value
            assertEquals(lossesOnly, state.filter)
            assertEquals(
                ListPageRequest(MatchSort.DATE_NEWEST, MATCH_LIST_PAGE_SIZE, lossesOnly, null),
                matches.requestedQueries.last(),
            )
            assertEquals(listOf(loss.id.toString()), state.matches.map { it.id })
        }

    @Test
    fun `the filter lives in the saved state that both the list query and the screen read`() =
        runTest {
            val viewModel = subscribedViewModel(listOf(match(day = 1)))
            val singles = FilterState(format = MatchFormat.SINGLES)

            viewModel.changeFilter(singles)

            assertEquals(singles, decodeFilterState(handle[FILTER_STATE_KEY]))
            assertEquals(singles, viewModel.uiState.value.filter)
            assertEquals(singles, matches.requestedQueries.last().filter)
        }

    @Test
    fun `a filter set elsewhere in the saved state reaches the list without going through the list screen`() =
        runTest {
            val viewModel = subscribedViewModel(listOf(match(day = 1, result = MatchResult.LOSS), match(day = 2)))
            val winsOnly = FilterState(result = MatchResult.WIN)

            handle[FILTER_STATE_KEY] = encodeFilterState(winsOnly)

            assertEquals(winsOnly, viewModel.uiState.value.filter)
            assertEquals(1, viewModel.uiState.value.matches.size)
        }

    @Test
    fun `clearing one filter keeps every other filter`() =
        runTest {
            val viewModel = subscribedViewModel(listOf(match(day = 1)))
            viewModel.changeFilter(
                FilterState(format = MatchFormat.SINGLES, result = MatchResult.WIN, location = "BGC"),
            )

            viewModel.clearFilter(FilterKind.RESULT)

            assertEquals(FilterState(format = MatchFormat.SINGLES, location = "BGC"), viewModel.uiState.value.filter)
        }

    @Test
    fun `clear all removes every filter and leaves nothing in saved state`() =
        runTest {
            val viewModel = subscribedViewModel(listOf(match(day = 1)))
            viewModel.changeFilter(FilterState(format = MatchFormat.SINGLES, result = MatchResult.WIN))

            viewModel.clearAllFilters()

            assertEquals(FilterState.NONE, viewModel.uiState.value.filter)
            assertNull(handle.get<String>(FILTER_STATE_KEY))
        }

    @Test
    fun `a fresh launch opens with no filter and no search`() =
        runTest {
            val state = subscribedViewModel(listOf(match(day = 1))).uiState.value

            assertEquals(FilterState.NONE, state.filter)
            assertNull(state.appliedSearch)
            assertEquals(
                ListPageRequest(MatchSort.DATE_NEWEST, MATCH_LIST_PAGE_SIZE, FilterState.NONE, null),
                matches.requestedQueries.single(),
            )
        }

    @Test
    fun `filters and search restored after process death are applied again`() =
        runTest {
            val ana = person("Ana")
            val filter = FilterState(opponentId = ana.id, format = MatchFormat.SINGLES)
            handle = SavedStateHandle(mapOf(FILTER_STATE_KEY to encodeFilterState(filter), SEARCH_TEXT_KEY to "dink"))
            val wanted = match(day = 2, format = MatchFormat.SINGLES, opponents = listOf(ana), notes = "good dinks")
            val stored = listOf(wanted, match(day = 3, format = MatchFormat.SINGLES, opponents = listOf(ana)))

            val viewModel = subscribedViewModel(stored, knownPeople = listOf(ana))
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)

            val state = viewModel.uiState.value
            assertEquals(filter, state.filter)
            assertEquals("dink", state.appliedSearch)
            assertEquals(listOf(wanted.id.toString()), state.matches.map { it.id })
            assertEquals("Ana", state.filteredOpponentName)
        }

    @Test
    fun `typing a search issues one query after the pause rather than one per keystroke`() =
        runTest {
            val viewModel = subscribedViewModel(listOf(match(day = 1, notes = "ana was great")))

            listOf("a", "an", "ana").forEach { text ->
                viewModel.changeSearch(text)
                advanceTimeBy(SEARCH_DEBOUNCE_MILLIS / 3)
            }
            assertEquals(listOf<SearchTerm?>(null), matches.requestedQueries.map { it.search })

            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS)

            assertEquals(listOf(null, SearchTerm.of("ana")), matches.requestedQueries.map { it.search })
            assertEquals("ana", viewModel.uiState.value.appliedSearch)
        }

    @Test
    fun `clearing the search applies at once without waiting for the pause`() =
        runTest {
            val viewModel = subscribedViewModel(listOf(match(day = 1, notes = "dink"), match(day = 2)))
            viewModel.changeSearch("dink")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            assertEquals(1, viewModel.uiState.value.matches.size)

            viewModel.changeSearch("")

            assertNull(viewModel.uiState.value.appliedSearch)
            assertEquals(2, viewModel.uiState.value.matches.size)
        }

    @Test
    fun `search results respect the active filter and sort`() =
        runTest {
            val oldWin = match(day = 1, notes = "windy")
            val newWin = match(day = 5, notes = "windy again")
            val loss = match(day = 3, result = MatchResult.LOSS, notes = "windy")
            val viewModel = subscribedViewModel(listOf(oldWin, newWin, loss, match(day = 4)))
            viewModel.changeFilter(FilterState(result = MatchResult.WIN))
            viewModel.selectSort(MatchSort.DATE_OLDEST)

            viewModel.changeSearch("WINDY")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)

            assertEquals(
                listOf(oldWin.id.toString(), newWin.id.toString()),
                viewModel.uiState.value.matches.map { it.id },
            )
        }

    @Test
    fun `a filter that matches nothing is a no_results state and not the never_logged empty state`() =
        runTest {
            val viewModel = subscribedViewModel(listOf(match(day = 1, format = MatchFormat.DOUBLES)))

            viewModel.changeFilter(FilterState(format = MatchFormat.SINGLES))

            val state = viewModel.uiState.value
            assertTrue(state.hasNoResults)
            assertFalse(state.isEmpty)
        }

    @Test
    fun `a search that matches nothing is also a no_results state`() =
        runTest {
            val viewModel = subscribedViewModel(listOf(match(day = 1)))

            viewModel.changeSearch("nobody")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)

            assertTrue(viewModel.uiState.value.hasNoResults)
            assertFalse(viewModel.uiState.value.isEmpty)
        }

    @Test
    fun `clearing filters and search together returns the whole list`() =
        runTest {
            val viewModel = subscribedViewModel(listOf(match(day = 1), match(day = 2, result = MatchResult.LOSS)))
            viewModel.changeFilter(FilterState(format = MatchFormat.SINGLES))
            viewModel.changeSearch("nothing")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)

            viewModel.clearFiltersAndSearch()

            val state = viewModel.uiState.value
            assertFalse(state.isNarrowed)
            assertEquals(2, state.matches.size)
        }

    @Test
    fun `rows carry the partner and location for the list to show`() =
        runTest {
            val cy = person("Cy")
            val state =
                subscribedViewModel(
                    listOf(match(day = 1, partner = cy, location = "Rec Center"), match(day = 2)),
                    knownPeople = listOf(cy),
                ).uiState.value

            val withDetails = state.matches.last()
            assertEquals("Cy", withDetails.partnerName)
            assertEquals("Rec Center", withDetails.location)
            assertNull(state.matches.first().partnerName)
            assertNull(state.matches.first().location)
        }

    @Test
    fun `the counts give the whole log and how many of it the filter leaves`() =
        runTest {
            val viewModel =
                subscribedViewModel(
                    listOf(
                        match(day = 1, format = MatchFormat.SINGLES),
                        match(day = 2, format = MatchFormat.DOUBLES),
                        match(day = 3, format = MatchFormat.DOUBLES),
                    ),
                )
            assertEquals(3, viewModel.uiState.value.totalCount)
            assertEquals(3, viewModel.uiState.value.resultCount)

            viewModel.changeFilter(FilterState(format = MatchFormat.SINGLES))

            assertEquals(3, viewModel.uiState.value.totalCount)
            assertEquals(1, viewModel.uiState.value.resultCount)
        }

    @Test
    fun `opponent choices come from the person list and locations from prior matches in a to z order`() =
        runTest {
            val ana = person("Ana")
            val ben = person("Ben")
            val stored =
                listOf(
                    match(day = 1, location = "bgc"),
                    match(day = 2, location = "Ayala"),
                    match(day = 3, location = "Alabang"),
                )

            val state = subscribedViewModel(stored, knownPeople = listOf(ana, ben)).uiState.value

            assertEquals(listOf(OpponentChoice(ana.id, "Ana"), OpponentChoice(ben.id, "Ben")), state.opponentChoices)
            assertEquals(listOf("Alabang", "Ayala", "bgc"), state.locationChoices)
        }

    @Test
    fun `thirty nine matches show no cap warning`() =
        runTest {
            val viewModel = subscribedViewModel(List(39) { match(it) })

            assertEquals(CapWarning.NONE, viewModel.uiState.value.capWarning)
        }

    @Test
    fun `forty matches show that ten remain and dismissing hides it`() =
        runTest {
            val viewModel = subscribedViewModel(List(40) { match(it) })

            assertEquals(CapWarning.APPROACHING, viewModel.uiState.value.capWarning)
            assertEquals(10, viewModel.uiState.value.remainingFreeMatches)

            viewModel.dismissCapWarning()

            assertEquals(CapWarning.NONE, viewModel.uiState.value.capWarning)
        }

    @Test
    fun `a dismissed first warning comes back more prominent at forty eight`() =
        runTest {
            val viewModel = subscribedViewModel(List(47) { match(it) })
            viewModel.dismissCapWarning()

            matches.saveMatch(match(100))

            assertEquals(CapWarning.IMMINENT, viewModel.uiState.value.capWarning)
            assertEquals(2, viewModel.uiState.value.remainingFreeMatches)
        }

    @Test
    fun `pro never sees the cap warning`() =
        runTest {
            val viewModel =
                subscribedViewModel(List(49) { match(it) }, entitlements = FakeEntitlementRepository(isPro = true))

            assertEquals(CapWarning.NONE, viewModel.uiState.value.capWarning)
        }
}
