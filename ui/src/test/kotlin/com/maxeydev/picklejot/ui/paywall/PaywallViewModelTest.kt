@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.maxeydev.picklejot.ui.paywall

import com.maxeydev.picklejot.domain.billing.PriceResult
import com.maxeydev.picklejot.domain.billing.PurchaseOutcome
import com.maxeydev.picklejot.domain.billing.RestoreOutcome
import com.maxeydev.picklejot.domain.billing.StoreProblem
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.streak.StreakEngine
import com.maxeydev.picklejot.ui.fakes.FakeEntitlementRepository
import com.maxeydev.picklejot.ui.fakes.FakeMatchRepository
import com.maxeydev.picklejot.ui.fakes.FakeProStore
import com.maxeydev.picklejot.ui.fakes.FixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class PaywallViewModelTest {
    private val entitlements = FakeEntitlementRepository()
    private val store = FakeProStore(entitlements)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun match(
        date: String,
        result: MatchResult,
    ): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.SINGLES,
            date = AppDate.parse(date),
            result = result,
            createdAt = AppInstant.fromEpochMilliseconds(0),
            updatedAt = AppInstant.fromEpochMilliseconds(0),
        )

    private fun viewModel(): PaywallViewModel =
        PaywallViewModel(
            matchRepository =
                FakeMatchRepository(
                    listOf(
                        match("2026-02-17", MatchResult.WIN),
                        match("2026-02-24", MatchResult.WIN),
                        match("2026-03-02", MatchResult.LOSS),
                    ),
                ),
            streakEngine = StreakEngine(FixedClock(AppInstant.parse("2026-03-04T12:00:00Z"))) { TimeZone.UTC },
            proStore = store,
            entitlementRepository = entitlements,
        )

    @Test
    fun `the paywall shows the user's own record and streak and the price from play`() {
        val state = viewModel().uiState.value

        assertEquals(2, state.wins)
        assertEquals(1, state.losses)
        assertEquals(3, state.streakWeeks)
        assertEquals("$6.99", state.price)
        assertTrue(state.canBuy)
    }

    @Test
    fun `buying unlocks and closes the paywall`() {
        val viewModel = viewModel()

        viewModel.buy()

        assertTrue(viewModel.uiState.value.isUnlocked)
        assertTrue(entitlements.current.isPro)
    }

    @Test
    fun `cancelling shows no error at all`() {
        store.purchaseOutcome = PurchaseOutcome.Canceled
        val viewModel = viewModel()

        viewModel.buy()

        assertNull(viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isUnlocked)
        assertTrue(viewModel.uiState.value.canBuy)
    }

    @Test
    fun `billing unavailable is explained without blaming the user`() {
        store.purchaseOutcome = PurchaseOutcome.Failed(StoreProblem.BILLING_UNAVAILABLE)
        val viewModel = viewModel()

        viewModel.buy()

        assertEquals(StoreMessage.BILLING_UNAVAILABLE, viewModel.uiState.value.message)
    }

    @Test
    fun `a pending purchase says it is pending`() {
        store.purchaseOutcome = PurchaseOutcome.Pending
        val viewModel = viewModel()

        viewModel.buy()

        assertEquals(StoreMessage.PENDING, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isUnlocked)
    }

    @Test
    fun `no price from play means no buy button rather than a made up price`() {
        store.price = PriceResult.Unavailable(StoreProblem.OFFLINE)

        val state = viewModel().uiState.value

        assertNull(state.price)
        assertFalse(state.canBuy)
        assertEquals(StoreMessage.OFFLINE, state.message)
    }

    @Test
    fun `restoring from the paywall unlocks when a purchase exists`() {
        store.restoreOutcome = RestoreOutcome.Restored
        val viewModel = viewModel()

        viewModel.restore()

        assertTrue(viewModel.uiState.value.isUnlocked)
    }

    @Test
    fun `every store problem has its own non blaming message`() {
        StoreProblem.entries.forEach { problem ->
            assertTrue(StoreMessage.of(problem).name.isNotEmpty())
        }
        assertEquals(
            StoreProblem.entries.size,
            StoreProblem.entries.map { StoreMessage.of(it) }.toSet().size,
        )
    }

    @Test
    fun `could not check and nothing to restore are different sentences`() {
        val offline = PaywallViewModel.restoreMessage(RestoreOutcome.CouldNotCheck(StoreProblem.OFFLINE))
        val nothing = PaywallViewModel.restoreMessage(RestoreOutcome.NothingToRestore)

        assertTrue(offline != nothing)
        assertEquals(StoreMessage.NOTHING_TO_RESTORE, nothing)
    }
}
