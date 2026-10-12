package com.maxeydev.picklejot.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklejot.domain.backup.BackupRepository
import com.maxeydev.picklejot.ui.PicklejotDependencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExportPromptViewModel(
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ExportPromptUiState())
    val uiState: StateFlow<ExportPromptUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            backupRepository.observeStatus().collect { status ->
                mutableUiState.update { it.copy(reason = status.pendingPrompt) }
            }
        }
    }

    fun dismiss() {
        viewModelScope.launch { backupRepository.dismissExportPrompt() }
    }

    companion object {
        fun factory(dependencies: PicklejotDependencies): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ExportPromptViewModel(backupRepository = dependencies.backupRepository) }
            }
    }
}
