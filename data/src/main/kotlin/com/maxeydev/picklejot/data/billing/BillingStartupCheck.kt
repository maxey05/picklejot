package com.maxeydev.picklejot.data.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BillingStartupCheck(
    private val repository: PlayEntitlementRepository,
    private val gateway: BillingGateway,
    private val scope: CoroutineScope,
    private val acknowledgementRetryDelaysMillis: List<Long> = DEFAULT_ACK_RETRY_DELAYS_MILLIS,
) {
    fun start(): Job {
        gateway.onUnsolicitedPurchases { purchases ->
            scope.launch { repository.process(purchases) }
        }
        return scope.launch { syncUntilAcknowledged() }
    }

    private suspend fun syncUntilAcknowledged() {
        var outcome = repository.syncWithPlay()
        for (pause in acknowledgementRetryDelaysMillis) {
            // Also retry while the verifier or Play could not be reached, so verification (and the
            // acknowledgement, which a down server must not block) is attempted again later.
            val done = if (outcome is SyncOutcome.Granted) outcome.isAcknowledged else outcome !is SyncOutcome.CouldNotCheck
            if (done) {
                return
            }
            delay(pause)
            outcome = repository.syncWithPlay()
        }
    }

    companion object {
        val DEFAULT_ACK_RETRY_DELAYS_MILLIS: List<Long> = listOf(60_000L, 5 * 60_000L, 30 * 60_000L)
    }
}
