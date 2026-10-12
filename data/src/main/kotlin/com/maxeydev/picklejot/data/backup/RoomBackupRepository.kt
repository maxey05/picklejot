package com.maxeydev.picklejot.data.backup

import com.maxeydev.picklejot.data.db.PicklejotDatabase
import com.maxeydev.picklejot.data.photo.PhotoStore
import com.maxeydev.picklejot.domain.backup.BackupRepository
import com.maxeydev.picklejot.domain.backup.BackupStatus
import com.maxeydev.picklejot.domain.backup.ExportOutcome
import com.maxeydev.picklejot.domain.backup.ExportPromptPolicy
import com.maxeydev.picklejot.domain.backup.ExportPromptStore
import com.maxeydev.picklejot.domain.backup.ImportOutcome
import com.maxeydev.picklejot.domain.backup.ImportProblem
import com.maxeydev.picklejot.domain.profile.EntitlementRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.time.Clock

const val MAX_IMPORT_BYTES = 32 * 1024 * 1024

private const val READ_CHUNK_BYTES = 8 * 1024

private const val BYTE_ORDER_MARK = "\uFEFF"

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
        val text =
            when (val read = readText(source)) {
                is SourceText.Failed -> return ImportOutcome.Rejected(read.problem)
                is SourceText.Loaded -> read.text
            }
        val outcome = importer.import(text)
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

    private suspend fun readText(source: String): SourceText =
        withContext(ioDispatcher) {
            try {
                val stream = openSource(source) ?: return@withContext SourceText.Failed(ImportProblem.NOT_READABLE)
                val bytes = stream.use { readBounded(it) }
                if (bytes == null) {
                    SourceText.Failed(ImportProblem.TOO_LARGE)
                } else {
                    SourceText.Loaded(bytes.decodeToString().removePrefix(BYTE_ORDER_MARK))
                }
            } catch (unreadable: IOException) {
                SourceText.Failed(ImportProblem.NOT_READABLE)
            } catch (denied: SecurityException) {
                SourceText.Failed(ImportProblem.NOT_READABLE)
            }
        }

    private fun readBounded(input: InputStream): ByteArray? {
        val collected = ByteArrayOutputStream()
        val chunk = ByteArray(READ_CHUNK_BYTES)
        var total = 0
        while (true) {
            val read = input.read(chunk)
            if (read < 0) {
                return collected.toByteArray()
            }
            total += read
            if (total > MAX_IMPORT_BYTES) {
                return null
            }
            collected.write(chunk, 0, read)
        }
    }

    private sealed interface SourceText {
        data class Loaded(
            val text: String,
        ) : SourceText

        data class Failed(
            val problem: ImportProblem,
        ) : SourceText
    }
}
