package com.maxeydev.picklejot.domain.person

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant

data class PersonUsage(
    val person: Person,
    val lastPlayedOn: AppDate,
    val lastLoggedAt: AppInstant,
)
