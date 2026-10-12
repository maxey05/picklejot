package com.maxeydev.picklejot.data.billing

import android.app.Activity

interface BillingGateway {
    suspend fun queryPurchases(): GatewayResult<List<PlayPurchase>>

    suspend fun acknowledge(purchaseToken: String): BillingResponse

    suspend fun proPrice(): GatewayResult<String>

    suspend fun launchProPurchase(activity: Activity): GatewayResult<List<PlayPurchase>>

    fun onUnsolicitedPurchases(listener: (List<PlayPurchase>) -> Unit)
}
