package com.maxeydev.picklejot.domain.billing

sealed interface PriceResult {
    data class Available(
        val formattedPrice: String,
    ) : PriceResult

    data class Unavailable(
        val problem: StoreProblem,
    ) : PriceResult
}

sealed interface PurchaseOutcome {
    data object Unlocked : PurchaseOutcome

    data object Canceled : PurchaseOutcome

    data object Pending : PurchaseOutcome

    data class Failed(
        val problem: StoreProblem,
    ) : PurchaseOutcome
}

sealed interface RestoreOutcome {
    data object Restored : RestoreOutcome

    data object Pending : RestoreOutcome

    data object NothingToRestore : RestoreOutcome

    data class CouldNotCheck(
        val problem: StoreProblem,
    ) : RestoreOutcome
}
