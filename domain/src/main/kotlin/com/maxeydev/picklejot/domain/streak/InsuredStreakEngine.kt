package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTimeZone
import com.maxeydev.picklejot.domain.datetime.toAppDateIn
import kotlin.time.Clock

class InsuredStreakEngine(
    private val clock: Clock,
    private val timeZone: () -> AppTimeZone,
) {
    fun today(): AppDate = WeekKey.localDateOf(clock.now(), timeZone())

    fun compute(
        matchDates: Iterable<AppDate>,
        insuranceStart: AppInstant?,
    ): InsuredStreak {
        val zone = timeZone()
        return StreakInsurance.compute(
            matchDates = matchDates,
            today = WeekKey.localDateOf(clock.now(), zone),
            proSince = insuranceStart?.toAppDateIn(zone),
        )
    }

    fun assessRisk(
        matchDates: Iterable<AppDate>,
        insuranceStart: AppInstant?,
    ): StreakRisk {
        val zone = timeZone()
        return StreakAtRisk.assess(
            matchDates = matchDates,
            today = WeekKey.localDateOf(clock.now(), zone),
            proSince = insuranceStart?.toAppDateIn(zone),
        )
    }
}
