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
    Entitlement(isPro = true, purchaseToken = "token-1", lastVerifiedAt = Instant.parse("2026-09-27T00:00:00Z"))

// Verified long enough ago that the stored purchase is due to be asked about again.
private val STALE_PRO =
    PRO.copy(lastVerifiedAt = Instant.parse("2026-01-01T00:00:00Z"), proSince = Instant.parse("2025-12-01T00:00:00Z"))

class PlayEntitlementRepositoryTest {
    private val gateway = FakeBillingGateway()
    private val clock =
        object : Clock {
            override fun now(): Instant = NOW
        }

    private val validator = FakePurchaseValidator()

    private fun repository(local: InMemoryEntitlementRepository): PlayEntitlementRepository =
        PlayEntitlementRepository(local, gateway, PurchaseAcknowledger(gateway, Backoff(emptyList())), validator, clock)

    @Test
    fun `a purchase Google confirms is granted and acknowledged even when no integrity token can be obtained`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            val keys = java.security.KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
            val server =
                ServerPurchaseValidator(
                    config =
                        LicenseServerConfig(
                            "https://verify.example",
                            java.util.Base64.getEncoder().encodeToString(keys.public.encoded),
                        ),
                    packageName = "com.maxeydev.picklejot",
                    integrity = IntegrityTokenProvider { throw IllegalStateException("Play Services outdated") },
                    post = { _, body ->
                        val nonce =
                            kotlinx.serialization.json.Json
                                .parseToJsonElement(body)
                                .let { it as kotlinx.serialization.json.JsonObject }
                                .getValue("clientNonce")
                                .let { (it as kotlinx.serialization.json.JsonPrimitive).content }
                        val claims =
                            """{"status":"granted","purchaseToken":"token-1","clientNonce":"$nonce",""" +
                                """"productId":"$PRO_PRODUCT_ID","packageName":"com.maxeydev.picklejot"}"""
                        val payload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(claims.toByteArray())
                        val sig =
                            java.security.Signature.getInstance("SHA256withECDSA").run {
                                initSign(keys.private)
                                update(payload.toByteArray())
                                sign()
                            }
                        val sigText = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(sig)
                        HttpReply(200, """{"payload":"$payload","sig":"$sigText"}""")
                    },
                )
            val repository =
                PlayEntitlementRepository(
                    local,
                    gateway,
                    PurchaseAcknowledger(gateway, Backoff(emptyList())),
                    server,
                    clock,
                )
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase())))

            assertEquals(SyncOutcome.Granted(isAcknowledged = true), repository.syncWithPlay())
            assertTrue(local.current.isPro)
            assertEquals(listOf("token-1"), gateway.acknowledged)
        }

    @Test
    fun `a purchase the verifier does not confirm is not granted or acknowledged`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            validator.result = ValidationResult.REJECTED
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase())))

            assertEquals(SyncOutcome.Rejected, repository(local).syncWithPlay())
            assertFalse(local.current.isPro)
            assertTrue(gateway.acknowledged.isEmpty())
        }

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

    @Test
    fun `a purchase the verifier rejects grants nothing and is not acknowledged`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            validator.result = ValidationResult.REJECTED
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase())))

            assertEquals(SyncOutcome.Rejected, repository(local).syncWithPlay())
            assertFalse(local.current.isPro)
            assertTrue(gateway.acknowledged.isEmpty())
        }

    @Test
    fun `a pending purchase with an unreachable verifier is not acknowledged`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            validator.result = ValidationResult.UNREACHABLE
            gateway.purchasesResults =
                mutableListOf(GatewayResult.Success(listOf(proPurchase(state = PlayPurchaseState.PENDING))))

            assertEquals(SyncOutcome.Pending, repository(local).syncWithPlay())
            assertTrue(gateway.acknowledged.isEmpty())
        }

    @Test
    fun `an unreachable verifier grants nothing but the purchase is acknowledged and a later verification unlocks pro`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            val repository = repository(local)
            validator.result = ValidationResult.UNREACHABLE
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase())))

            assertEquals(SyncOutcome.CouldNotCheck(BillingResponse.NETWORK_ERROR), repository.syncWithPlay())
            assertFalse(local.current.isPro)
            assertEquals(listOf("token-1"), gateway.acknowledged)
            assertEquals(EntitlementSignal.CheckUnavailable, local.signals.single())

            validator.result = ValidationResult.VALID
            assertEquals(SyncOutcome.Granted(isAcknowledged = true), repository.syncWithPlay())
            assertTrue(local.current.isPro)
        }

    @Test
    fun `a rejected or unreachable verifier never revokes an existing pro`() =
        runTest {
            ValidationResult.entries.filter { it != ValidationResult.VALID }.forEach { result ->
                val local = InMemoryEntitlementRepository(PRO)
                validator.result = result
                gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase(token = "token-2"))))

                repository(local).syncWithPlay()

                assertEquals("after $result", PRO, local.current)
            }
        }

    @Test
    fun `a token already verified is not sent to the verifier again on every launch`() =
        runTest {
            val local = InMemoryEntitlementRepository(PRO)
            validator.result = ValidationResult.REJECTED
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase(token = "token-1"))))
            gateway.acknowledgeResponses = mutableListOf(BillingResponse.OK)

            assertEquals(SyncOutcome.Granted(isAcknowledged = true), repository(local).syncWithPlay())
            assertTrue(validator.validated.isEmpty())
        }

    @Test
    fun `a new token is always verified even when pro is already cached`() =
        runTest {
            val local = InMemoryEntitlementRepository(PRO)
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase(token = "token-2"))))
            gateway.acknowledgeResponses = mutableListOf(BillingResponse.OK)

            repository(local).syncWithPlay()

            assertEquals(listOf("token-2"), validator.validated)
            assertEquals("token-2", local.current.purchaseToken)
        }

    @Test
    fun `an explicit refund verdict is recorded and grants nothing`() =
        runTest {
            val local = InMemoryEntitlementRepository()
            validator.result = ValidationResult.REFUNDED
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(listOf(proPurchase(token = "token-9"))))

            assertEquals(SyncOutcome.NothingFound, repository(local).syncWithPlay())
            assertFalse(local.current.isPro)
            assertTrue(local.signals.contains(EntitlementSignal.RefundConfirmed("token-9")))
        }

    @Test
    fun `a stale cached purchase that Play no longer lists is revoked only by an explicit refund verdict`() =
        runTest {
            val local = InMemoryEntitlementRepository(STALE_PRO)
            validator.result = ValidationResult.REFUNDED
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(emptyList()))

            assertEquals(SyncOutcome.NothingFound, repository(local).syncWithPlay())

            assertEquals(listOf("token-1"), validator.validated)
            assertFalse(local.current.isPro)
        }

    @Test
    fun `a stale cached purchase that is still valid gets a fresh stamp and keeps its original purchase date`() =
        runTest {
            val local = InMemoryEntitlementRepository(STALE_PRO)
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(emptyList()))

            repository(local).syncWithPlay()

            assertTrue(local.current.isPro)
            assertEquals(NOW, local.current.lastVerifiedAt)
            assertEquals(STALE_PRO.proSince, local.current.proSince)
        }

    @Test
    fun `a stale recheck that is rejected or unreachable changes nothing and is not reported to the user`() =
        runTest {
            listOf(ValidationResult.REJECTED, ValidationResult.UNREACHABLE).forEach { result ->
                val local = InMemoryEntitlementRepository(STALE_PRO)
                validator.result = result
                gateway.purchasesResults = mutableListOf(GatewayResult.Success(emptyList()))

                assertEquals(SyncOutcome.NothingFound, repository(local).syncWithPlay())
                assertEquals("after $result", STALE_PRO, local.current)
            }
        }

    @Test
    fun `a recently verified purchase is not rechecked`() =
        runTest {
            val local = InMemoryEntitlementRepository(PRO)
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(emptyList()))

            repository(local).syncWithPlay()

            assertTrue(validator.validated.isEmpty())
        }

    @Test
    fun `the recheck runs once per process however often play is synced`() =
        runTest {
            val local = InMemoryEntitlementRepository(STALE_PRO)
            validator.result = ValidationResult.UNREACHABLE
            gateway.purchasesResults = mutableListOf(GatewayResult.Success(emptyList()))
            val repository = repository(local)

            repeat(3) { repository.syncWithPlay() }

            assertEquals(1, validator.validated.size)
        }
}
