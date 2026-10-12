package com.maxeydev.picklejot.ui.settings

data class BackupActions(
    val onBack: () -> Unit,
    val onExportRequested: () -> Unit,
    val onExportConfirmed: () -> Unit,
    val onExportDialogDismissed: () -> Unit,
    val onShareLaunched: () -> Unit,
    val onShareFailed: () -> Unit,
    val onImportPicked: (String?) -> Unit,
    val onImportSummaryDismissed: () -> Unit,
    val onMessageDismissed: () -> Unit,
)
