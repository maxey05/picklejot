package com.maxeydev.picklejot.data.billing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class BillingStartupCheckTest {
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

    @Test
    fun `every app start restores and acknowledges a purchase left unacknowledged by a force quit`() =
        runTest {
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase())))
            gateway.acknowledgeResponses = mutableListOf(BillingResponse.OK)

            BillingStartupCheck(repository, gateway, this, listOf(1_000L)).start().join()

            assertTrue(local.current.isPro)
            assertEquals(listOf("token-1"), gateway.acknowledged)
            assertEquals(1, gateway.queries)
        }

    @Test
    fun `an acknowledgement that still fails is tried again later in the session`() =
        runTest {
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase())))
            gateway.acknowledgeResponses = mutableListOf(BillingResponse.SERVICE_UNAVAILABLE, BillingResponse.OK)

            BillingStartupCheck(repository, gateway, this, listOf(1_000L, 2_000L)).start().join()

            assertEquals(2, gateway.queries)
            assertEquals(2, gateway.acknowledged.size)
        }

    @Test
    fun `purchases that arrive outside a purchase flow are still granted`() =
        runTest {
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(emptyList()))
            gateway.acknowledgeResponses = mutableListOf(BillingResponse.OK)
            BillingStartupCheck(repository, gateway, this, emptyList()).start().join()

            requireNotNull(gateway.unsolicited).invoke(listOf(proPurchase(token = "late")))
            advanceUntilIdle()

            assertTrue(local.current.isPro)
            assertEquals(listOf("late"), gateway.acknowledged)
        }
}
