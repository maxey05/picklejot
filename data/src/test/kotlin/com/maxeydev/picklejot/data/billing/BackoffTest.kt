package com.maxeydev.picklejot.data.billing

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BackoffTest {
    private val backoff = Backoff(listOf(100L, 200L, 400L))

    @Test
    fun `a disconnected service is retried with growing pauses until it answers`() =
        runTest {
            val responses =
                mutableListOf<GatewayResult<Int>>(
                    GatewayResult.Failure(BillingResponse.SERVICE_DISCONNECTED),
                    GatewayResult.Failure(BillingResponse.SERVICE_DISCONNECTED),
                    GatewayResult.Success(7),
                )

            val result = backoff.retrying { responses.removeAt(0) }

            assertEquals(GatewayResult.Success(7), result)
            assertEquals(300L, currentTime)
        }

    @Test
    fun `a non retryable answer is returned at once`() =
        runTest {
            var attempts = 0

            val result =
                backoff.retrying<Int> {
                    attempts++
                    GatewayResult.Failure(BillingResponse.BILLING_UNAVAILABLE)
                }

            assertEquals(GatewayResult.Failure(BillingResponse.BILLING_UNAVAILABLE), result)
            assertEquals(1, attempts)
            assertEquals(0L, currentTime)
        }

    @Test
    fun `after the last pause the final failure is reported rather than swallowed`() =
        runTest {
            var attempts = 0

            val result =
                backoff.retrying<Int> {
                    attempts++
                    GatewayResult.Failure(BillingResponse.NETWORK_ERROR)
                }

            assertEquals(GatewayResult.Failure(BillingResponse.NETWORK_ERROR), result)
            assertEquals(4, attempts)
            assertEquals(700L, currentTime)
        }
}
