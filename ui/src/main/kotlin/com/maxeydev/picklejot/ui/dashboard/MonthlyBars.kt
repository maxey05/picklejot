package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.stats.MonthRecord
import com.maxeydev.picklejot.domain.stats.percentIfEnoughMatches
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.match.currentLocale
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val BAR_MILLIS = 560
private const val BAR_STAGGER_MILLIS = 50
private const val MAX_STAGGER_STEPS = 8
private const val SHORT_MONTH_PATTERN = "LLL"
private const val YEAR_PATTERN = "yyyy"
private val COLUMN_WIDTH = 52.dp
private val BAR_WIDTH = 22.dp
private val HALF_HEIGHT = 56.dp
private val MIN_BAR_HEIGHT = 3.dp
private val BAR_CORNER = 5.dp

@Composable
internal fun MonthlyBars(
    months: List<MonthRecord>,
    modifier: Modifier = Modifier,
) {
    val reveal = rememberSectionReveal("pro_months")
    val locale = currentLocale()
    val unit = months.maxOfOrNull { maxOf(it.record?.wins ?: 0, it.record?.losses ?: 0) }?.coerceAtLeast(1) ?: 1

    ProCard(reveal = reveal, modifier = modifier) {
        ProCardHeader(
            title = stringResource(R.string.stats_pro_months_title),
            subtitle = stringResource(R.string.stats_pro_months_legend),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            reverseLayout = true,
            horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.xs),
        ) {
            itemsIndexed(months) { index, month ->
                MonthColumn(
                    month = month,
                    index = index,
                    unit = unit,
                    revealed = reveal.revealed,
                    locale = locale,
                )
            }
        }
    }
}

@Composable
private fun MonthColumn(
    month: MonthRecord,
    index: Int,
    unit: Int,
    revealed: Boolean,
    locale: Locale,
) {
    val progress =
        rememberRevealProgress(revealed, BAR_MILLIS, minOf(index, MAX_STAGGER_STEPS) * BAR_STAGGER_MILLIS)
    val yearMonth = YearMonth.of(month.year, month.month)
    val shortName = yearMonth.format(DateTimeFormatter.ofPattern(SHORT_MONTH_PATTERN, locale))
    val year = yearMonth.format(DateTimeFormatter.ofPattern(YEAR_PATTERN, locale))
    val record = month.record
    val fullName = monthLabel(month, locale)
    val description =
        if (record == null) {
            stringResource(R.string.stats_pro_month_a11y_gap, fullName)
        } else {
            stringResource(R.string.stats_pro_month_a11y, fullName, record.wins, record.losses)
        }
    val percentText =
        record?.percentIfEnoughMatches()?.let { stringResource(R.string.stats_percent, it) }
            ?: stringResource(R.string.stats_no_value)

    Column(
        modifier =
            Modifier
                .width(COLUMN_WIDTH)
                .testTag(DashboardTestTags.advancedRow(DashboardTestTags.GROUP_MONTH, index))
                .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.xs),
    ) {
        Text(
            text = percentText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
        Box(modifier = Modifier.width(BAR_WIDTH).height(HALF_HEIGHT), contentAlignment = Alignment.BottomCenter) {
            if (record != null && record.wins > 0) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(barHeight(record.wins, unit))
                            .growFromBottom(progress)
                            .background(PicklejotTheme.colors.winBadge, RoundedCornerShape(BAR_CORNER)),
                )
            }
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = PicklejotTheme.colors.cardBorder)
        Box(modifier = Modifier.width(BAR_WIDTH).height(HALF_HEIGHT), contentAlignment = Alignment.TopCenter) {
            if (record != null && record.losses > 0) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(barHeight(record.losses, unit))
                            .growFromTop(progress)
                            .background(PicklejotTheme.colors.streakFlame, RoundedCornerShape(BAR_CORNER)),
                )
            }
        }
        Text(
            text = shortName,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true),
        )
        Text(
            text = year,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true),
        )
    }
}

private fun barHeight(
    count: Int,
    unit: Int,
) = (HALF_HEIGHT * count / unit).coerceAtLeast(MIN_BAR_HEIGHT)
