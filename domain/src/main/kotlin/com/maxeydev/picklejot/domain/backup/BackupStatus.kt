package com.maxeydev.picklejot.domain.backup

import com.maxeydev.picklejot.domain.datetime.AppInstant

data class BackupStatus(
    val lastExportAt: AppInstant? = null,
    val pendingPrompt: ExportPromptReason? = null,
)
