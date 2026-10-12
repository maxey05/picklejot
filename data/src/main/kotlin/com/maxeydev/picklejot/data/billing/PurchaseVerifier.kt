package com.maxeydev.picklejot.data.billing

object PurchaseVerifier {
    fun isVerifiedPro(purchase: PlayPurchase): Boolean =
        purchase.isPro && purchase.state == PlayPurchaseState.PURCHASED && purchase.purchaseToken.isNotBlank()
}
