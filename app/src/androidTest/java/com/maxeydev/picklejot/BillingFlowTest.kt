package com.maxeydev.picklejot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxeydev.picklejot.data.billing.Backoff
import com.maxeydev.picklejot.data.billing.BillingClientWrapper
import com.maxeydev.picklejot.data.billing.BillingResponse
import com.maxeydev.picklejot.data.billing.GatewayResult
import com.maxeydev.picklejot.data.billing.PlayEntitlementRepository
import com.maxeydev.picklejot.data.billing.PurchaseAcknowledger
import com.maxeydev.picklejot.data.billing.SyncOutcome
import com.maxeydev.picklejot.domain.entitlement.EntitlementRules
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class BillingFlowTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private class MemoryEntitlements(
        initial: Entitlement,
    ) : EntitlementRepository {
        val state = MutableStateFlow(initial)

        override fun observeEntitlement(): Flow<Entitlement> = state

        override suspend fun record(signal: EntitlementSignal) {
            state.update { EntitlementRules.next(it, signal) }
        }
    }

    @Test
    fun querying_real_play_is_configured_correctly_and_never_crashes() =
        runBlocking {
            val gateway = BillingClientWrapper(context, Backoff(listOf(500L, 1_000L)))

            val purchases = gateway.queryPurchases()
            val price = gateway.proPrice()

            assertFalse("the billing call was malformed: $purchases", purchases.isDeveloperError())
            assertFalse("the product query was malformed: $price", price.isDeveloperError())
        }

    private fun GatewayResult<*>.isDeveloperError(): Boolean =
        this is GatewayResult.Failure && response == BillingResponse.DEVELOPER_ERROR

    @Test
    fun a_pro_device_stays_pro_whatever_real_play_answers() =
        runBlocking {
            val gateway = BillingClientWrapper(context, Backoff(listOf(500L)))
            val local = MemoryEntitlements(Entitlement(isPro = true, purchaseToken = "installed-before"))
            val repository = PlayEntitlementRepository(local, gateway, PurchaseAcknowledger(gateway), Clock.System)

            val outcome = repository.syncWithPlay()

            assertTrue("outcome $outcome revoked pro", local.state.value.isPro)
            assertTrue(
                outcome is SyncOutcome.Granted ||
                    outcome == SyncOutcome.NothingFound ||
                    outcome == SyncOutcome.Pending ||
                    (outcome is SyncOutcome.CouldNotCheck && outcome.response != BillingResponse.OK),
            )
        }
}
