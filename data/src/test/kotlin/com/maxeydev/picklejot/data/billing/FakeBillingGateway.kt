package com.maxeydev.picklejot.data.billing

import android.app.Activity

class FakeBillingGateway : BillingGateway {
    var purchasesResults: MutableList<GatewayResult<List<PlayPurchase>>> = mutableListOf()
    var acknowledgeResponses: MutableList<BillingResponse> = mutableListOf()
    var price: GatewayResult<String> = GatewayResult.Success("$6.99")
    val acknowledged = mutableListOf<String>()
    var queries = 0
        private set
    var unsolicited: ((List<PlayPurchase>) -> Unit)? = null
        private set

    override suspend fun queryPurchases(): GatewayResult<List<PlayPurchase>> {
        queries++
        return if (purchasesResults.size > 1) purchasesResults.removeAt(0) else purchasesResults.single()
    }

    override suspend fun acknowledge(purchaseToken: String): BillingResponse {
        acknowledged += purchaseToken
        return if (acknowledgeResponses.size > 1) acknowledgeResponses.removeAt(0) else acknowledgeResponses.single()
    }

    override suspend fun proPrice(): GatewayResult<String> = price

    override suspend fun launchProPurchase(activity: Activity): GatewayResult<List<PlayPurchase>> =
        error("Purchases are launched through the PlayProStore launcher in tests.")

    override fun onUnsolicitedPurchases(listener: (List<PlayPurchase>) -> Unit) {
        unsolicited = listener
    }
}

fun proPurchase(
    token: String = "token-1",
    state: PlayPurchaseState = PlayPurchaseState.PURCHASED,
    isAcknowledged: Boolean = false,
): PlayPurchase = PlayPurchase(token, listOf(PRO_PRODUCT_ID), state, isAcknowledged)
