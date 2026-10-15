package com.maxeydev.picklejot.data.billing

class PurchaseAcknowledger(
    private val gateway: BillingGateway,
    private val backoff: Backoff = Backoff(),
) {
    suspend fun acknowledgeIfNeeded(purchase: PlayPurchase): BillingResponse {
        if (purchase.isAcknowledged || !PurchaseVerifier.isVerifiedPro(purchase)) {
            return BillingResponse.OK
        }
        val response = backoff.retryingResponse { gateway.acknowledge(purchase.purchaseToken) }
        // Already acknowledged elsewhere (another process or a retry that landed late) is the goal state.
        return if (response == BillingResponse.ITEM_ALREADY_OWNED) BillingResponse.OK else response
    }
}
