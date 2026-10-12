package com.maxeydev.picklejot.ui.paywall

data class PaywallUiState(
    val price: String? = null,
    val isLoadingPrice: Boolean = true,
    val wins: Int = 0,
    val losses: Int = 0,
    val streakWeeks: Int = 0,
    val isWorking: Boolean = false,
    val message: StoreMessage? = null,
    val isUnlocked: Boolean = false,
) {
    val savedMatches: Int
        get() = wins + losses

    val canBuy: Boolean
        get() = price != null && !isWorking && !isUnlocked
}
