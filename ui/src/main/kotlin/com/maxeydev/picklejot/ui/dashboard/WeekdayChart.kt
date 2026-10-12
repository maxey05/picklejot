package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.stats.ProInsights
import com.maxeydev.picklejot.domain.stats.WeekdayRecord
import com.maxeydev.picklejot.domain.stats.percentIfEnoughMatches
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.match.currentLocale
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private const val BAR_MILLIS = 640
private const val BAR_STAGGER_MILLIS = 70
private const val NOTE_DELAY_MILLIS = 700
private val BAR_AREA_HEIGHT = 80.dp
private val MIN_BAR_HEIGHT = 3.dp
private val BAR_CORNER = 6.dp

private fun nameOf(
    dayIndex: Int,
    style: TextStyle,
    locale: Locale,
): String = DayOfWeek.of(dayIndex + 1).getDisplayName(style, locale)

@Composable
internal fun WeekdayChart(
    insights: ProInsights,
    modifier: Modifier = Modifier,
) {
    val reveal = rememberSectionReveal("pro_weekdays")
    val locale = currentLocale()
    val overall = insights.overall.winPercent
    val busiest = insights.weekdays.maxBy { it.record.total }
    val best =
        insights.weekdays
            .filter { it.record.percentIfEnoughMatches() != null }
            .maxByOrNull { it.record.percentIfEnoughMatches() ?: 0 }
    val noteProgress = rememberRevealProgress(reveal.revealed, BAR_MILLIS, NOTE_DELAY_MILLIS)
    val busiestName = nameOf(busiest.dayIndex, TextStyle.FULL, locale)

    ProCard(reveal = reveal, modifier = modifier.testTag(DashboardTestTags.STATS_PRO_WEEKDAYS)) {
        ProCardHeader(
            title = stringResource(R.string.stats_pro_weekdays_title),
            subtitle = stringResource(R.string.stats_pro_weekdays_subtitle),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.xs),
        ) {
            insights.weekdays.forEach { day ->
                WeekdayColumn(
                    day = day,
                    maxMatches = busiest.record.total,
                    overall = overall,
                    revealed = reveal.revealed,
                    locale = locale,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text =
                if (best == null) {
                    stringResource(R.string.stats_pro_weekdays_busiest, busiestName)
                } else {
                    stringResource(
                        R.string.stats_pro_weekdays_best,
                        nameOf(best.dayIndex, TextStyle.FULL, locale),
                        best.record.percentIfEnoughMatches() ?: 0,
                        busiestName,
                    )
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { alpha = noteProgress.value },
        )
    }
}

@Composable
private fun WeekdayColumn(
    day: WeekdayRecord,
    maxMatches: Int,
    overall: Int?,
    revealed: Boolean,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val progress: Animatable<Float, AnimationVector1D> =
        rememberRevealProgress(revealed, BAR_MILLIS, day.dayIndex * BAR_STAGGER_MILLIS)
    val percent = day.record.percentIfEnoughMatches()
    val isAbove = percent != null && overall != null && percent >= overall
    val barColor = if (isAbove) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val percentText =
        percent?.let { stringResource(R.string.stats_percent, it) } ?: stringResource(R.string.stats_no_value)
    val fullName = nameOf(day.dayIndex, TextStyle.FULL, locale)
    val matchesText = pluralStringResource(R.plurals.dashboard_match_count, day.record.total, day.record.total)
    val description = stringResource(R.string.stats_pro_weekday_a11y, fullName, matchesText, percentText)
    val barHeight =
        if (day.record.total == 0) {
            MIN_BAR_HEIGHT
        } else {
            (BAR_AREA_HEIGHT * day.record.total / maxMatches).coerceAtLeast(MIN_BAR_HEIGHT)
        }

    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.xs),
    ) {
        Text(
            text = day.record.total.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.fillMaxWidth().height(BAR_AREA_HEIGHT), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .growFromBottom(progress)
                        .background(barColor, RoundedCornerShape(BAR_CORNER)),
            )
        }
        Text(
            text = nameOf(day.dayIndex, TextStyle.SHORT, locale),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true),
        )
        Text(
            text = percentText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isAbove) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true),
        )
    }
}
