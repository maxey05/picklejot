package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate

object StreakAtRisk {
    fun assess(
        matchDates: Iterable<AppDate>,
        today: AppDate,
        proSince: AppDate?,
    ): StreakRisk {
        val dates = matchDates.toList()
        val thisWeek = WeekKey.of(today)
        if (dates.any { WeekKey.of(it) == thisWeek }) {
            return StreakRisk.NONE
        }
        val insured = StreakInsurance.compute(dates, today, proSince)
        if (!insured.streak.isAlive) {
            return StreakRisk.NONE
        }
        val skipAvailable = proSince != null && StreakInsurance.canCover(dates, thisWeek, proSince)
        return if (skipAvailable) StreakRisk.AT_RISK_SKIP_AVAILABLE else StreakRisk.AT_RISK
    }
}
