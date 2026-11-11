package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.billing.ReviewerAccess
import com.maxeydev.picklejot.domain.billing.ReviewerOutcome
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal

class FakeReviewerAccess(
    private val entitlements: FakeEntitlementRepository,
    var acceptedCode: String = "reviewer-code",
    var unavailable: Boolean = false,
) : ReviewerAccess {
    val attempts: MutableList<String> = mutableListOf()

    override suspend fun redeem(code: String): ReviewerOutcome {
        attempts += code
        return when {
            unavailable -> ReviewerOutcome.Unavailable
            code != acceptedCode -> ReviewerOutcome.WrongCode
            else -> {
                val verifiedAt = AppInstant.fromEpochMilliseconds(0)
                entitlements.record(EntitlementSignal.PurchaseVerified("reviewer-access", verifiedAt))
                ReviewerOutcome.Unlocked
            }
        }
    }
}
