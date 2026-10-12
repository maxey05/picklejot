package com.maxeydev.picklejot.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private enum class EraseStep {
    NONE,
    ASK,
    FINAL,
}

@Composable
fun EraseDataSection(
    isErasing: Boolean,
    eraseFailed: Boolean,
    savedMatches: Int,
    onOpenBackup: () -> Unit,
    onEraseConfirmed: () -> Unit,
    onEraseFailureDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableStateOf(EraseStep.NONE) }
    Surface(
        shape = SETTINGS_CARD_SHAPE,
        color = settingsCardColor(),
        border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        SettingsRow(
            icon = R.drawable.ic_warning,
            label = stringResource(R.string.settings_erase_open),
            contentColor = MaterialTheme.colorScheme.error,
            iconTint = MaterialTheme.colorScheme.error,
            modifier =
                Modifier
                    .clickable(role = Role.Button) { step = EraseStep.ASK }
                    .testTag(SettingsTestTags.ERASE_OPEN),
        )
    }
    val dismiss = {
        step = EraseStep.NONE
        onEraseFailureDismissed()
    }
    when (step) {
        EraseStep.NONE -> Unit
        EraseStep.ASK ->
            EraseAskDialog(
                isErasing = isErasing,
                onExportFirst = {
                    step = EraseStep.NONE
                    onOpenBackup()
                },
                onContinue = { step = EraseStep.FINAL },
                onDismiss = dismiss,
            )
        EraseStep.FINAL ->
            EraseFinalDialog(
                isErasing = isErasing,
                eraseFailed = eraseFailed,
                savedMatches = savedMatches,
                onConfirm = onEraseConfirmed,
                onDismiss = dismiss,
            )
    }
}

@Composable
private fun EraseAskDialog(
    isErasing: Boolean,
    onExportFirst: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isErasing) onDismiss() },
        title = { Text(stringResource(R.string.settings_erase_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_erase_dialog_body))
                TextButton(
                    onClick = onExportFirst,
                    enabled = !isErasing,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(SettingsTestTags.ERASE_EXPORT_FIRST),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_upload),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_erase_export_first),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onContinue,
                enabled = !isErasing,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.heightIn(min = 48.dp).testTag(SettingsTestTags.ERASE_CONFIRM),
            ) {
                Text(stringResource(R.string.settings_erase_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isErasing,
                modifier = Modifier.heightIn(min = 48.dp).testTag(SettingsTestTags.ERASE_CANCEL),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = Modifier.testTag(SettingsTestTags.ERASE_ASK_DIALOG),
    )
}

@Composable
private fun EraseFinalDialog(
    isErasing: Boolean,
    eraseFailed: Boolean,
    savedMatches: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = { if (!isErasing) onDismiss() },
        title = { Text(stringResource(R.string.settings_erase_final_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (savedMatches > 0) {
                        pluralStringResource(R.plurals.settings_erase_final_body, savedMatches, savedMatches)
                    } else {
                        stringResource(R.string.settings_erase_final_body_empty)
                    },
                )
                if (eraseFailed) {
                    Text(
                        text = stringResource(R.string.settings_erase_failed),
                        color = colors.error,
                        modifier = Modifier.testTag(SettingsTestTags.ERASE_ERROR),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isErasing,
                colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
                modifier = Modifier.heightIn(min = 48.dp).testTag(SettingsTestTags.ERASE_FINAL_CONFIRM),
            ) {
                Text(stringResource(R.string.settings_erase_final_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isErasing,
                modifier = Modifier.heightIn(min = 48.dp).testTag(SettingsTestTags.ERASE_FINAL_CANCEL),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = Modifier.testTag(SettingsTestTags.ERASE_FINAL_DIALOG),
    )
}
