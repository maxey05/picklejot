package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.stats.ProInsights
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTextStyles
import kotlin.math.roundToInt

private const val COUNT_MILLIS = 900
private const val TILE_STAGGER_MILLIS = 90
private const val ENTER_SHARE = 2.5f
private val TILE_RISE = 14.dp
private const val TILE_START_SCALE = 0.97f

private class KeyTileModel(
    val label: String,
    val value: String,
    val support: String,
    val progress: Animatable<Float, AnimationVector1D>,
    val testTag: String,
    val valueColor: Color,
)

@Composable
internal fun ProKeyNumbers(
    insights: ProInsights,
    modifier: Modifier = Modifier,
) {
    val reveal = rememberSectionReveal("pro_key_numbers")
    val trendProgress = rememberRevealProgress(reveal.revealed, COUNT_MILLIS)
    val weekProgress = rememberRevealProgress(reveal.revealed, COUNT_MILLIS, TILE_STAGGER_MILLIS)
    val daysProgress = rememberRevealProgress(reveal.revealed, COUNT_MILLIS, TILE_STAGGER_MILLIS * 2)
    val lastTenProgress = rememberRevealProgress(reveal.revealed, COUNT_MILLIS, TILE_STAGGER_MILLIS * 3)

    val trend = insights.trendPoints
    val trendValue =
        if (trend == null) {
            stringResource(R.string.stats_no_value)
        } else {
            stringResource(R.string.stats_pro_points_signed, (trend * trendProgress.value).roundToInt())
        }
    val lastTen = insights.lastTen
    val lastTenValue =
        stringResource(
            R.string.stats_split_record,
            (lastTen.wins * lastTenProgress.value).roundToInt(),
            (lastTen.losses * lastTenProgress.value).roundToInt(),
        )
    val primary = MaterialTheme.colorScheme.primary
    val plain = MaterialTheme.colorScheme.onSurface

    val tiles =
        listOf(
            KeyTileModel(
                label = stringResource(R.string.stats_pro_trend_label),
                value = trendValue,
                support =
                    if (trend == null) {
                        stringResource(R.string.stats_pro_trend_unknown)
                    } else {
                        stringResource(R.string.stats_pro_trend_support)
                    },
                progress = trendProgress,
                testTag = DashboardTestTags.STATS_PRO_TREND,
                valueColor = if (trend != null && trend > 0) primary else plain,
            ),
            KeyTileModel(
                label = stringResource(R.string.stats_pro_per_week_label),
                value = stringResource(R.string.stats_pro_per_week_value, insights.matchesPerWeek * weekProgress.value),
                support = stringResource(R.string.stats_pro_per_week_support),
                progress = weekProgress,
                testTag = DashboardTestTags.STATS_PRO_PER_WEEK,
                valueColor = plain,
            ),
            KeyTileModel(
                label = stringResource(R.string.stats_pro_days_label),
                value = (insights.daysPlayed * daysProgress.value).roundToInt().toString(),
                support =
                    pluralStringResource(
                        R.plurals.stats_pro_longest_run,
                        insights.longestDayRun,
                        insights.longestDayRun,
                    ),
                progress = daysProgress,
                testTag = DashboardTestTags.STATS_PRO_DAYS,
                valueColor = plain,
            ),
            KeyTileModel(
                label = stringResource(R.string.stats_pro_last_ten_label),
                value = lastTenValue,
                support = stringResource(R.string.stats_pro_last_ten_support, lastTen.winPercent ?: 0),
                progress = lastTenProgress,
                testTag = DashboardTestTags.STATS_PRO_LAST_TEN,
                valueColor = plain,
            ),
        )

    Column(
        modifier = modifier.then(reveal.track),
        verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
    ) {
        if (isLargeFont()) {
            tiles.forEach { KeyTile(model = it, modifier = Modifier.fillMaxWidth()) }
        } else {
            tiles.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
                ) {
                    pair.forEach { KeyTile(model = it, modifier = Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}

@Composable
private fun KeyTile(
    model: KeyTileModel,
    modifier: Modifier = Modifier,
) {
    StatsCard(
        modifier =
            modifier.graphicsLayer {
                val enter = (model.progress.value * ENTER_SHARE).coerceIn(0f, 1f)
                alpha = enter
                translationY = (1f - enter) * TILE_RISE.toPx()
                val scale = TILE_START_SCALE + (1f - TILE_START_SCALE) * enter
                scaleX = scale
                scaleY = scale
            },
    ) {
        Column(
            modifier =
                Modifier
                    .testTag(model.testTag)
                    .semantics(mergeDescendants = true) {}
                    .padding(PicklejotSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = model.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = model.value, style = PicklejotTextStyles.hero, color = model.valueColor)
            Text(
                text = model.support,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
