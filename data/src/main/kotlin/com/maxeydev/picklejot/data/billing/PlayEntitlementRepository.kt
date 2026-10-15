package com.maxeydev.picklejot.data.billing

import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.profile.Entitlement
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class PlayEntitlementRepository(
    private val local: EntitlementRepository,
    private val gateway: BillingGateway,
    private val acknowledger: PurchaseAcknowledger,
    private val validator: PurchaseValidator,
    private val clock: Clock,
) : EntitlementRepository {
    private val processing = Mutex()

    // Once per process: a device that keeps failing the check must not re-run it on every sync.
    private var recheckedThisProcess = false

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
            recheckCachedPurchase()
            val known = local.observeEntitlement().first().takeIf { it.isPro }?.purchaseToken
            val results =
                pro.filter(PurchaseVerifier::isVerifiedPro).associateWith { purchase ->
                    // A token the verifier already vouched for is not re-checked on every launch: that
                    // would spend Play Integrity quota (10k/day) on every pro user's every start.
                    if (purchase.purchaseToken == known) ValidationResult.VALID else validator.validate(purchase)
                }
            val verified = results.filterValues { it == ValidationResult.VALID }.keys
            results.filterValues { it == ValidationResult.REFUNDED }.keys.forEach {
                record(EntitlementSignal.RefundConfirmed(it.purchaseToken))
            }
            // A verifier we could not reach says nothing about the purchase, and Play auto-refunds an
            // unacknowledged one after 3 days: acknowledge it now (PURCHASED only; the acknowledger skips
            // PENDING). Never on REJECTED: that device gets no Pro and should keep the auto-refund.
            // This records nothing; only a VALID verdict unlocks Pro, and the startup check retries the verify.
            results.filterValues { it == ValidationResult.UNREACHABLE }.keys.forEach {
                acknowledger.acknowledgeIfNeeded(it)
            }
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
                ValidationResult.UNREACHABLE in results.values -> {
                    record(EntitlementSignal.CheckUnavailable)
                    SyncOutcome.CouldNotCheck(BillingResponse.NETWORK_ERROR)
                }
                ValidationResult.REJECTED in results.values -> SyncOutcome.Rejected
                else -> {
                    record(EntitlementSignal.NoPurchaseFound)
                    SyncOutcome.NothingFound
                }
            }
        }

    /**
     * Pro is trusted from the cache, but a purchase is re-asked about every [RECHECK_AFTER]: Play stops
     * listing a refunded purchase, so only asking about the stored token can surface a refund. Only an
     * explicit REFUNDED verdict revokes (R58); VALID refreshes the stamp; REJECTED and UNREACHABLE do nothing.
     */
    private suspend fun recheckCachedPurchase() {
        val cached = local.observeEntitlement().first()
        val token = cached.purchaseToken
        val due = cached.lastVerifiedAt?.let { clock.now() - it >= RECHECK_AFTER } ?: true
        if (recheckedThisProcess || !cached.isPro || token == null || !due) return
        recheckedThisProcess = true
        val stored =
            PlayPurchase(
                purchaseToken = token,
                productIds = listOf(PRO_PRODUCT_ID),
                state = PlayPurchaseState.PURCHASED,
                isAcknowledged = true,
                purchasedAtMillis = cached.proSince?.toEpochMilliseconds(),
            )
        when (validator.validate(stored)) {
            ValidationResult.VALID ->
                record(
                    EntitlementSignal.PurchaseVerified(
                        purchaseToken = token,
                        verifiedAt = clock.now(),
                        purchasedAt = cached.proSince ?: clock.now(),
                    ),
                )
            ValidationResult.REFUNDED -> record(EntitlementSignal.RefundConfirmed(token))
            ValidationResult.REJECTED, ValidationResult.UNREACHABLE -> Unit
        }
    }

    private companion object {
        val RECHECK_AFTER = 30.days
    }
}
