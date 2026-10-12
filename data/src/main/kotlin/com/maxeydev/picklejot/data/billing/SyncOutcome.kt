package com.maxeydev.picklejot.data.billing

sealed interface SyncOutcome {
    data class Granted(
        val isAcknowledged: Boolean,
    ) : SyncOutcome

    data object Pending : SyncOutcome

    data object NothingFound : SyncOutcome

    data class CouldNotCheck(
        val response: BillingResponse,
    ) : SyncOutcome
}
