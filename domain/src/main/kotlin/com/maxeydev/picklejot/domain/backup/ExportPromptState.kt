package com.maxeydev.picklejot.domain.backup

import com.maxeydev.picklejot.domain.datetime.AppInstant

data class ExportPromptState(
    val trackingSince: AppInstant? = null,
    val lastExportAt: AppInstant? = null,
    val lastPromptAt: AppInstant? = null,
    val anchorCount: Int = 0,
    val pending: ExportPromptReason? = null,
)
