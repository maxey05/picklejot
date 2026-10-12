package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant

data class FreeTextUsage(
    val value: String,
    val lastPlayedOn: AppDate,
    val lastLoggedAt: AppInstant,
)
