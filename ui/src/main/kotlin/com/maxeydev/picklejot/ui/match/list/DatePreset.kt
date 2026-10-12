package com.maxeydev.picklejot.ui.match.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.ui.R
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate

private const val MONTHS_IN_QUARTER = 3

enum class DatePreset {
    THIS_MONTH,
    LAST_THREE_MONTHS,
    THIS_YEAR,
    ;

    fun rangeEndingOn(today: AppDate): Pair<AppDate, AppDate> =
        when (this) {
            THIS_MONTH -> today.minus(today.toJavaLocalDate().dayOfMonth - 1, DateTimeUnit.DAY) to today
            LAST_THREE_MONTHS -> today.minus(MONTHS_IN_QUARTER, DateTimeUnit.MONTH) to today
            THIS_YEAR -> today.minus(today.toJavaLocalDate().dayOfYear - 1, DateTimeUnit.DAY) to today
        }
}

fun FilterState.matchingDatePreset(today: AppDate): DatePreset? =
    DatePreset.entries.firstOrNull { preset ->
        val (from, to) = preset.rangeEndingOn(today)
        fromDate == from && toDate == to
    }

@Composable
fun datePresetLabel(preset: DatePreset): String =
    when (preset) {
        DatePreset.THIS_MONTH -> stringResource(R.string.filter_date_this_month)
        DatePreset.LAST_THREE_MONTHS -> stringResource(R.string.filter_date_last_three_months)
        DatePreset.THIS_YEAR -> stringResource(R.string.filter_date_this_year)
    }
