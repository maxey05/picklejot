package com.maxeydev.picklejot.data.backup

import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.domain.backup.BackupRepository
import com.maxeydev.picklejot.domain.backup.BackupStatus
import com.maxeydev.picklejot.domain.backup.ExportOutcome
import com.maxeydev.picklejot.domain.backup.ExportPromptPolicy
import com.maxeydev.picklejot.domain.backup.ExportPromptStore
import com.maxeydev.picklejot.domain.backup.ImportOutcome
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.time.Clock

const val MAX_IMPORT_BYTES = 32 * 1024 * 1024

class RoomBackupRepository(
    database: PicklejotDatabase,
    photoStore: PhotoStore,
    entitlements: EntitlementRepository,
    private val promptStore: ExportPromptStore,
    exportDirectory: File,
    private val openSource: (String) -> InputStream?,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher,
) : BackupRepository {
    private val matchDao = database.matchDao()
    private val exporter = JsonExporter(database, exportDirectory, clock, ioDispatcher)
    private val importer = JsonImporter(database, photoStore, entitlements, ioDispatcher)

    override fun observeStatus(): Flow<BackupStatus> =
        promptStore
            .observe()
            .map { state -> BackupStatus(lastExportAt = state.lastExportAt, pendingPrompt = state.pending) }
            .distinctUntilChanged()

    override suspend fun export(): ExportOutcome = exporter.export()

    override suspend fun recordExportShared() {
        val count = withContext(ioDispatcher) { matchDao.matchCount() }
        val now = clock.now()
        promptStore.update { ExportPromptPolicy.afterExport(it, count, now) }
    }

    override suspend fun importFrom(source: String): ImportOutcome {
        val outcome = importer.import { openSource(source) }
        if (outcome is ImportOutcome.Imported) {
            val count = withContext(ioDispatcher) { matchDao.matchCount() }
            promptStore.update { ExportPromptPolicy.afterImport(it, count) }
        }
        return outcome
    }

    override suspend fun dismissExportPrompt() {
        promptStore.update(ExportPromptPolicy::afterDismissal)
    }

    override suspend fun evaluateExportPrompt() {
        val count = withContext(ioDispatcher) { matchDao.matchCount() }
        val now = clock.now()
        promptStore.update { ExportPromptPolicy.evaluate(it, count, now) }
    }
}
