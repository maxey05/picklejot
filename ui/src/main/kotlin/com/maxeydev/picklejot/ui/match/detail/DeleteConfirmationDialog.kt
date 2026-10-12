package com.maxeydev.picklejot.ui.match.detail

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklejot.ui.R

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_match_title)) },
        text = { Text(stringResource(R.string.delete_match_body)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(MatchDetailTestTags.CONFIRM_DELETE),
            ) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(MatchDetailTestTags.CANCEL_DELETE),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
