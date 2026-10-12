package com.maxeydev.picklejot.data.billing

sealed interface GatewayResult<out T> {
    data class Success<T>(
        val value: T,
    ) : GatewayResult<T>

    data class Failure(
        val response: BillingResponse,
    ) : GatewayResult<Nothing>
}
