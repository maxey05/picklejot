@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.maxeydev.picklejot.ui.match.detail

import androidx.lifecycle.SavedStateHandle
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.ui.fakes.FakeMatchRepository
import com.maxeydev.picklejot.ui.navigation.MATCH_ID_ARGUMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MatchDetailViewModelTest {
    private val match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.SINGLES,
            date = AppDate.parse("2026-09-24"),
            result = MatchResult.WIN,
            createdAt = AppInstant.fromEpochMilliseconds(1_000),
            updatedAt = AppInstant.fromEpochMilliseconds(1_000),
            startTime = AppTime.parse("23:30"),
            endTime = AppTime.parse("01:00"),
        )
    private lateinit var matches: FakeMatchRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        matches = FakeMatchRepository(listOf(match))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.subscribedViewModel(id: Uuid = match.id): MatchDetailViewModel {
        val viewModel = MatchDetailViewModel(SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to id.toString())), matches)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `an existing match loads with a positive duration across midnight`() =
        runTest {
            val state = subscribedViewModel().uiState.value

            assertEquals(match, state.match)
            assertEquals(1.hours + 30.minutes, state.duration)
            assertTrue(state.endsNextDay)
            assertFalse(state.isGone)
        }

    @Test
    fun `the match number is its position in date order`() =
        runTest {
            val earlier = match.copy(id = Uuid.random(), date = AppDate.parse("2026-09-01"))
            val sameDayLater =
                match.copy(
                    id = Uuid.random(),
                    createdAt = AppInstant.fromEpochMilliseconds(2_000),
                )
            val later = match.copy(id = Uuid.random(), date = AppDate.parse("2026-10-01"))
            matches = FakeMatchRepository(listOf(later, match, earlier, sameDayLater))

            assertEquals(2, subscribedViewModel(match.id).uiState.value.matchNumber)
            assertEquals(1, subscribedViewModel(earlier.id).uiState.value.matchNumber)
            assertEquals(3, subscribedViewModel(sameDayLater.id).uiState.value.matchNumber)
            assertEquals(4, subscribedViewModel(later.id).uiState.value.matchNumber)
        }

    @Test
    fun `deleting an earlier match renumbers the later ones`() =
        runTest {
            val earlier = match.copy(id = Uuid.random(), date = AppDate.parse("2026-09-01"))
            matches = FakeMatchRepository(listOf(match, earlier))
            val viewModel = subscribedViewModel(match.id)
            assertEquals(2, viewModel.uiState.value.matchNumber)

            matches.deleteMatch(earlier.id)

            assertEquals(1, viewModel.uiState.value.matchNumber)
        }

    @Test
    fun `requesting delete asks for confirmation and deletes nothing yet`() =
        runTest {
            val viewModel = subscribedViewModel()

            viewModel.requestDelete()

            assertTrue(viewModel.uiState.value.isConfirmingDelete)
            assertTrue(matches.deletedIds.isEmpty())
        }

    @Test
    fun `dismissing the confirmation leaves the match untouched`() =
        runTest {
            val viewModel = subscribedViewModel()

            viewModel.requestDelete()
            viewModel.dismissDelete()

            assertFalse(viewModel.uiState.value.isConfirmingDelete)
            assertTrue(matches.deletedIds.isEmpty())
            assertEquals(listOf(match), matches.current)
        }

    @Test
    fun `confirming deletes the match and reports it gone`() =
        runTest {
            val viewModel = subscribedViewModel()

            viewModel.requestDelete()
            viewModel.confirmDelete()

            assertEquals(listOf(match.id), matches.deletedIds)
            assertTrue(viewModel.uiState.value.isGone)
            assertFalse(viewModel.uiState.value.isConfirmingDelete)
        }

    @Test
    fun `confirming without a pending request deletes nothing`() =
        runTest {
            val viewModel = subscribedViewModel()

            viewModel.confirmDelete()

            assertTrue(matches.deletedIds.isEmpty())
        }

    @Test
    fun `a match that does not exist is reported gone`() =
        runTest {
            assertTrue(subscribedViewModel(Uuid.random()).uiState.value.isGone)
        }
}
