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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.stats.PersonRecord
import com.maxeydev.picklejot.domain.stats.ProInsights
import com.maxeydev.picklejot.domain.stats.WinLoss
import com.maxeydev.picklejot.domain.stats.percentIfEnoughMatches
import com.maxeydev.picklejot.domain.stats.pointsAbove
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTheme
import kotlin.uuid.ExperimentalUuidApi

private const val COLLAPSED_RIVALS = 5
private const val ROW_MILLIS = 520
private const val ROW_STAGGER_MILLIS = 80
private const val BAR_MILLIS = 760
private const val BAR_DELAY_MILLIS = 250
private val RECORD_WIDTH = 52.dp
private val WIN_WIDTH = 48.dp
private val VERSUS_WIDTH = 52.dp
private val SPLIT_BAR_HEIGHT = 4.dp

private data class VersusRow(
    val name: String,
    val record: WinLoss,
)

@Composable
internal fun RivalsTable(
    rivals: List<PersonRecord>,
    insights: ProInsights,
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    val reveal = rememberSectionReveal("pro_rivals")
    val unknownPerson = stringResource(R.string.stats_advanced_unknown_person)
    val rows = rivals.map { VersusRow(name = state.nameOf(it.personId) ?: unknownPerson, record = it.record) }
    val group = DashboardTestTags.GROUP_HEAD_TO_HEAD

    ProCard(reveal = reveal, modifier = modifier) {
        ProCardHeader(title = stringResource(R.string.stats_pro_rivals_title))
        if (rows.isEmpty()) {
            EmptyNote(
                text = stringResource(R.string.stats_advanced_no_opponents),
                testTag = DashboardTestTags.advancedEmpty(group),
            )
        } else {
            ExpandableList(group = group, total = rows.size, collapsedCount = COLLAPSED_RIVALS) { visible ->
                val stacked = isLargeFont()
                if (!stacked) {
                    RivalsHeader()
                }
                rows.take(visible).forEachIndexed { index, row ->
                    RivalRow(
                        row = row,
                        baseline = insights.overall,
                        revealed = reveal.revealed,
                        index = index,
                        stacked = stacked,
                        testTag = DashboardTestTags.advancedRow(group, index),
                    )
                }
            }
            Text(
                text = stringResource(R.string.stats_pro_rivals_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RivalsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
    ) {
        HeaderCell(stringResource(R.string.stats_pro_col_opponent), Modifier.weight(1f), TextAlign.Start)
        HeaderCell(stringResource(R.string.stats_pro_col_record), Modifier.width(RECORD_WIDTH), TextAlign.End)
        HeaderCell(stringResource(R.string.stats_pro_col_win), Modifier.width(WIN_WIDTH), TextAlign.End)
        HeaderCell(stringResource(R.string.stats_pro_col_versus), Modifier.width(VERSUS_WIDTH), TextAlign.End)
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier,
    align: TextAlign,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun RivalRow(
    row: VersusRow,
    baseline: WinLoss,
    revealed: Boolean,
    index: Int,
    stacked: Boolean,
    testTag: String,
) {
    val slide = rememberRevealProgress(revealed, ROW_MILLIS, index * ROW_STAGGER_MILLIS)
    val split = rememberRevealProgress(revealed, BAR_MILLIS, BAR_DELAY_MILLIS + index * ROW_STAGGER_MILLIS)
    val percent = row.record.percentIfEnoughMatches()
    val delta = row.record.pointsAbove(baseline)
    val recordText = stringResource(R.string.stats_split_record, row.record.wins, row.record.losses)
    val percentText =
        percent?.let { stringResource(R.string.stats_percent, it) } ?: stringResource(R.string.stats_no_value)
    val deltaText =
        delta?.let { stringResource(R.string.stats_pro_delta_signed, it) } ?: stringResource(R.string.stats_no_value)
    val isGain = delta != null && delta >= 0
    val deltaColor = if (isGain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .semantics(mergeDescendants = true) {}
                .slideInFromEnd(slide),
    ) {
        if (index > 0) {
            HorizontalDivider(color = PicklejotTheme.colors.cardBorder)
        }
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = PicklejotSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.xs),
            ) {
                NameCell(row = row, percent = percent, split = split)
                Row(horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.md)) {
                    Text(text = recordText, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = deltaText, style = MaterialTheme.typography.bodyMedium, color = deltaColor)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = MIN_ROW_HEIGHT),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    NameCell(row = row, percent = percent, split = split)
                }
                Text(
                    text = recordText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(RECORD_WIDTH),
                )
                Text(
                    text = percentText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(WIN_WIDTH),
                )
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = deltaColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(VERSUS_WIDTH),
                )
            }
        }
    }
}

@Composable
private fun NameCell(
    row: VersusRow,
    percent: Int?,
    split: Animatable<Float, AnimationVector1D>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.xs)) {
        Text(
            text = row.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (percent == null) {
            Text(
                text = stringResource(R.string.stats_advanced_small_sample),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SplitBar(record = row.record, progress = split)
        }
    }
}

@Composable
private fun SplitBar(
    record: WinLoss,
    progress: Animatable<Float, AnimationVector1D>,
) {
    val total = record.total.coerceAtLeast(1).toFloat()
    val winShare = (record.wins / total * progress.value).coerceIn(0f, 1f)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(SPLIT_BAR_HEIGHT)
                .clip(RoundedCornerShape(SPLIT_BAR_HEIGHT / 2)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(winShare)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(PicklejotTheme.colors.lossBadge),
        )
    }
}
