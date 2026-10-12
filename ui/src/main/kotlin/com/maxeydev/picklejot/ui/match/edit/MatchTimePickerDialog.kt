package com.maxeydev.picklejot.ui.match.edit

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MatchTimePickerDialog(
    initial: AppTime,
    onConfirm: (AppTime) -> Unit,
    onDismiss: () -> Unit,
    clearLabel: String? = null,
    onClear: () -> Unit = {},
) {
    val context = LocalContext.current
    val pickerState =
        rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(AppTime(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            Row {
                if (clearLabel != null) {
                    TextButton(onClick = onClear) {
                        Text(clearLabel)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
        text = { TimePicker(state = pickerState) },
    )
}
