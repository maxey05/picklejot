package com.maxeydev.picklejot.domain.backup

sealed interface ExportOutcome {
    data class Ready(
        val filePath: String,
        val fileName: String,
        val matchCount: Int,
    ) : ExportOutcome

    data object Failed : ExportOutcome
}
