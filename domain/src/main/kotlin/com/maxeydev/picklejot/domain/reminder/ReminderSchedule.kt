package com.maxeydev.picklejot.domain.reminder

import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.datetime.AppTimeZone
import com.maxeydev.picklejot.domain.datetime.atTimeIn
import com.maxeydev.picklejot.domain.datetime.plusDays
import com.maxeydev.picklejot.domain.streak.WeekKey

object ReminderSchedule {
    private const val FRIDAY_OFFSET_FROM_MONDAY = 4
    private const val SUNDAY_OFFSET_FROM_MONDAY = 6
    private const val DEFAULT_FIRE_HOUR = 18

    val DEFAULT_FIRE_TIME: AppTime = AppTime(DEFAULT_FIRE_HOUR, 0)

    fun nextFire(
        now: AppInstant,
        zone: AppTimeZone,
        fireTime: AppTime = DEFAULT_FIRE_TIME,
    ): AppInstant {
        val thisWeek = WeekKey.containing(now, zone)
        val thisFriday = fireOn(thisWeek, FRIDAY_OFFSET_FROM_MONDAY, zone, fireTime)
        return if (thisFriday > now) thisFriday else fireOn(thisWeek.next(), FRIDAY_OFFSET_FROM_MONDAY, zone, fireTime)
    }

    fun isLateInWeek(
        now: AppInstant,
        zone: AppTimeZone,
        fireTime: AppTime = DEFAULT_FIRE_TIME,
    ): Boolean {
        val week = WeekKey.containing(now, zone)
        val opens = fireOn(week, FRIDAY_OFFSET_FROM_MONDAY, zone, fireTime)
        val closes = fireOn(week, SUNDAY_OFFSET_FROM_MONDAY, zone, fireTime)
        return now >= opens && now < closes
    }

    private fun fireOn(
        week: WeekKey,
        offsetFromMonday: Int,
        zone: AppTimeZone,
        fireTime: AppTime,
    ): AppInstant = week.monday.plusDays(offsetFromMonday).atTimeIn(fireTime, zone)
}
