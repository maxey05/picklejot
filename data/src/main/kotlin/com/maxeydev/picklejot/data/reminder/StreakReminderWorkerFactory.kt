package com.maxeydev.picklejot.data.reminder

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.maxeydev.picklejot.domain.reminder.StreakReminder

class StreakReminderWorkerFactory(
    private val reminder: suspend () -> StreakReminder,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        if (workerClassName == StreakReminderWorker::class.java.name) {
            StreakReminderWorker(appContext, workerParameters, reminder)
        } else {
            null
        }
}
