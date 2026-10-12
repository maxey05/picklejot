@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.maxeydev.picklejot.ui.match.edit

import androidx.lifecycle.SavedStateHandle
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.entitlement.CanAddMatch
import com.maxeydev.picklejot.domain.entitlement.CanAddPhoto
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.ui.fakes.FakeEntitlementRepository
import com.maxeydev.picklejot.ui.fakes.FakeLastUsedFormatStore
import com.maxeydev.picklejot.ui.fakes.FakeMatchRepository
import com.maxeydev.picklejot.ui.fakes.FakePersonRepository
import com.maxeydev.picklejot.ui.fakes.FakePhotoImportQueue
import com.maxeydev.picklejot.ui.fakes.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PAST_DEBOUNCE = SUGGESTION_DEBOUNCE_MILLIS + 50

class MatchEditSuggestionsTest {
    private val proEntitlement = FakeEntitlementRepository(isPro = true)

    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = FixedClock(Instant.parse("2026-09-24T12:30:45Z"))
    private lateinit var matches: FakeMatchRepository
    private lateinit var people: FakePersonRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        matches = FakeMatchRepository()
        people = FakePersonRepository(matchSource = matches)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): MatchEditViewModel =
        MatchEditViewModel(
            savedStateHandle = SavedStateHandle(),
            matchRepository = matches,
            personRepository = people,
            lastUsedFormatStore = FakeLastUsedFormatStore(),
            photoImportQueue = FakePhotoImportQueue(),
            canAddMatch = CanAddMatch(matches, proEntitlement),
            canAddPhoto = CanAddPhoto(proEntitlement),
            photoFile = { File("/files", it) },
            clock = clock,
            timeZone = { TimeZone.of("Asia/Manila") },
            defaultDispatcher = dispatcher,
        )

    private suspend fun history(vararg played: Pair<String, List<String>>): List<Person> {
        played.forEachIndexed { index, (date, names) ->
            val roster = names.map { people.findOrCreatePerson(it) }
            matches.saveMatch(
                Match(
                    id = Uuid.random(),
                    format = MatchFormat.DOUBLES,
                    date = AppDate.parse(date),
                    result = MatchResult.WIN,
                    createdAt = AppInstant.fromEpochMilliseconds(index.toLong()),
                    updatedAt = AppInstant.fromEpochMilliseconds(index.toLong()),
                    opponents = roster.take(2),
                    partner = roster.getOrNull(2),
                    location = "Court $index",
                ),
            )
        }
        people.findOrCreateRequests.clear()
        matches.saved.clear()
        return people.current
    }

    private fun TestScope.type(
        viewModel: MatchEditViewModel,
        slot: PersonSlot,
        text: String,
    ) {
        viewModel.focusSuggestionTarget(SuggestionTarget.forSlot(slot))
        viewModel.changePersonName(slot, text)
        advanceTimeBy(PAST_DEBOUNCE)
    }

    private fun MatchEditViewModel.labels(): List<String> = uiState.value.suggestions.map { it.label }

    @Test
    fun `with an empty database a focused field shows no suggestion list`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            type(viewModel, PersonSlot.OPPONENT_1, "a")

            assertTrue(viewModel.uiState.value.suggestions.isEmpty())
        }

    @Test
    fun `typing R suggests Dave R with the most recently played first`() =
        runTest(dispatcher) {
            history("2026-09-01" to listOf("Rosa"), "2026-09-10" to listOf("Dave R."), "2026-08-01" to listOf("Ana"))
            val viewModel = viewModel()

            type(viewModel, PersonSlot.OPPONENT_1, "r")

            assertEquals(SuggestionTarget.OPPONENT_1, viewModel.uiState.value.suggestionTarget)
            assertEquals(listOf("Dave R.", "Rosa"), viewModel.labels())
        }

    @Test
    fun `suggestions wait for typing to pause`() =
        runTest(dispatcher) {
            history("2026-09-01" to listOf("Dave R."))
            val viewModel = viewModel()
            viewModel.focusSuggestionTarget(SuggestionTarget.OPPONENT_1)

            viewModel.changePersonName(PersonSlot.OPPONENT_1, "d")
            advanceTimeBy(SUGGESTION_DEBOUNCE_MILLIS / 2)
            viewModel.changePersonName(PersonSlot.OPPONENT_1, "da")
            advanceTimeBy(SUGGESTION_DEBOUNCE_MILLIS / 2)
            assertTrue(viewModel.labels().isEmpty())

            advanceTimeBy(PAST_DEBOUNCE)
            assertEquals(listOf("Dave R."), viewModel.labels())
        }

    @Test
    fun `the people query runs once per focus rather than once per keystroke`() =
        runTest(dispatcher) {
            history("2026-09-01" to listOf("Dave R."))
            val viewModel = viewModel()
            viewModel.focusSuggestionTarget(SuggestionTarget.OPPONENT_1)

            listOf("d", "da", "dav", "dave").forEach { text ->
                viewModel.changePersonName(PersonSlot.OPPONENT_1, text)
                advanceTimeBy(PAST_DEBOUNCE)
            }

            assertEquals(1, people.recentlyUsedSubscriptions)
        }

    @Test
    fun `selecting a suggestion binds that person so two matches share one person row`() =
        runTest(dispatcher) {
            val dave = history("2026-09-01" to listOf("Dave R.")).single()

            repeat(2) {
                val viewModel = viewModel()
                type(viewModel, PersonSlot.OPPONENT_1, "da")
                viewModel.selectSuggestion(SuggestionTarget.OPPONENT_1, viewModel.uiState.value.suggestions.single())
                assertTrue(viewModel.uiState.value.suggestions.isEmpty())
                viewModel.selectResult(MatchResult.WIN)
                viewModel.save()
            }

            assertEquals(listOf(dave.id, dave.id), matches.saved.map { it.opponents.single().id })
            assertTrue(people.findOrCreateRequests.isEmpty())
            assertEquals(1, people.current.size)
        }

    @Test
    fun `editing a selected name unbinds it and a brand-new name is saved without any confirmation`() =
        runTest(dispatcher) {
            history("2026-09-01" to listOf("Dave R."))
            val viewModel = viewModel()
            type(viewModel, PersonSlot.OPPONENT_1, "da")
            viewModel.selectSuggestion(SuggestionTarget.OPPONENT_1, viewModel.uiState.value.suggestions.single())

            viewModel.changePersonName(PersonSlot.OPPONENT_1, "Dave S.")
            viewModel.selectResult(MatchResult.WIN)
            viewModel.save()

            assertEquals("Dave S.", matches.saved.single().opponents.single().displayName)
            assertEquals(listOf("Dave S."), people.findOrCreateRequests)
            assertEquals(2, people.current.size)
        }

    @Test
    fun `a person already chosen for another slot is not suggested again`() =
        runTest(dispatcher) {
            history("2026-09-01" to listOf("Ben A.", "Ben B."))
            val viewModel = viewModel()
            type(viewModel, PersonSlot.OPPONENT_1, "ben")
            val benB = viewModel.uiState.value.suggestions.first { it.label == "Ben B." }
            viewModel.selectSuggestion(SuggestionTarget.OPPONENT_1, benB)
            viewModel.leaveSuggestionTarget(SuggestionTarget.OPPONENT_1)

            type(viewModel, PersonSlot.PARTNER, "ben")

            assertEquals(listOf("Ben A."), viewModel.labels())
        }

    @Test
    fun `location suggests prior values and choosing one fills the field and hides the list`() =
        runTest(dispatcher) {
            history("2026-09-01" to listOf("Ana"), "2026-09-02" to listOf("Ben"))
            val viewModel = viewModel()
            viewModel.focusSuggestionTarget(SuggestionTarget.LOCATION)
            viewModel.changeLocation("court")
            advanceTimeBy(PAST_DEBOUNCE)
            assertEquals(listOf("Court 1", "Court 0"), viewModel.labels())

            viewModel.selectSuggestion(SuggestionTarget.LOCATION, viewModel.uiState.value.suggestions.last())
            advanceTimeBy(PAST_DEBOUNCE)

            assertEquals("Court 0", viewModel.uiState.value.location)
            assertTrue(viewModel.uiState.value.suggestions.isEmpty())
        }

    @Test
    fun `leaving the field hides its suggestions`() =
        runTest(dispatcher) {
            history("2026-09-01" to listOf("Dave R."))
            val viewModel = viewModel()
            type(viewModel, PersonSlot.OPPONENT_1, "d")
            assertEquals(listOf("Dave R."), viewModel.labels())

            viewModel.leaveSuggestionTarget(SuggestionTarget.OPPONENT_1)

            assertNull(viewModel.uiState.value.suggestionTarget)
            assertTrue(viewModel.uiState.value.suggestions.isEmpty())
        }

    @Test
    fun `suggestions only appear under the focused field`() =
        runTest(dispatcher) {
            history("2026-09-01" to listOf("Dave R."))
            val viewModel = viewModel()

            type(viewModel, PersonSlot.OPPONENT_1, "d")

            val state = viewModel.uiState.value
            assertEquals(listOf("Dave R."), state.suggestionsFor(SuggestionTarget.OPPONENT_1).map { it.label })
            assertTrue(state.suggestionsFor(SuggestionTarget.PARTNER).isEmpty())
        }
}
