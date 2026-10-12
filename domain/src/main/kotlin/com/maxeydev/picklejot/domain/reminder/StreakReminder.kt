package com.maxeydev.picklejot.domain.reminder

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.datetime.AppTimeZone
import com.maxeydev.picklejot.domain.streak.InsuredStreakEngine
import com.maxeydev.picklejot.domain.streak.StreakRisk
import com.maxeydev.picklejot.domain.streak.WeekKey
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

class StreakReminder(
    private val store: ReminderStore,
    private val matchDates: suspend () -> List<AppDate>,
    private val insuranceStart: suspend () -> AppInstant?,
    private val engine: InsuredStreakEngine,
    private val scheduling: ReminderScheduling,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
    private val timeZone: () -> AppTimeZone,
) {
    suspend fun enable() {
        store.setEnabled(true)
        arm()
    }

    suspend fun disable() {
        scheduling.cancel()
        store.setEnabled(false)
    }

    suspend fun setFireTime(time: AppTime) {
        store.setFireTime(time)
        if (store.observe().first().enabled) {
            arm()
        }
    }

    suspend fun ensureArmed() {
        if (store.observe().first().enabled) {
            arm()
        }
    }

    suspend fun onFire(): ReminderOutcome {
        val state = store.observe().first()
        if (!state.enabled) {
            return ReminderOutcome.DISABLED
        }
        arm()
        val now = clock.now()
        val zone = timeZone()
        if (!ReminderSchedule.isLateInWeek(now, zone, state.fireTime)) {
            return ReminderOutcome.OUTSIDE_WINDOW
        }
        val week = WeekKey.containing(now, zone)
        if (state.lastNotifiedWeek == week.ordinal) {
            return ReminderOutcome.ALREADY_NOTIFIED
        }
        val risk = engine.assessRisk(matchDates(), insuranceStart())
        if (risk == StreakRisk.NONE) {
            return ReminderOutcome.NOT_AT_RISK
        }
        store.markNotified(week.ordinal)
        val posted = notifier.notifyStreakAtRisk(skipAvailable = risk == StreakRisk.AT_RISK_SKIP_AVAILABLE)
        if (!posted) {
            store.markNotified(null)
            return ReminderOutcome.NOT_POSTED
        }
        return ReminderOutcome.NOTIFIED
    }

    private suspend fun arm() {
        val fireTime = store.observe().first().fireTime
        scheduling.scheduleNext(ReminderSchedule.nextFire(clock.now(), timeZone(), fireTime))
    }
}
