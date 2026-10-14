package com.maxeydev.picklejot.data.billing

sealed interface SyncOutcome {
    data class Granted(
        val isAcknowledged: Boolean,
    ) : SyncOutcome

    data object Pending : SyncOutcome

    data object NothingFound : SyncOutcome

    /** Play reports a purchase but the verifier refused it. Nothing was granted; nothing was revoked. */
    data object Rejected : SyncOutcome

    data class CouldNotCheck(
        val response: BillingResponse,
    ) : SyncOutcome
}
