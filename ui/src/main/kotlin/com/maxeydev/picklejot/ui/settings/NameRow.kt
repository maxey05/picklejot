package com.maxeydev.picklejot.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.DisplayNameField

@Composable
fun NameRow(
    displayName: String,
    nameDraft: String,
    canSave: Boolean,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    SettingsRow(
        icon = R.drawable.ic_person,
        label = stringResource(R.string.settings_name_label),
        value = displayName,
        navigates = true,
        modifier =
            modifier
                .clickable(role = Role.Button) { editing = true }
                .testTag(SettingsTestTags.NAME_OPEN),
    )
    if (editing) {
        val save = {
            onSave()
            editing = false
        }
        val cancel = {
            onCancel()
            editing = false
        }
        AlertDialog(
            onDismissRequest = cancel,
            title = { Text(stringResource(R.string.settings_name_dialog_title)) },
            text = {
                DisplayNameField(
                    value = nameDraft,
                    onValueChange = onNameChanged,
                    onDone = { if (canSave) save() },
                    modifier = Modifier.testTag(SettingsTestTags.NAME_FIELD),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = save,
                    enabled = canSave,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(SettingsTestTags.NAME_SAVE),
                ) {
                    Text(stringResource(R.string.settings_name_save))
                }
            },
            dismissButton = {
                TextButton(onClick = cancel, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
