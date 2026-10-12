package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.billing.PriceResult
import com.maxeydev.picklejot.domain.billing.ProStore
import com.maxeydev.picklejot.domain.billing.PurchaseOutcome
import com.maxeydev.picklejot.domain.billing.RestoreOutcome
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal

class FakeProStore(
    private val entitlements: FakeEntitlementRepository,
    var price: PriceResult = PriceResult.Available("$6.99"),
    var purchaseOutcome: PurchaseOutcome = PurchaseOutcome.Unlocked,
    var restoreOutcome: RestoreOutcome = RestoreOutcome.NothingToRestore,
) : ProStore {
    var purchaseAttempts = 0
        private set

    var restoreAttempts = 0
        private set

    override suspend fun loadPrice(): PriceResult = price

    override suspend fun purchase(): PurchaseOutcome {
        purchaseAttempts++
        if (purchaseOutcome == PurchaseOutcome.Unlocked) {
            grant()
        }
        return purchaseOutcome
    }

    override suspend fun restore(): RestoreOutcome {
        restoreAttempts++
        if (restoreOutcome == RestoreOutcome.Restored) {
            grant()
        }
        return restoreOutcome
    }

    private suspend fun grant() {
        val verifiedAt = AppInstant.fromEpochMilliseconds(0)
        val token = FakeEntitlementRepository.FAKE_PURCHASE_TOKEN
        entitlements.record(EntitlementSignal.PurchaseVerified(token, verifiedAt))
    }
}
