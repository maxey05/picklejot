package com.maxeydev.picklejot.domain.streak

data class StreakNoticeState(
    val acknowledgedSkipWeek: Long? = null,
    val acknowledgedMissedWeek: Long? = null,
)
