package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.backup.BackupRepository
import com.maxeydev.picklejot.domain.backup.BackupStatus
import com.maxeydev.picklejot.domain.backup.ExportOutcome
import com.maxeydev.picklejot.domain.backup.ExportPromptReason
import com.maxeydev.picklejot.domain.backup.ImportOutcome
import com.maxeydev.picklejot.domain.backup.ImportSummary
import com.maxeydev.picklejot.domain.datetime.AppInstant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.CopyOnWriteArrayList

class FakeBackupRepository(
    var exportOutcome: ExportOutcome =
        ExportOutcome.Ready(
            filePath = "/tmp/picklejot-export-2026-09-30.json",
            fileName = "picklejot-export-2026-09-30.json",
            matchCount = 3,
        ),
    var importOutcome: ImportOutcome = ImportOutcome.Imported(ImportSummary(0, 0, 0, 0)),
) : BackupRepository {
    private val status = MutableStateFlow(BackupStatus())

    val importedSources: MutableList<String> = CopyOnWriteArrayList()

    var exportCalls = 0
        private set

    var sharesRecorded = 0
        private set

    var dismissals = 0
        private set

    var evaluations = 0
        private set

    fun showPrompt(reason: ExportPromptReason?) {
        status.update { it.copy(pendingPrompt = reason) }
    }

    fun setLastExport(instant: AppInstant?) {
        status.update { it.copy(lastExportAt = instant) }
    }

    override fun observeStatus(): Flow<BackupStatus> = status

    override suspend fun export(): ExportOutcome {
        exportCalls++
        return exportOutcome
    }

    override suspend fun recordExportShared() {
        sharesRecorded++
        status.update { it.copy(lastExportAt = AppInstant.fromEpochMilliseconds(0), pendingPrompt = null) }
    }

    override suspend fun importFrom(source: String): ImportOutcome {
        importedSources += source
        return importOutcome
    }

    override suspend fun dismissExportPrompt() {
        dismissals++
        status.update { it.copy(pendingPrompt = null) }
    }

    override suspend fun evaluateExportPrompt() {
        evaluations++
    }
}
