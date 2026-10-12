package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate

object SkipNotices {
    const val MIN_STREAK_WORTH_SAVING = 2
    const val NOTICE_WINDOW_WEEKS = 6L

    fun findMissedOpportunity(
        matchDates: Iterable<AppDate>,
        today: AppDate,
    ): MissedSkipOpportunity? {
        val played = matchDates.mapTo(HashSet()) { WeekKey.of(it) }
        val missedWeek = WeekKey.of(today).previous()
        if (missedWeek in played) {
            return null
        }
        var week = missedWeek.previous()
        var length = 0
        while (week in played) {
            length += 1
            week = week.previous()
        }
        if (length < MIN_STREAK_WORTH_SAVING) {
            return null
        }
        return MissedSkipOpportunity(missedWeek = missedWeek, brokenStreakWeeks = length)
    }

    fun unacknowledgedSkip(
        insured: InsuredStreak,
        today: AppDate,
        acknowledgedThrough: Long?,
    ): WeekKey? {
        val oldestShown = WeekKey.of(today).ordinal - NOTICE_WINDOW_WEEKS
        return insured.skippedWeeks
            .filter { it.ordinal >= oldestShown && (acknowledgedThrough == null || it.ordinal > acknowledgedThrough) }
            .maxOrNull()
    }

    fun isMissedOpportunityAcknowledged(
        opportunity: MissedSkipOpportunity,
        acknowledgedWeek: Long?,
    ): Boolean = acknowledgedWeek != null && acknowledgedWeek >= opportunity.missedWeek.ordinal
}
