package com.maxeydev.picklejot.data.billing

import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.profile.Entitlement
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

private val NOW = Instant.parse("2026-09-28T09:00:00Z")

private val PRO =
    Entitlement(isPro = true, purchaseToken = "token-1", lastVerifiedAt = Instant.parse("2026-01-01T00:00:00Z"))

class PlayEntitlementRepositoryTest {
    private val gateway = FakeBillingGateway()
    private val clock =
        object : Clock {
            override fun now(): Instant = NOW
        }

    private fun repository(local: InMemoryEntitlementRepository): PlayEntitlementRepository =
        PlayEntitlementRepository(local, gateway, PurchaseAcknowledger(gateway, Backoff(emptyList())), clock)

    @Test
    fun `no failed purchase query of any kind revokes pro`() =
        runTest {
            BillingResponse.entries.filter { it != BillingResponse.OK }.forEach { response ->
                val local = InMemoryEntitlementRepository(PRO)
                gateway.purchasesResults = mutableListOf(GatewayResult.Failure(response))

                val outcome = repository(local).syncWithPlay()

                assertEquals(SyncOutcome.CouldNotCheck(response), outcome)
                assertEquals("after $response", PRO, local.current)
            }
        }

    @Test
    fun `a successful query that finds nothing still does not revoke pro`() =
        runTest {
            val local = InMemoryEntitlementRepository(PRO)
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(emptyList()))

            assertEquals(SyncOutcome.NothingFound, repository(local).syncWithPlay())
            assertEquals(PRO, local.current)
        }

    @Test
    fun `offline at first launch leaves the free tier in place and reports it as a check problem`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            gateway.purchasesResults = mutableListOf(GatewayResult.Failure(BillingResponse.NETWORK_ERROR))

            val outcome = repository(local).syncWithPlay()

            assertEquals(SyncOutcome.CouldNotCheck(BillingResponse.NETWORK_ERROR), outcome)
            assertEquals(EntitlementSignal.CheckUnavailable, local.signals.single())
            assertFalse(local.current.isPro)
        }

    @Test
    fun `an existing purchase is restored silently and acknowledged`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase())))
            gateway.acknowledgeResponses = mutableListOf(BillingResponse.OK)

            val outcome = repository(local).syncWithPlay()

            assertEquals(SyncOutcome.Granted(isAcknowledged = true), outcome)
            assertTrue(local.current.isPro)
            assertEquals("token-1", local.current.purchaseToken)
            assertEquals(NOW, local.current.lastVerifiedAt)
            assertEquals(listOf("token-1"), gateway.acknowledged)
        }

    @Test
    fun `an already acknowledged purchase is not acknowledged again`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase(isAcknowledged = true))))

            repository(local).syncWithPlay()

            assertTrue(local.current.isPro)
            assertTrue(gateway.acknowledged.isEmpty())
        }

    @Test
    fun `a failed acknowledgement is reported so it can be retried and pro is still granted`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase())))
            gateway.acknowledgeResponses = mutableListOf(BillingResponse.SERVICE_UNAVAILABLE)

            val outcome = repository(local).syncWithPlay()

            assertEquals(SyncOutcome.Granted(isAcknowledged = false), outcome)
            assertTrue(local.current.isPro)
        }

    @Test
    fun `a pending purchase does not grant pro yet`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            gateway.purchasesResults =
                mutableListOf(GatewayResult.Success(listOf(proPurchase(state = PlayPurchaseState.PENDING))))

            assertEquals(SyncOutcome.Pending, repository(local).syncWithPlay())
            assertFalse(local.current.isPro)
            assertTrue(gateway.acknowledged.isEmpty())
        }

    @Test
    fun `a purchase of some other product is ignored`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            val other = PlayPurchase("t", listOf("something_else"), PlayPurchaseState.PURCHASED, false)
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(other)))

            assertEquals(SyncOutcome.NothingFound, repository(local).syncWithPlay())
            assertFalse(local.current.isPro)
        }

    @Test
    fun `the repository never records a signal that could revoke`() =
        runTest {
            val local = InMemoryEntitlementRepository(PRO)
            val repository = repository(local)
            gateway.acknowledgeResponses = mutableListOf(BillingResponse.ERROR)
            listOf<GatewayResult<List<PlayPurchase>>>(
                GatewayResult.Failure(BillingResponse.SERVICE_DISCONNECTED),
                GatewayResult.Success(emptyList()),
                GatewayResult.Success(listOf(proPurchase(state = PlayPurchaseState.PENDING))),
                GatewayResult.Success(listOf(proPurchase(state = PlayPurchaseState.UNSPECIFIED))),
                GatewayResult.Failure(BillingResponse.NETWORK_ERROR),
            ).forEach { result ->
                gateway.purchasesResults = mutableListOf(result)
                repository.syncWithPlay()
            }

            assertTrue(local.current.isPro)
            assertTrue(local.signals.none { it is EntitlementSignal.RefundConfirmed })
        }
}
