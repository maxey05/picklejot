package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.plusDays
import com.maxeydev.picklejot.domain.stats.ProInsights
import com.maxeydev.picklejot.domain.streak.WeekKey
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.match.currentLocale
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTheme
import java.time.DayOfWeek
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private const val SIX_MONTH_WEEKS = 26
private const val YEAR_WEEKS = 52
private const val DAYS_IN_WEEK = 7
private const val MAX_LEVEL = 4
private const val POP_MILLIS = 420
private const val COLUMN_STEP_MILLIS = 22
private const val MIN_LABEL_SPAN = 3
private const val LEGEND_LEVELS = 5
private val CELL_GAP = 2.dp
private val CELL_RADIUS = 2.dp
private val WEEKDAY_LABEL_WIDTH = 16.dp
private val LABEL_SPACING = 6.dp
private val LEGEND_CELL = 10.dp
private val TODAY_STROKE = 1.5.dp
private val POP_EASING = CubicBezierEasing(0.3f, 1.5f, 0.5f, 1f)
private val LABELLED_WEEKDAYS = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)

private class MonthSpan(
    val month: Int,
    val columns: Int,
)

internal fun weekStarts(
    today: AppDate,
    weeks: Int,
): List<AppDate> {
    val current = WeekKey.of(today)
    return (weeks - 1 downTo 0).map { back -> WeekKey(current.ordinal - back).monday }
}

internal fun activityLevel(matches: Int): Int = matches.coerceIn(0, MAX_LEVEL)

private fun monthSpans(columns: List<AppDate>): List<MonthSpan> {
    val spans = mutableListOf<MonthSpan>()
    var currentMonth = -1
    var count = 0
    for (monday in columns) {
        val month = monday.month.ordinal + 1
        if (month != currentMonth) {
            if (count > 0) {
                spans.add(MonthSpan(currentMonth, count))
            }
            currentMonth = month
            count = 0
        }
        count++
    }
    spans.add(MonthSpan(currentMonth, count))
    return spans
}

@Composable
private fun activityPalette(): List<Color> =
    with(PicklejotTheme.colors) { listOf(activityNone, activityLow, activityMid, activityHigh, activityPeak) }

private fun levelGrid(
    columns: List<AppDate>,
    insights: ProInsights,
): IntArray {
    val levels = IntArray(columns.size * DAYS_IN_WEEK)
    columns.forEachIndexed { columnIndex, monday ->
        for (row in 0 until DAYS_IN_WEEK) {
            val date = monday.plusDays(row)
            levels[columnIndex * DAYS_IN_WEEK + row] =
                if (date > insights.today) -1 else activityLevel(insights.matchesOn(date))
        }
    }
    return levels
}

@Composable
internal fun ActivityHeatmap(
    insights: ProInsights,
    modifier: Modifier = Modifier,
) {
    val reveal = rememberSectionReveal("pro_activity")
    var showYear by rememberSaveable { mutableStateOf(false) }
    val weeks = if (showYear) YEAR_WEEKS else SIX_MONTH_WEEKS
    val columns = remember(insights.today, weeks) { weekStarts(insights.today, weeks) }
    val levels = remember(columns, insights) { levelGrid(columns, insights) }
    val daysInRange =
        remember(columns, insights) {
            insights.matchesByDay.keys.count { it >= columns.first() && it <= insights.today }
        }
    val totalMillis = columns.size * COLUMN_STEP_MILLIS + POP_MILLIS
    val timeline = remember(weeks) { Animatable(0f) }
    LaunchedEffect(reveal.revealed, weeks) {
        if (reveal.revealed) {
            timeline.animateTo(1f, tween(totalMillis, easing = LinearEasing))
        }
    }
    val summary =
        stringResource(
            R.string.stats_pro_join,
            pluralStringResource(R.plurals.stats_pro_days_played, daysInRange, daysInRange),
            pluralStringResource(R.plurals.stats_pro_longest_run, insights.longestDayRun, insights.longestDayRun),
        )

    ProCard(reveal = reveal, modifier = modifier.testTag(DashboardTestTags.STATS_PRO_HEATMAP)) {
        ProCardHeader(
            title = stringResource(R.string.stats_pro_activity_title),
            trailing = { RangeToggle(showYear = showYear, onChange = { showYear = it }) },
        )
        HeatmapGrid(
            columns = columns,
            levels = levels,
            today = insights.today,
            timeline = timeline,
            totalMillis = totalMillis,
            description = summary,
        )
        HeatmapFooter(summary = summary)
    }
}

@Composable
private fun HeatmapGrid(
    columns: List<AppDate>,
    levels: IntArray,
    today: AppDate,
    timeline: Animatable<Float, AnimationVector1D>,
    totalMillis: Int,
    description: String,
) {
    val locale = currentLocale()
    val showWeekdays = !isLargeFont()
    val palette = activityPalette()
    val todayColor = MaterialTheme.colorScheme.onSurface
    val spans = remember(columns) { monthSpans(columns) }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        val labelWidth = if (showWeekdays) WEEKDAY_LABEL_WIDTH + LABEL_SPACING else 0.dp
        val gridWidth = maxWidth - labelWidth
        val cell = (gridWidth - CELL_GAP * (columns.size - 1)) / columns.size
        val gridHeight = cell * DAYS_IN_WEEK + CELL_GAP * (DAYS_IN_WEEK - 1)
        Column(verticalArrangement = Arrangement.spacedBy(LABEL_SPACING)) {
            MonthLabels(spans = spans, locale = locale, startPadding = labelWidth)
            Row(horizontalArrangement = Arrangement.spacedBy(LABEL_SPACING)) {
                if (showWeekdays) {
                    WeekdayLabels(cell = cell, height = gridHeight, locale = locale)
                }
                Canvas(modifier = Modifier.weight(1f).height(gridHeight)) {
                    val cellPx = cell.toPx()
                    val gapPx = CELL_GAP.toPx()
                    val radius = CornerRadius(CELL_RADIUS.toPx())
                    val elapsed = timeline.value * totalMillis
                    val todayRow = ProInsights.weekdayIndexOf(today)
                    columns.forEachIndexed { columnIndex, _ ->
                        val local = ((elapsed - columnIndex * COLUMN_STEP_MILLIS) / POP_MILLIS).coerceIn(0f, 1f)
                        val scale = POP_EASING.transform(local)
                        if (scale > 0f) {
                            for (row in 0 until DAYS_IN_WEEK) {
                                val level = levels[columnIndex * DAYS_IN_WEEK + row]
                                if (level >= 0) {
                                    val side = cellPx * scale
                                    val inset = (cellPx - side) / 2f
                                    val topLeft =
                                        Offset(
                                            x = columnIndex * (cellPx + gapPx) + inset,
                                            y = row * (cellPx + gapPx) + inset,
                                        )
                                    drawRoundRect(
                                        color = palette[level],
                                        topLeft = topLeft,
                                        size = Size(side, side),
                                        cornerRadius = radius,
                                        alpha = local,
                                    )
                                    if (columnIndex == columns.lastIndex && row == todayRow) {
                                        drawRoundRect(
                                            color = todayColor,
                                            topLeft = topLeft,
                                            size = Size(side, side),
                                            cornerRadius = radius,
                                            alpha = local,
                                            style = Stroke(width = TODAY_STROKE.toPx()),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthLabels(
    spans: List<MonthSpan>,
    locale: Locale,
    startPadding: Dp,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = startPadding)) {
        spans.forEach { span ->
            Box(modifier = Modifier.weight(span.columns.toFloat())) {
                if (span.columns >= MIN_LABEL_SPAN) {
                    Text(
                        text = Month.of(span.month).getDisplayName(TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayLabels(
    cell: Dp,
    height: Dp,
    locale: Locale,
) {
    Column(
        modifier = Modifier.width(WEEKDAY_LABEL_WIDTH).height(height),
        verticalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        for (row in 0 until DAYS_IN_WEEK) {
            val day = DayOfWeek.of(row + 1)
            Box(modifier = Modifier.height(cell)) {
                if (day in LABELLED_WEEKDAYS) {
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.wrapContentHeight(Alignment.CenterVertically, unbounded = true),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapFooter(summary: String) {
    val palette = activityPalette()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = stringResource(R.string.stats_pro_legend_less),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            repeat(LEGEND_LEVELS) { level ->
                Box(
                    modifier =
                        Modifier
                            .size(LEGEND_CELL)
                            .background(palette[level], RoundedCornerShape(CELL_RADIUS)),
                )
            }
            Text(
                text = stringResource(R.string.stats_pro_legend_more),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RangeToggle(
    showYear: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.selectableGroup()) {
        RangeOption(
            label = stringResource(R.string.stats_pro_range_six_months),
            selected = !showYear,
            testTag = DashboardTestTags.STATS_PRO_HEATMAP_SIX_MONTHS,
            onClick = { onChange(false) },
        )
        RangeOption(
            label = stringResource(R.string.stats_pro_range_year),
            selected = showYear,
            testTag = DashboardTestTags.STATS_PRO_HEATMAP_YEAR,
            onClick = { onChange(true) },
        )
    }
}

@Composable
private fun RangeOption(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier =
            Modifier
                .minimumInteractiveComponentSize()
                .testTag(testTag)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = container, contentColor = content) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = PicklejotSpacing.md, vertical = 6.dp),
            )
        }
    }
}
