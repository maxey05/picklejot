@file:OptIn(ExperimentalCoroutinesApi::class)

package com.maxeydev.picklejot.ui.settings

import com.maxeydev.picklejot.domain.backup.ExportOutcome
import com.maxeydev.picklejot.domain.backup.ImportOutcome
import com.maxeydev.picklejot.domain.backup.ImportProblem
import com.maxeydev.picklejot.domain.backup.ImportSummary
import com.maxeydev.picklejot.ui.fakes.FakeBackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupViewModelTest {
    private val backup = FakeBackupRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `asking to export shows the photo disclosure and exports nothing yet`() {
        val viewModel = BackupViewModel(backup)

        viewModel.requestExport()

        assertTrue(viewModel.uiState.value.isExportDialogVisible)
        assertEquals(0, backup.exportCalls)
    }

    @Test
    fun `cancelling the disclosure exports nothing`() {
        val viewModel = BackupViewModel(backup)
        viewModel.requestExport()

        viewModel.dismissExportDialog()

        assertFalse(viewModel.uiState.value.isExportDialogVisible)
        assertEquals(0, backup.exportCalls)
    }

    @Test
    fun `confirming exports and hands the file over for sharing without recording it yet`() {
        val viewModel = BackupViewModel(backup)
        viewModel.requestExport()

        viewModel.confirmExport()

        val state = viewModel.uiState.value
        assertEquals(1, backup.exportCalls)
        assertFalse(state.isExporting)
        assertFalse(state.isExportDialogVisible)
        assertNotNull(state.pendingShare)
        assertEquals(0, backup.sharesRecorded)
    }

    @Test
    fun `launching the share sheet records the export and clears the pending file`() {
        val viewModel = BackupViewModel(backup)
        viewModel.confirmExport()

        viewModel.shareLaunched()

        assertNull(viewModel.uiState.value.pendingShare)
        assertEquals(1, backup.sharesRecorded)
    }

    @Test
    fun `a share sheet that cannot open reports a failure and does not record an export`() {
        val viewModel = BackupViewModel(backup)
        viewModel.confirmExport()

        viewModel.shareFailed()

        assertNull(viewModel.uiState.value.pendingShare)
        assertEquals(BackupMessage.EXPORT_FAILED, viewModel.uiState.value.message)
        assertEquals(0, backup.sharesRecorded)
    }

    @Test
    fun `an export that cannot be written explains itself and records nothing`() {
        backup.exportOutcome = ExportOutcome.Failed
        val viewModel = BackupViewModel(backup)

        viewModel.confirmExport()

        assertEquals(BackupMessage.EXPORT_FAILED, viewModel.uiState.value.message)
        assertNull(viewModel.uiState.value.pendingShare)
        assertEquals(0, backup.sharesRecorded)
    }

    @Test
    fun `a successful import shows exactly how many were added, skipped and held back`() {
        val summary = ImportSummary(added = 20, alreadyPresent = 5, heldBack = 10, photosNotRestored = 2)
        backup.importOutcome = ImportOutcome.Imported(summary)
        val viewModel = BackupViewModel(backup)

        viewModel.importPicked("content://files/export.json")

        assertEquals(summary, viewModel.uiState.value.importSummary)
        assertEquals(listOf("content://files/export.json"), backup.importedSources)
        assertFalse(viewModel.uiState.value.isImporting)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `a rejected import says so in plain words and shows no summary`() {
        backup.importOutcome = ImportOutcome.Rejected(ImportProblem.CORRUPT_CONTENT)
        val viewModel = BackupViewModel(backup)

        viewModel.importPicked("content://files/export.json")

        assertEquals(BackupMessage.CORRUPT, viewModel.uiState.value.message)
        assertNull(viewModel.uiState.value.importSummary)
    }

    @Test
    fun `every import problem has its own message`() {
        val messages = ImportProblem.entries.map { BackupMessage.of(it) }

        assertEquals(ImportProblem.entries.size, messages.toSet().size)
    }

    @Test
    fun `cancelling the file picker does nothing`() {
        val viewModel = BackupViewModel(backup)

        viewModel.importPicked(null)

        assertTrue(backup.importedSources.isEmpty())
        assertFalse(viewModel.uiState.value.isImporting)
    }

    @Test
    fun `dismissing the summary and the message clears them`() {
        backup.importOutcome = ImportOutcome.Imported(ImportSummary(1, 0, 0, 0))
        val viewModel = BackupViewModel(backup)
        viewModel.importPicked("content://files/export.json")

        viewModel.dismissImportSummary()
        viewModel.shareFailed()
        viewModel.dismissMessage()

        assertNull(viewModel.uiState.value.importSummary)
        assertNull(viewModel.uiState.value.message)
    }
}
