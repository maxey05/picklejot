package com.maxeydev.picklejot.data.reminder

import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.reminder.ReminderScheduling
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration

private const val LOG_TAG = "Picklejot"
private const val SECONDS_PER_WEEK = 604_800L
private const val SLOT_COUNT = 2L

const val STREAK_REMINDER_WORK_PREFIX = "streak-reminder"

class ReminderScheduler(
    private val workManager: WorkManager,
    private val clock: Clock,
) : ReminderScheduling {
    override fun scheduleNext(fireAt: AppInstant) {
        try {
            val delay = (fireAt - clock.now()).coerceAtLeast(Duration.ZERO)
            val request =
                OneTimeWorkRequest
                    .Builder(StreakReminderWorker::class.java)
                    .setInitialDelay(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                    .build()
            workManager.enqueueUniqueWork(workNameFor(fireAt), ExistingWorkPolicy.REPLACE, request)
        } catch (notReady: IllegalStateException) {
            Log.w(LOG_TAG, "The next streak reminder could not be scheduled.", notReady)
        }
    }

    override fun cancel() {
        try {
            allWorkNames().forEach { workManager.cancelUniqueWork(it) }
        } catch (notReady: IllegalStateException) {
            Log.w(LOG_TAG, "The scheduled streak reminder could not be cancelled.", notReady)
        }
    }

    companion object {
        fun workNameFor(fireAt: AppInstant): String =
            "$STREAK_REMINDER_WORK_PREFIX-${(fireAt.epochSeconds / SECONDS_PER_WEEK) % SLOT_COUNT}"

        fun allWorkNames(): List<String> = (0 until SLOT_COUNT).map { "$STREAK_REMINDER_WORK_PREFIX-$it" }
    }
}
