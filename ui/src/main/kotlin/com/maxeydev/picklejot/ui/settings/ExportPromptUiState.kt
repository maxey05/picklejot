package com.maxeydev.picklejot.ui.settings

import com.maxeydev.picklejot.domain.backup.ExportPromptReason

data class ExportPromptUiState(
    val reason: ExportPromptReason? = null,
)
