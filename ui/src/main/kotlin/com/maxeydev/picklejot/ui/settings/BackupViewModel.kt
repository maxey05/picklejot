package com.maxeydev.picklejot.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklejot.domain.backup.BackupRepository
import com.maxeydev.picklejot.domain.backup.ExportOutcome
import com.maxeydev.picklejot.domain.backup.ImportOutcome
import com.maxeydev.picklejot.ui.PicklejotDependencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BackupViewModel(
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            backupRepository.observeStatus().collect { status ->
                mutableUiState.update { it.copy(lastExportAt = status.lastExportAt) }
            }
        }
    }

    fun requestExport() {
        if (mutableUiState.value.isBusy) {
            return
        }
        mutableUiState.update { it.copy(isExportDialogVisible = true) }
    }

    fun dismissExportDialog() {
        mutableUiState.update { it.copy(isExportDialogVisible = false) }
    }

    fun confirmExport() {
        if (mutableUiState.value.isBusy) {
            return
        }
        mutableUiState.update {
            it.copy(isExportDialogVisible = false, isExporting = true, message = null, importSummary = null)
        }
        viewModelScope.launch {
            when (val outcome = backupRepository.export()) {
                is ExportOutcome.Ready ->
                    mutableUiState.update { it.copy(isExporting = false, pendingShare = outcome) }
                ExportOutcome.Failed ->
                    mutableUiState.update { it.copy(isExporting = false, message = BackupMessage.EXPORT_FAILED) }
            }
        }
    }

    fun shareLaunched() {
        mutableUiState.update { it.copy(pendingShare = null) }
        viewModelScope.launch { backupRepository.recordExportShared() }
    }

    fun shareFailed() {
        mutableUiState.update { it.copy(pendingShare = null, message = BackupMessage.EXPORT_FAILED) }
    }

    fun importPicked(source: String?) {
        if (source == null || mutableUiState.value.isBusy) {
            return
        }
        mutableUiState.update { it.copy(isImporting = true, message = null, importSummary = null) }
        viewModelScope.launch {
            when (val outcome = backupRepository.importFrom(source)) {
                is ImportOutcome.Imported ->
                    mutableUiState.update { it.copy(isImporting = false, importSummary = outcome.summary) }
                is ImportOutcome.Rejected ->
                    mutableUiState.update {
                        it.copy(isImporting = false, message = BackupMessage.of(outcome.problem))
                    }
            }
        }
    }

    fun dismissImportSummary() {
        mutableUiState.update { it.copy(importSummary = null) }
    }

    fun dismissMessage() {
        mutableUiState.update { it.copy(message = null) }
    }

    companion object {
        fun factory(dependencies: PicklejotDependencies): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { BackupViewModel(backupRepository = dependencies.backupRepository) }
            }
    }
}
