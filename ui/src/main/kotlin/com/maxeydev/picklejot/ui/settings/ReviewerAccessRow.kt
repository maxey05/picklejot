package com.maxeydev.picklejot.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R

@Composable
fun ReviewerAccessRow(
    state: SettingsUiState,
    onCodeChanged: (String) -> Unit,
    onRedeem: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    SettingsRow(
        icon = R.drawable.ic_lock,
        label = stringResource(R.string.settings_reviewer_row),
        navigates = true,
        modifier =
            modifier
                .clickable(role = Role.Button) { open = true }
                .testTag(SettingsTestTags.REVIEWER_OPEN),
    )
    if (open) {
        val close = {
            onDismiss()
            open = false
        }
        AlertDialog(
            onDismissRequest = close,
            title = { Text(stringResource(R.string.settings_reviewer_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_reviewer_body))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.reviewerCodeDraft,
                        onValueChange = onCodeChanged,
                        enabled = !state.isRedeemingReviewerCode,
                        singleLine = true,
                        label = { Text(stringResource(R.string.settings_reviewer_field_label)) },
                        isError = state.reviewerProblem != null,
                        supportingText = {
                            val problem = state.reviewerProblem
                            if (problem != null) {
                                Text(
                                    text =
                                        stringResource(
                                            when (problem) {
                                                ReviewerProblem.WRONG_CODE -> R.string.settings_reviewer_wrong
                                                ReviewerProblem.UNAVAILABLE -> R.string.settings_reviewer_unavailable
                                            },
                                        ),
                                    modifier =
                                        Modifier
                                            .semantics { liveRegion = LiveRegionMode.Polite }
                                            .testTag(SettingsTestTags.REVIEWER_ERROR),
                                )
                            }
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions = KeyboardActions(onDone = { onRedeem() }),
                        modifier = Modifier.fillMaxWidth().testTag(SettingsTestTags.REVIEWER_FIELD),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onRedeem,
                    enabled = state.canRedeemReviewerCode,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(SettingsTestTags.REVIEWER_UNLOCK),
                ) {
                    Text(stringResource(R.string.settings_reviewer_unlock))
                }
            },
            dismissButton = {
                TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
