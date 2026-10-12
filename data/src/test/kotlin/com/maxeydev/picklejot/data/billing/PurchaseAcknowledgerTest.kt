package com.maxeydev.picklejot.data.billing

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PurchaseAcknowledgerTest {
    private val gateway = FakeBillingGateway()

    @Test
    fun `a transient acknowledgement failure is retried until it succeeds`() =
        runTest {
            gateway.acknowledgeResponses =
                mutableListOf(BillingResponse.SERVICE_DISCONNECTED, BillingResponse.NETWORK_ERROR, BillingResponse.OK)

            val acknowledger = PurchaseAcknowledger(gateway, Backoff(listOf(10L, 20L, 40L)))

            val response = acknowledger.acknowledgeIfNeeded(proPurchase())

            assertEquals(BillingResponse.OK, response)
            assertEquals(3, gateway.acknowledged.size)
        }

    @Test
    fun `a failure that outlasts every retry is returned not swallowed`() =
        runTest {
            gateway.acknowledgeResponses = mutableListOf(BillingResponse.SERVICE_UNAVAILABLE)

            val response = PurchaseAcknowledger(gateway, Backoff(listOf(10L, 20L))).acknowledgeIfNeeded(proPurchase())

            assertEquals(BillingResponse.SERVICE_UNAVAILABLE, response)
            assertEquals(3, gateway.acknowledged.size)
        }

    @Test
    fun `a pending purchase is never acknowledged`() =
        runTest {
            val response =
                PurchaseAcknowledger(gateway, Backoff(emptyList()))
                    .acknowledgeIfNeeded(proPurchase(state = PlayPurchaseState.PENDING))

            assertEquals(BillingResponse.OK, response)
            assertEquals(0, gateway.acknowledged.size)
        }
}
