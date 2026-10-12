package com.maxeydev.picklejot.data.billing

const val PRO_PRODUCT_ID = "picklejot_pro"

enum class PlayPurchaseState {
    PURCHASED,
    PENDING,
    UNSPECIFIED,
}

data class PlayPurchase(
    val purchaseToken: String,
    val productIds: List<String>,
    val state: PlayPurchaseState,
    val isAcknowledged: Boolean,
    val purchasedAtMillis: Long? = null,
) {
    val isPro: Boolean
        get() = PRO_PRODUCT_ID in productIds
}
