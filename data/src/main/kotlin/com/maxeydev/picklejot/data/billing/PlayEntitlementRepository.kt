package com.maxeydev.picklejot.data.billing

import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

class PlayEntitlementRepository(
    private val local: EntitlementRepository,
    private val gateway: BillingGateway,
    private val acknowledger: PurchaseAcknowledger,
    private val clock: Clock,
) : EntitlementRepository {
    private val processing = Mutex()

    override fun observeEntitlement(): Flow<Entitlement> = local.observeEntitlement()

    override suspend fun record(signal: EntitlementSignal) {
        local.record(signal)
    }

    suspend fun syncWithPlay(): SyncOutcome =
        when (val result = gateway.queryPurchases()) {
            is GatewayResult.Failure -> {
                record(
                    if (result.response == BillingResponse.NETWORK_ERROR) {
                        EntitlementSignal.CheckUnavailable
                    } else {
                        EntitlementSignal.CheckFailed
                    },
                )
                SyncOutcome.CouldNotCheck(result.response)
            }
            is GatewayResult.Success -> process(result.value)
        }

    suspend fun process(purchases: List<PlayPurchase>): SyncOutcome =
        processing.withLock {
            val pro = purchases.filter { it.isPro }
            val verified = pro.filter(PurchaseVerifier::isVerifiedPro)
            when {
                verified.isNotEmpty() -> {
                    var allAcknowledged = true
                    verified.forEach { purchase ->
                        val now = clock.now()
                        record(
                            EntitlementSignal.PurchaseVerified(
                                purchaseToken = purchase.purchaseToken,
                                verifiedAt = now,
                                purchasedAt = purchase.purchasedAtMillis?.let(Instant::fromEpochMilliseconds) ?: now,
                            ),
                        )
                        if (acknowledger.acknowledgeIfNeeded(purchase) != BillingResponse.OK) {
                            allAcknowledged = false
                        }
                    }
                    SyncOutcome.Granted(isAcknowledged = allAcknowledged)
                }
                pro.any { it.state == PlayPurchaseState.PENDING } -> SyncOutcome.Pending
                else -> {
                    record(EntitlementSignal.NoPurchaseFound)
                    SyncOutcome.NothingFound
                }
            }
        }
}
