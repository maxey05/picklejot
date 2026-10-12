package com.maxeydev.picklejot.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.backup.ImportSummary
import com.maxeydev.picklejot.domain.entitlement.FreeTier
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.AnimatedMascot
import com.maxeydev.picklejot.ui.common.Mascot
import com.maxeydev.picklejot.ui.common.WorkingMascot
import com.maxeydev.picklejot.ui.sound.Cue
import com.maxeydev.picklejot.ui.sound.LocalSoundEffects
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val IMPORT_MIME_TYPES = arrayOf("*/*")
private val WORKING_MASCOT_SIZE = 24.dp
private val MESSAGE_MASCOT_SIZE = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    state: BackupUiState,
    actions: BackupActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sound = LocalSoundEffects.current
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            actions.onImportPicked(uri?.toString())
        }
    LaunchedEffect(state.pendingShare) {
        val share = state.pendingShare ?: return@LaunchedEffect
        sound.play(Cue.EXPORT_READY)
        try {
            ExportShareLauncher.launch(context, share)
            actions.onShareLaunched()
        } catch (noShareTarget: ActivityNotFoundException) {
            actions.onShareFailed()
        }
    }
    LaunchedEffect(state.importSummary != null) {
        if (state.importSummary != null) {
            sound.play(Cue.IMPORT_DONE)
        }
    }
    Scaffold(
        modifier = modifier.testTag(BackupTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            BackupButtons(
                state = state,
                onExport = actions.onExportRequested,
                onImport = { picker.launch(IMPORT_MIME_TYPES) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LastExportCard(state = state)
            Text(
                text = stringResource(R.string.backup_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsCard {
                SettingsRow(
                    icon = R.drawable.ic_info,
                    label = stringResource(R.string.backup_photos_not_included_short),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsRow(
                    icon = R.drawable.ic_info,
                    label = stringResource(R.string.backup_auto_backup_short),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(BackupTestTags.HONESTY),
                )
            }
            state.message?.let { MessageCard(message = it, onDismiss = actions.onMessageDismissed) }
            state.importSummary?.let { ImportSummaryCard(summary = it, onDismiss = actions.onImportSummaryDismissed) }
        }
    }
    if (state.isExportDialogVisible) {
        ExportDialog(onConfirm = actions.onExportConfirmed, onDismiss = actions.onExportDialogDismissed)
    }
}

@Composable
private fun LastExportCard(state: BackupUiState) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PicklejotTheme.colors.header,
        contentColor = PicklejotTheme.colors.onHeader,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp).semantics(mergeDescendants = true) { },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.backup_last_export_label),
                style = MaterialTheme.typography.labelLarge,
                color = PicklejotTheme.colors.onHeaderMuted,
            )
            LastExportText(
                lastExportAt = state.lastExportAt,
                style = MaterialTheme.typography.headlineSmall,
                color = PicklejotTheme.colors.onHeader,
            )
            Text(
                text = stringResource(R.string.backup_on_device_only),
                style = MaterialTheme.typography.bodyMedium,
                color = PicklejotTheme.colors.onHeaderMuted,
            )
        }
    }
}

@Composable
private fun BackupButtons(
    state: BackupUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onExport,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(BackupTestTags.EXPORT),
        ) {
            if (state.isExporting) {
                WorkingIndicator(R.string.backup_export_working)
            } else {
                ButtonLabel(icon = R.drawable.ic_upload, text = R.string.backup_export_button)
            }
        }
        OutlinedButton(
            onClick = onImport,
            enabled = !state.isBusy,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(BackupTestTags.IMPORT),
        ) {
            if (state.isImporting) {
                WorkingIndicator(R.string.backup_import_working)
            } else {
                ButtonLabel(icon = R.drawable.ic_download, text = R.string.backup_import_button)
            }
        }
    }
}

@Composable
private fun ButtonLabel(
    @DrawableRes icon: Int,
    @StringRes text: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(stringResource(text))
    }
}

@Composable
private fun WorkingIndicator(label: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        WorkingMascot(modifier = Modifier.size(WORKING_MASCOT_SIZE))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun MessageCard(
    message: BackupMessage,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag(BackupTestTags.MESSAGE),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AnimatedMascot(mascot = Mascot.HEAD_OOPS, modifier = Modifier.size(MESSAGE_MASCOT_SIZE))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(message.text),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(BackupTestTags.MESSAGE_DISMISS),
                ) {
                    Text(stringResource(R.string.backup_result_ok))
                }
            }
        }
    }
}

@Composable
private fun ImportSummaryCard(
    summary: ImportSummary,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag(BackupTestTags.IMPORT_SUMMARY),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    text = pluralStringResource(R.plurals.backup_import_added, summary.added, summary.added),
                    fontWeight = FontWeight.SemiBold,
                )
                if (summary.alreadyPresent > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.backup_import_skipped,
                            summary.alreadyPresent,
                            summary.alreadyPresent,
                        ),
                    )
                }
                if (summary.heldBack > 0) {
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.backup_import_held_back,
                                summary.heldBack,
                                summary.heldBack,
                                FreeTier.MATCH_LIMIT,
                            ),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag(BackupTestTags.IMPORT_HELD_BACK),
                    )
                }
                if (summary.photosNotRestored > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.backup_import_photos_missing,
                            summary.photosNotRestored,
                            summary.photosNotRestored,
                        ),
                    )
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp).testTag(BackupTestTags.IMPORT_SUMMARY_DISMISS),
            ) {
                Text(stringResource(R.string.backup_result_ok))
            }
        }
    }
}

@Composable
private fun ExportDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(BackupTestTags.EXPORT_DIALOG),
        title = { Text(stringResource(R.string.backup_export_dialog_title)) },
        text = {
            Text(
                text = stringResource(R.string.backup_photos_not_included),
                modifier = Modifier.testTag(BackupTestTags.EXPORT_DIALOG_PHOTOS),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 48.dp).testTag(BackupTestTags.EXPORT_DIALOG_CONFIRM),
            ) {
                Text(stringResource(R.string.backup_export_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp).testTag(BackupTestTags.EXPORT_DIALOG_CANCEL),
            ) {
                Text(stringResource(R.string.backup_dialog_cancel))
            }
        },
    )
}
