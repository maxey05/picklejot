package com.maxeydev.picklejot.data.billing

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.maxeydev.picklejot.domain.billing.StoreProblem

enum class BillingResponse {
    OK,
    USER_CANCELED,
    ITEM_ALREADY_OWNED,
    SERVICE_DISCONNECTED,
    SERVICE_UNAVAILABLE,
    NETWORK_ERROR,
    BILLING_UNAVAILABLE,
    FEATURE_NOT_SUPPORTED,
    ITEM_UNAVAILABLE,
    ITEM_NOT_OWNED,
    DEVELOPER_ERROR,
    ERROR,
    ;

    val isRetryable: Boolean
        get() = this == SERVICE_DISCONNECTED || this == SERVICE_UNAVAILABLE || this == NETWORK_ERROR || this == ERROR

    val problem: StoreProblem
        get() =
            when (this) {
                NETWORK_ERROR -> StoreProblem.OFFLINE
                SERVICE_DISCONNECTED, SERVICE_UNAVAILABLE -> StoreProblem.PLAY_UNAVAILABLE
                BILLING_UNAVAILABLE, FEATURE_NOT_SUPPORTED -> StoreProblem.BILLING_UNAVAILABLE
                ITEM_UNAVAILABLE -> StoreProblem.PRODUCT_UNAVAILABLE
                OK, USER_CANCELED, ITEM_ALREADY_OWNED, ITEM_NOT_OWNED, DEVELOPER_ERROR, ERROR -> StoreProblem.UNEXPECTED
            }

    companion object {
        fun fromCode(code: Int): BillingResponse =
            when (code) {
                BillingResponseCode.OK -> OK
                BillingResponseCode.USER_CANCELED -> USER_CANCELED
                BillingResponseCode.ITEM_ALREADY_OWNED -> ITEM_ALREADY_OWNED
                BillingResponseCode.SERVICE_DISCONNECTED -> SERVICE_DISCONNECTED
                BillingResponseCode.SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE
                BillingResponseCode.NETWORK_ERROR -> NETWORK_ERROR
                BillingResponseCode.BILLING_UNAVAILABLE -> BILLING_UNAVAILABLE
                BillingResponseCode.FEATURE_NOT_SUPPORTED -> FEATURE_NOT_SUPPORTED
                BillingResponseCode.ITEM_UNAVAILABLE -> ITEM_UNAVAILABLE
                BillingResponseCode.ITEM_NOT_OWNED -> ITEM_NOT_OWNED
                BillingResponseCode.DEVELOPER_ERROR -> DEVELOPER_ERROR
                else -> ERROR
            }
    }
}
