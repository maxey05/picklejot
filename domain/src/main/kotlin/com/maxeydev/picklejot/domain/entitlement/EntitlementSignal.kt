package com.maxeydev.picklejot.domain.entitlement

import com.maxeydev.picklejot.domain.datetime.AppInstant

sealed interface EntitlementSignal {
    data class PurchaseVerified(
        val purchaseToken: String,
        val verifiedAt: AppInstant,
        val purchasedAt: AppInstant = verifiedAt,
    ) : EntitlementSignal

    data class RefundConfirmed(
        val purchaseToken: String,
    ) : EntitlementSignal

    data object NoPurchaseFound : EntitlementSignal

    data object CheckFailed : EntitlementSignal

    data object CheckUnavailable : EntitlementSignal
}
