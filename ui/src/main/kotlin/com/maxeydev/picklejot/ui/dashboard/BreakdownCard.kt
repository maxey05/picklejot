package com.maxeydev.picklejot.ui.dashboard

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.stats.percentIfEnoughMatches
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private const val COLLAPSED_BREAKDOWN_ROWS = 5
private const val FILL_MILLIS = 760
private const val FILL_STAGGER_MILLIS = 90
private const val FILL_START_MILLIS = 150
private const val PERCENT_SCALE = 100f
private val TRACK_HEIGHT = 6.dp

@Composable
internal fun BreakdownCard(
    title: String,
    revealKey: String,
    group: String,
    emptyText: String,
    rows: List<BreakdownRow>,
    modifier: Modifier = Modifier,
) {
    val reveal = rememberSectionReveal(revealKey)
    ProCard(reveal = reveal, modifier = modifier) {
        ProCardHeader(title = title)
        if (rows.isEmpty()) {
            EmptyNote(text = emptyText, testTag = DashboardTestTags.advancedEmpty(group))
        } else {
            ExpandableList(group = group, total = rows.size, collapsedCount = COLLAPSED_BREAKDOWN_ROWS) { visible ->
                rows.take(visible).forEachIndexed { index, row ->
                    BreakdownRowView(
                        row = row,
                        revealed = reveal.revealed,
                        index = index,
                        testTag = DashboardTestTags.advancedRow(group, index),
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownRowView(
    row: BreakdownRow,
    revealed: Boolean,
    index: Int,
    testTag: String,
) {
    val progress = rememberRevealProgress(revealed, FILL_MILLIS, FILL_START_MILLIS + index * FILL_STAGGER_MILLIS)
    val percent = row.record.percentIfEnoughMatches()
    val fraction = ((percent ?: 0) / PERCENT_SCALE * progress.value).coerceIn(0f, 1f)

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
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_ROW_HEIGHT)
                    .padding(vertical = PicklejotSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = breakdownSupporting(row),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = breakdownValue(row),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(TRACK_HEIGHT)
                        .clip(RoundedCornerShape(TRACK_HEIGHT / 2))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
