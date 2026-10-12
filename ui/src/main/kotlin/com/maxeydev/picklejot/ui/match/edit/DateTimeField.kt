package com.maxeydev.picklejot.ui.match.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.AddPill
import com.maxeydev.picklejot.ui.common.dashedBorder
import com.maxeydev.picklejot.ui.match.currentLocale
import com.maxeydev.picklejot.ui.match.formatMatchDate
import com.maxeydev.picklejot.ui.match.formatMatchTime
import com.maxeydev.picklejot.ui.match.toUtcEpochMillis
import com.maxeydev.picklejot.ui.match.todayInDeviceZone
import com.maxeydev.picklejot.ui.match.utcEpochMillisToAppDate

private val PILL_HEIGHT = 40.dp
private val MIN_TOUCH_TARGET = 48.dp
private val FALLBACK_PICKER_TIME = AppTime(12, 0)

private enum class DateTimeEditor {
    DATE,
    START,
    END,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateTimeField(
    date: AppDate,
    startTime: AppTime?,
    endTime: AppTime?,
    onDateSelected: (AppDate) -> Unit,
    onStartTimeChanged: (AppTime?) -> Unit,
    onEndTimeChanged: (AppTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf<DateTimeEditor?>(null) }
    val locale = currentLocale()
    val dateText = formatMatchDate(date, locale)
    val notSet = stringResource(R.string.time_not_set)
    val startLabel = stringResource(R.string.label_start_time)
    val endLabel = stringResource(R.string.label_end_time)
    val startText = startTime?.let { formatMatchTime(it, locale) }
    val endText = endTime?.let { formatMatchTime(it, locale) }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DateTimePill(
            text = dateText,
            isSet = true,
            description = stringResource(R.string.date_button_description, dateText),
            onClick = { editing = DateTimeEditor.DATE },
        )
        DateTimePill(
            text = startText ?: startLabel,
            isSet = startText != null,
            description = stringResource(R.string.time_button_description, startLabel, startText ?: notSet),
            onClick = { editing = DateTimeEditor.START },
        )
        if (endText != null) {
            Box(
                modifier = Modifier.height(MIN_TOUCH_TARGET).clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.time_range_separator),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DateTimePill(
                text = endText,
                isSet = true,
                description = stringResource(R.string.time_button_description, endLabel, endText),
                onClick = { editing = DateTimeEditor.END },
            )
        } else {
            AddPill(
                label = stringResource(R.string.add_end_time),
                description = stringResource(R.string.time_button_description, endLabel, notSet),
                onClick = { editing = DateTimeEditor.END },
            )
        }
    }
    when (editing) {
        DateTimeEditor.DATE ->
            MatchDatePickerDialog(
                initial = date,
                onConfirm = { picked ->
                    onDateSelected(picked)
                    editing = null
                },
                onDismiss = { editing = null },
            )
        DateTimeEditor.START ->
            MatchTimePickerDialog(
                initial = startTime ?: endTime ?: FALLBACK_PICKER_TIME,
                onConfirm = { picked ->
                    onStartTimeChanged(picked)
                    editing = null
                },
                onDismiss = { editing = null },
                clearLabel = if (startTime != null) stringResource(R.string.clear_start_time) else null,
                onClear = {
                    onStartTimeChanged(null)
                    editing = null
                },
            )
        DateTimeEditor.END ->
            MatchTimePickerDialog(
                initial = endTime ?: startTime ?: FALLBACK_PICKER_TIME,
                onConfirm = { picked ->
                    onEndTimeChanged(picked)
                    editing = null
                },
                onDismiss = { editing = null },
                clearLabel = if (endTime != null) stringResource(R.string.clear_end_time) else null,
                onClear = {
                    onEndTimeChanged(null)
                    editing = null
                },
            )
        null -> Unit
    }
}

@Composable
private fun DateTimePill(
    text: String,
    isSet: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier =
            Modifier
                .heightIn(min = MIN_TOUCH_TARGET)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surfaceContainerLowest,
            contentColor = if (isSet) colors.onSurface else colors.onSurfaceVariant,
            border = if (isSet) BorderStroke(1.dp, colors.outlineVariant) else null,
            modifier = if (isSet) Modifier else Modifier.dashedBorder(colors.outline, PILL_HEIGHT / 2),
        ) {
            Box(
                modifier = Modifier.heightIn(min = PILL_HEIGHT).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchDatePickerDialog(
    initial: AppDate,
    onConfirm: (AppDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { todayInDeviceZone() }
    val selectableDates = remember(today) { PastDatesOnly(today.toUtcEpochMillis(), today.year) }
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initial.toUtcEpochMillis(),
            selectableDates = selectableDates,
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = pickerState.selectedDateMillis
                    if (selected == null) {
                        onDismiss()
                    } else {
                        onConfirm(utcEpochMillisToAppDate(selected))
                    }
                },
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        Column {
            DatePicker(state = pickerState)
            Text(
                text = stringResource(R.string.date_future_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class PastDatesOnly(
    private val lastSelectableMillis: Long,
    private val lastSelectableYear: Int,
) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= lastSelectableMillis

    override fun isSelectableYear(year: Int): Boolean = year <= lastSelectableYear
}
