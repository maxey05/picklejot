package com.maxeydev.picklejot.data.backup

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

const val EXPORT_PROMPT_PERIODIC_WORK = "export-prompt-periodic"
const val EXPORT_PROMPT_CHECK_WORK = "export-prompt-check"

class ExportPromptScheduler(
    private val workManager: WorkManager,
) {
    fun schedulePeriodicCheck() {
        val request = PeriodicWorkRequest.Builder(ExportPromptWorker::class.java, 1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(EXPORT_PROMPT_PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun checkNow() {
        val request = OneTimeWorkRequest.Builder(ExportPromptWorker::class.java).build()
        workManager.enqueueUniqueWork(EXPORT_PROMPT_CHECK_WORK, ExistingWorkPolicy.REPLACE, request)
    }
}
