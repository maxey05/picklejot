package com.maxeydev.picklejot.data.billing

import com.maxeydev.picklejot.domain.billing.PriceResult
import com.maxeydev.picklejot.domain.billing.ProStore
import com.maxeydev.picklejot.domain.billing.PurchaseOutcome
import com.maxeydev.picklejot.domain.billing.RestoreOutcome
import com.maxeydev.picklejot.domain.billing.StoreProblem

class PlayProStore(
    private val gateway: BillingGateway,
    private val repository: PlayEntitlementRepository,
    private val launchPurchase: suspend () -> GatewayResult<List<PlayPurchase>>?,
) : ProStore {
    override suspend fun loadPrice(): PriceResult =
        when (val result = gateway.proPrice()) {
            is GatewayResult.Success -> PriceResult.Available(result.value)
            is GatewayResult.Failure -> PriceResult.Unavailable(result.response.problem)
        }

    override suspend fun purchase(): PurchaseOutcome {
        val result = launchPurchase() ?: return PurchaseOutcome.Failed(StoreProblem.UNEXPECTED)
        return when (result) {
            is GatewayResult.Success -> outcomeOf(repository.process(result.value))
            is GatewayResult.Failure ->
                when (result.response) {
                    BillingResponse.USER_CANCELED -> PurchaseOutcome.Canceled
                    BillingResponse.ITEM_ALREADY_OWNED -> outcomeOf(repository.syncWithPlay())
                    else -> PurchaseOutcome.Failed(result.response.problem)
                }
        }
    }

    override suspend fun restore(): RestoreOutcome =
        when (val outcome = repository.syncWithPlay()) {
            is SyncOutcome.Granted -> RestoreOutcome.Restored
            SyncOutcome.Pending -> RestoreOutcome.Pending
            SyncOutcome.NothingFound -> RestoreOutcome.NothingToRestore
            is SyncOutcome.CouldNotCheck -> RestoreOutcome.CouldNotCheck(outcome.response.problem)
        }

    private fun outcomeOf(sync: SyncOutcome): PurchaseOutcome =
        when (sync) {
            is SyncOutcome.Granted -> PurchaseOutcome.Unlocked
            SyncOutcome.Pending -> PurchaseOutcome.Pending
            SyncOutcome.NothingFound -> PurchaseOutcome.Failed(StoreProblem.UNEXPECTED)
            is SyncOutcome.CouldNotCheck -> PurchaseOutcome.Failed(sync.response.problem)
        }
}
