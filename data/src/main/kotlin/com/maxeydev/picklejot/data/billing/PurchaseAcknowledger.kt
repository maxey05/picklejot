package com.maxeydev.picklejot.data.billing

class PurchaseAcknowledger(
    private val gateway: BillingGateway,
    private val backoff: Backoff = Backoff(),
) {
    suspend fun acknowledgeIfNeeded(purchase: PlayPurchase): BillingResponse {
        if (purchase.isAcknowledged || !PurchaseVerifier.isVerifiedPro(purchase)) {
            return BillingResponse.OK
        }
        return backoff.retryingResponse { gateway.acknowledge(purchase.purchaseToken) }
    }
}
