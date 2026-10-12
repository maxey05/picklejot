package com.maxeydev.picklejot.data.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maxeydev.picklejot.domain.reminder.StreakReminder
import kotlinx.coroutines.CancellationException
import java.io.IOException

private const val LOG_TAG = "Picklejot"
private const val MAX_ATTEMPTS = 3

class StreakReminderWorker(
    appContext: Context,
    parameters: WorkerParameters,
    private val reminder: suspend () -> StreakReminder,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result =
        try {
            reminder().onFire()
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (unreadable: IOException) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        } catch (failure: Exception) {
            Log.w(LOG_TAG, "The streak reminder check failed: ${failure.javaClass.simpleName}")
            Result.success()
        }
}
