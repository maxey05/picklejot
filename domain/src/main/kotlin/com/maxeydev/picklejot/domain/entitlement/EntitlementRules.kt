package com.maxeydev.picklejot.domain.entitlement

import com.maxeydev.picklejot.domain.profile.Entitlement

object EntitlementRules {
    fun next(
        current: Entitlement,
        signal: EntitlementSignal,
    ): Entitlement =
        when (signal) {
            is EntitlementSignal.PurchaseVerified ->
                current.copy(
                    isPro = true,
                    purchaseToken = signal.purchaseToken,
                    lastVerifiedAt = signal.verifiedAt,
                    proSince = signal.purchasedAt,
                )
            is EntitlementSignal.RefundConfirmed -> revokedBy(current, signal)
            EntitlementSignal.NoPurchaseFound -> current
            EntitlementSignal.CheckFailed -> current
            EntitlementSignal.CheckUnavailable -> current
        }

    private fun revokedBy(
        current: Entitlement,
        refund: EntitlementSignal.RefundConfirmed,
    ): Entitlement =
        if (current.isPro && current.purchaseToken == refund.purchaseToken) {
            current.copy(isPro = false)
        } else {
            current
        }
}
