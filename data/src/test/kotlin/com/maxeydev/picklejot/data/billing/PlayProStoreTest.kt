package com.maxeydev.picklejot.data.billing

import com.maxeydev.picklejot.domain.billing.PriceResult
import com.maxeydev.picklejot.domain.billing.PurchaseOutcome
import com.maxeydev.picklejot.domain.billing.RestoreOutcome
import com.maxeydev.picklejot.domain.billing.StoreProblem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock

class PlayProStoreTest {
    private val gateway = FakeBillingGateway()
    private val local = InMemoryEntitlementRepository()
    private val repository =
        PlayEntitlementRepository(
            local,
            gateway,
            PurchaseAcknowledger(gateway, Backoff(emptyList())),
            FakePurchaseValidator(),
            Clock.System,
        )

    private fun store(launch: GatewayResult<List<PlayPurchase>>?): PlayProStore =
        PlayProStore(gateway, repository) { launch }

    @Test
    fun `the price comes from play and is never hardcoded`() =
        runTest {
            gateway.price = GatewayResult.Success("₱399.00")

            assertEquals(PriceResult.Available("₱399.00"), store(null).loadPrice())
        }

    @Test
    fun `a completed purchase unlocks and is acknowledged`() =
        runTest {
            gateway.acknowledgeResponses = mutableListOf(BillingResponse.OK)

            val outcome = store(GatewayResult.Success(listOf(proPurchase()))).purchase()

            assertEquals(PurchaseOutcome.Unlocked, outcome)
            assertTrue(local.current.isPro)
            assertEquals(listOf("token-1"), gateway.acknowledged)
        }

    @Test
    fun `cancelling returns quietly with nothing changed`() =
        runTest {
            val outcome = store(GatewayResult.Failure(BillingResponse.USER_CANCELED)).purchase()

            assertEquals(PurchaseOutcome.Canceled, outcome)
            assertFalse(local.current.isPro)
        }

    @Test
    fun `item already owned is treated as success and restores`() =
        runTest {
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase(isAcknowledged = true))))

            val outcome = store(GatewayResult.Failure(BillingResponse.ITEM_ALREADY_OWNED)).purchase()

            assertEquals(PurchaseOutcome.Unlocked, outcome)
            assertTrue(local.current.isPro)
        }

    @Test
    fun `billing unavailable is reported as a device problem without crashing`() =
        runTest {
            val outcome = store(GatewayResult.Failure(BillingResponse.BILLING_UNAVAILABLE)).purchase()

            assertEquals(PurchaseOutcome.Failed(StoreProblem.BILLING_UNAVAILABLE), outcome)
        }

    @Test
    fun `every failing response code maps to an outcome rather than throwing`() =
        runTest {
            BillingResponse.entries.filter { it != BillingResponse.OK }.forEach { response ->
                gateway.purchasesResults = mutableListOf(GatewayResult.Failure(BillingResponse.NETWORK_ERROR))
                val outcome = store(GatewayResult.Failure(response)).purchase()

                assertTrue("$response gave $outcome", outcome != PurchaseOutcome.Unlocked)
                assertFalse(local.current.isPro)
            }
        }

    @Test
    fun `a pending purchase says so instead of unlocking`() =
        runTest {
            val pending = proPurchase(state = PlayPurchaseState.PENDING)

            val outcome = store(GatewayResult.Success(listOf(pending))).purchase()

            assertEquals(PurchaseOutcome.Pending, outcome)
        }

    @Test
    fun `no foreground screen to launch from fails gracefully`() =
        runTest {
            assertEquals(PurchaseOutcome.Failed(StoreProblem.UNEXPECTED), store(null).purchase())
        }

    @Test
    fun `restore distinguishes nothing to restore from could not check`() =
        runTest {
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(emptyList()))
            assertEquals(RestoreOutcome.NothingToRestore, store(null).restore())

            gateway.purchasesResults = mutableListOf(GatewayResult.Failure(BillingResponse.NETWORK_ERROR))
            assertEquals(RestoreOutcome.CouldNotCheck(StoreProblem.OFFLINE), store(null).restore())
        }
}
