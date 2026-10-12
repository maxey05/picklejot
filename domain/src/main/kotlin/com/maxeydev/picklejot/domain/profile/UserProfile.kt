package com.maxeydev.picklejot.domain.profile

import com.maxeydev.picklejot.domain.datetime.AppInstant

data class UserProfile(
    val displayName: String,
    val createdAt: AppInstant,
    val entitlement: Entitlement,
)
