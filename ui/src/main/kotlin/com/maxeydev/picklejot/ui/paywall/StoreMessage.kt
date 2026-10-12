package com.maxeydev.picklejot.ui.paywall

import androidx.annotation.StringRes
import com.maxeydev.picklejot.domain.billing.StoreProblem
import com.maxeydev.picklejot.ui.R

enum class StoreMessage(
    @StringRes val text: Int,
) {
    UNLOCKED(R.string.store_unlocked),
    RESTORED(R.string.store_restored),
    PENDING(R.string.store_pending),
    NOTHING_TO_RESTORE(R.string.store_nothing_to_restore),
    OFFLINE(R.string.store_offline),
    PLAY_UNAVAILABLE(R.string.store_play_unavailable),
    BILLING_UNAVAILABLE(R.string.store_billing_unavailable),
    PRODUCT_UNAVAILABLE(R.string.store_product_unavailable),
    UNEXPECTED(R.string.store_unexpected),
    ;

    companion object {
        fun of(problem: StoreProblem): StoreMessage =
            when (problem) {
                StoreProblem.OFFLINE -> OFFLINE
                StoreProblem.PLAY_UNAVAILABLE -> PLAY_UNAVAILABLE
                StoreProblem.BILLING_UNAVAILABLE -> BILLING_UNAVAILABLE
                StoreProblem.PRODUCT_UNAVAILABLE -> PRODUCT_UNAVAILABLE
                StoreProblem.UNEXPECTED -> UNEXPECTED
            }
    }
}
