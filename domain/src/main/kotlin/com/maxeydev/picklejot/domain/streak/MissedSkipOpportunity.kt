package com.maxeydev.picklejot.domain.streak

data class MissedSkipOpportunity(
    val missedWeek: WeekKey,
    val brokenStreakWeeks: Int,
)
