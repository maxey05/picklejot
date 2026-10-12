package com.maxeydev.picklejot.data.billing

import kotlinx.coroutines.delay

class Backoff(
    private val delaysMillis: List<Long> = DEFAULT_DELAYS_MILLIS,
) {
    suspend fun <T> retrying(attempt: suspend () -> GatewayResult<T>): GatewayResult<T> {
        var result = attempt()
        for (pause in delaysMillis) {
            if (result !is GatewayResult.Failure || !result.response.isRetryable) {
                return result
            }
            delay(pause)
            result = attempt()
        }
        return result
    }

    suspend fun retryingResponse(attempt: suspend () -> BillingResponse): BillingResponse {
        val result =
            retrying {
                val response = attempt()
                if (response == BillingResponse.OK) GatewayResult.Success(Unit) else GatewayResult.Failure(response)
            }
        return when (result) {
            is GatewayResult.Success -> BillingResponse.OK
            is GatewayResult.Failure -> result.response
        }
    }

    companion object {
        val DEFAULT_DELAYS_MILLIS: List<Long> = listOf(500L, 1_000L, 2_000L, 4_000L, 8_000L)
    }
}
