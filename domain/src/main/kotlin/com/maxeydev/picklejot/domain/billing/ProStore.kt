package com.maxeydev.picklejot.domain.billing

interface ProStore {
    suspend fun loadPrice(): PriceResult

    suspend fun purchase(): PurchaseOutcome

    suspend fun restore(): RestoreOutcome
}
