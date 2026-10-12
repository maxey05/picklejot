@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.stats.PersonRecord
import com.maxeydev.picklejot.domain.stats.ProInsights
import com.maxeydev.picklejot.domain.stats.WinLoss
import com.maxeydev.picklejot.domain.stats.pointsAbove
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTheme
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi

private const val COLLAPSED_PARTNERS = 5
private const val MIN_SCALE_POINTS = 10
private const val BAR_MILLIS = 760
private const val BAR_STAGGER_MILLIS = 90
private const val BAR_START_MILLIS = 150
private val BAR_WIDTH = 120.dp
private val DELTA_WIDTH = 44.dp
private val DIVERGING_HEIGHT = 14.dp
private val BAR_CORNER = 3.dp
private val CENTER_LINE_WIDTH = 1.5.dp

private data class PartnerRow(
    val name: String,
    val record: WinLoss,
    val delta: Int?,
)

@Composable
internal fun PartnerChemistry(
    partners: List<PersonRecord>,
    insights: ProInsights,
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    val reveal = rememberSectionReveal("pro_partners")
    val unknownPerson = stringResource(R.string.stats_advanced_unknown_person)
    val baseline = insights.doubles
    val rows =
        partners.map {
            PartnerRow(
                name = state.nameOf(it.personId) ?: unknownPerson,
                record = it.record,
                delta = it.record.pointsAbove(baseline),
            )
        }
    val scale = maxOf(MIN_SCALE_POINTS, rows.maxOfOrNull { abs(it.delta ?: 0) } ?: 0)
    val group = DashboardTestTags.GROUP_PARTNER

    ProCard(reveal = reveal, modifier = modifier) {
        ProCardHeader(
            title = stringResource(R.string.stats_pro_partners_title),
            subtitle = baseline.winPercent?.let { stringResource(R.string.stats_pro_partners_subtitle, it) },
        )
        if (rows.isEmpty()) {
            EmptyNote(
                text = stringResource(R.string.stats_advanced_no_partners),
                testTag = DashboardTestTags.advancedEmpty(group),
            )
        } else {
            ExpandableList(group = group, total = rows.size, collapsedCount = COLLAPSED_PARTNERS) { visible ->
                val stacked = isLargeFont()
                rows.take(visible).forEachIndexed { index, row ->
                    PartnerRowView(
                        row = row,
                        scale = scale,
                        revealed = reveal.revealed,
                        index = index,
                        stacked = stacked,
                        testTag = DashboardTestTags.advancedRow(group, index),
                    )
                }
            }
            Text(
                text = stringResource(R.string.stats_pro_partners_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PartnerRowView(
    row: PartnerRow,
    scale: Int,
    revealed: Boolean,
    index: Int,
    stacked: Boolean,
    testTag: String,
) {
    val progress = rememberRevealProgress(revealed, BAR_MILLIS, BAR_START_MILLIS + index * BAR_STAGGER_MILLIS)
    val deltaText =
        row.delta?.let { stringResource(R.string.stats_pro_delta_signed, it) }
            ?: stringResource(R.string.stats_no_value)
    val supporting =
        if (row.delta == null) {
            stringResource(R.string.stats_advanced_small_sample)
        } else {
            stringResource(R.string.stats_split_record, row.record.wins, row.record.losses)
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .semantics(mergeDescendants = true) {},
    ) {
        if (index > 0) {
            HorizontalDivider(color = PicklejotTheme.colors.cardBorder)
        }
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = PicklejotSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.xs),
            ) {
                PartnerName(name = row.name, supporting = supporting)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
                ) {
                    DivergingBar(delta = row.delta, scale = scale, progress = progress, modifier = Modifier.weight(1f))
                    DeltaText(text = deltaText)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = MIN_ROW_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    PartnerName(name = row.name, supporting = supporting)
                }
                DivergingBar(
                    delta = row.delta,
                    scale = scale,
                    progress = progress,
                    modifier = Modifier.width(BAR_WIDTH),
                )
                DeltaText(text = deltaText)
            }
        }
    }
}

@Composable
private fun PartnerName(
    name: String,
    supporting: String,
) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeltaText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
        modifier = Modifier.width(DELTA_WIDTH),
    )
}

@Composable
private fun DivergingBar(
    delta: Int?,
    scale: Int,
    progress: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier,
) {
    val share = if (delta == null) 0f else (abs(delta).toFloat() / scale * progress.value).coerceIn(0f, 1f)
    val isPositive = delta != null && delta >= 0
    val isNegative = delta != null && delta < 0
    Row(modifier = modifier.height(DIVERGING_HEIGHT)) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
            if (isNegative) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(share)
                            .fillMaxHeight()
                            .background(
                                PicklejotTheme.colors.streakFlame,
                                RoundedCornerShape(topStart = BAR_CORNER, bottomStart = BAR_CORNER),
                            ),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .width(CENTER_LINE_WIDTH)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
        )
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            if (isPositive) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(share)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(topEnd = BAR_CORNER, bottomEnd = BAR_CORNER),
                            ),
                )
            }
        }
    }
}
