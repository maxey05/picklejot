package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import com.maxeydev.picklejot.domain.stats.LabelRecord
import com.maxeydev.picklejot.domain.stats.MonthRecord
import com.maxeydev.picklejot.domain.stats.WinLoss
import com.maxeydev.picklejot.domain.stats.percentIfEnoughMatches
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MONTH_PATTERN = "LLLL yyyy"

internal data class BreakdownRow(
    val label: String,
    val record: WinLoss,
)

@Composable
fun AdvancedStatsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    val advanced = state.advanced
    Column(
        modifier = modifier.testTag(DashboardTestTags.STATS_ADVANCED_SECTION),
        verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.lg),
    ) {
        if (advanced.isEmpty) {
            StatsGroup(title = stringResource(R.string.stats_advanced_title)) {
                Text(
                    text = stringResource(R.string.stats_advanced_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .padding(horizontal = PicklejotSpacing.xs)
                            .testTag(DashboardTestTags.STATS_ADVANCED_EMPTY),
                )
            }
        } else {
            ProKeyNumbers(insights = advanced.insights)
            ActivityHeatmap(insights = advanced.insights)
            FormChart(insights = advanced.insights)
            WeekdayChart(insights = advanced.insights)
            RivalsTable(rivals = advanced.headToHead, insights = advanced.insights, state = state)
            PartnerChemistry(partners = advanced.withPartner, insights = advanced.insights, state = state)
            BreakdownCard(
                title = stringResource(R.string.stats_pro_courts_title),
                revealKey = "pro_courts",
                group = DashboardTestTags.GROUP_LOCATION,
                emptyText = stringResource(R.string.stats_advanced_no_locations),
                rows = advanced.byLocation.map { it.toRow() },
            )
            BreakdownCard(
                title = stringResource(R.string.stats_pro_paddles_title),
                revealKey = "pro_paddles",
                group = DashboardTestTags.GROUP_PADDLE,
                emptyText = stringResource(R.string.stats_pro_no_paddles),
                rows = advanced.byPaddle.map { it.toRow() },
            )
            MonthlyBars(months = advanced.monthly)
        }
    }
}

private fun LabelRecord.toRow(): BreakdownRow = BreakdownRow(label = label, record = record)

@Composable
internal fun ExpandableList(
    group: String,
    total: Int,
    collapsedCount: Int,
    content: @Composable (visible: Int) -> Unit,
) {
    var expanded by rememberSaveable(group) { mutableStateOf(false) }
    Column {
        content(if (expanded) total else collapsedCount)
        if (total > collapsedCount) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.testTag(DashboardTestTags.advancedToggle(group)),
            ) {
                Text(
                    if (expanded) {
                        stringResource(R.string.stats_advanced_show_less)
                    } else {
                        stringResource(R.string.stats_advanced_show_all, total)
                    },
                )
            }
        }
    }
}

@Composable
internal fun breakdownValue(row: BreakdownRow): String = valueOf(row.record)

@Composable
internal fun breakdownSupporting(row: BreakdownRow): String =
    if (row.record.percentIfEnoughMatches() == null) {
        stringResource(R.string.stats_advanced_small_sample)
    } else {
        stringResource(R.string.stats_split_record, row.record.wins, row.record.losses)
    }

@Composable
private fun valueOf(record: WinLoss): String =
    record.percentIfEnoughMatches()?.let { stringResource(R.string.stats_percent, it) }
        ?: stringResource(R.string.stats_split_record, record.wins, record.losses)

@Composable
internal fun EmptyNote(
    text: String,
    testTag: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_ROW_HEIGHT)
                .padding(horizontal = PicklejotSpacing.xs, vertical = PicklejotSpacing.md)
                .testTag(testTag),
    )
}

internal fun monthLabel(
    month: MonthRecord,
    locale: Locale,
): String = YearMonth.of(month.year, month.month).format(DateTimeFormatter.ofPattern(MONTH_PATTERN, locale))
