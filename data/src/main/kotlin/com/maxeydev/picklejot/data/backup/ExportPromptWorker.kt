package com.maxeydev.picklejot.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maxeydev.picklejot.domain.backup.BackupRepository

class ExportPromptWorker(
    appContext: Context,
    parameters: WorkerParameters,
    private val repository: suspend () -> BackupRepository,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        repository().evaluateExportPrompt()
        return Result.success()
    }
}
