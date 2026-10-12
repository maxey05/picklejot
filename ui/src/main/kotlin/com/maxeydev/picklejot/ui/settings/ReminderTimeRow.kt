package com.maxeydev.picklejot.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.match.currentLocale
import com.maxeydev.picklejot.ui.match.edit.MatchTimePickerDialog
import com.maxeydev.picklejot.ui.match.formatMatchTime

@Composable
fun ReminderTimeRow(
    time: AppTime,
    onTimeChanged: (AppTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val shown = formatMatchTime(time, currentLocale())
    val rowDescription = stringResource(R.string.settings_reminder_time_button, shown)
    SettingsRow(
        icon = R.drawable.ic_schedule,
        label = stringResource(R.string.settings_reminder_time_label),
        showDivider = true,
        modifier =
            modifier
                .clickable(role = Role.Button) { picking = true }
                .semantics(mergeDescendants = true) { contentDescription = rowDescription }
                .testTag(SettingsTestTags.REMINDER_TIME),
    ) {
        Text(
            text = shown,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (picking) {
        MatchTimePickerDialog(
            initial = time,
            onConfirm = { picked ->
                picking = false
                onTimeChanged(picked)
            },
            onDismiss = { picking = false },
        )
    }
}
