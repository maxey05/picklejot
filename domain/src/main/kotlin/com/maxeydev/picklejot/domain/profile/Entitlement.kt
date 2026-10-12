package com.maxeydev.picklejot.domain.profile

import com.maxeydev.picklejot.domain.datetime.AppInstant

data class Entitlement(
    val isPro: Boolean,
    val purchaseToken: String? = null,
    val lastVerifiedAt: AppInstant? = null,
    val proSince: AppInstant? = null,
)
