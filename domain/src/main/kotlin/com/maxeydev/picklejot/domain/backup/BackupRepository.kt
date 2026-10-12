package com.maxeydev.picklejot.domain.backup

import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    fun observeStatus(): Flow<BackupStatus>

    suspend fun export(): ExportOutcome

    suspend fun recordExportShared()

    suspend fun importFrom(source: String): ImportOutcome

    suspend fun dismissExportPrompt()

    suspend fun evaluateExportPrompt()
}
