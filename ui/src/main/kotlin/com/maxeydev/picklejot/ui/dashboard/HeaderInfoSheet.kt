@file:OptIn(ExperimentalMaterial3Api::class)

package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTextStyles
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val CHIP_MIN_WIDTH = 88.dp
private val CHIP_MIN_HEIGHT = 44.dp
private val DOT_SIZE = 12.dp
private val DOT_RING_WIDTH = 2.dp
private val FLAME_SIZE = 18.dp
private val FILTER_ICON_SIZE = 20.dp
private const val SAMPLE_WIN_PERCENT = 50
private const val SAMPLE_STREAK_WEEKS = 2
private val CHIP_FIGURE_STYLE = TextStyle(fontSize = 22.sp, lineHeight = 26.sp)

@Composable
fun HeaderInfoSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.testTag(DashboardTestTags.INFO_SHEET),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PicklejotSpacing.xl),
        ) {
            Text(
                text = stringResource(R.string.header_info_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.header_info_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = PicklejotSpacing.xs),
            )
            Column(modifier = Modifier.padding(top = PicklejotSpacing.sm)) {
                InfoItem(
                    title = stringResource(R.string.header_info_win_rate_title),
                    body = stringResource(R.string.header_info_win_rate_body),
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_win_percent_short, SAMPLE_WIN_PERCENT),
                        style = PicklejotTextStyles.display.merge(CHIP_FIGURE_STYLE),
                        color = PicklejotTheme.colors.headerAccent,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_win_rate_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = PicklejotTheme.colors.onHeaderMuted,
                    )
                }
                InfoDivider()
                InfoItem(
                    title = stringResource(R.string.header_info_record_title),
                    body = stringResource(R.string.header_info_record_body),
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_record_compact, 1, 1),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                InfoDivider()
                InfoItem(
                    title = stringResource(R.string.header_info_recent_title),
                    body = stringResource(R.string.header_info_recent_body),
                ) {
                    val dotColor = PicklejotTheme.colors.onHeaderMuted
                    Row(horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm)) {
                        Box(modifier = Modifier.size(DOT_SIZE).border(DOT_RING_WIDTH, dotColor, CircleShape))
                        Box(modifier = Modifier.size(DOT_SIZE).background(dotColor, CircleShape))
                    }
                }
                InfoDivider()
                InfoItem(
                    title = stringResource(R.string.header_info_streak_title),
                    body = stringResource(R.string.header_info_streak_body),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_flame),
                        contentDescription = null,
                        tint = PicklejotTheme.colors.streakFlame,
                        modifier = Modifier.size(FLAME_SIZE),
                    )
                    Text(
                        text = stringResource(R.string.dashboard_streak_compact, SAMPLE_STREAK_WEEKS),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            FiltersNote(modifier = Modifier.padding(top = PicklejotSpacing.sm))
            Button(
                onClick = onDismiss,
                shape = CircleShape,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(top = PicklejotSpacing.lg)
                        .testTag(DashboardTestTags.INFO_DISMISS),
            ) {
                Text(
                    text = stringResource(R.string.header_info_dismiss),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(PicklejotSpacing.xl))
        }
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(color = PicklejotTheme.colors.cardBorder)
}

@Composable
private fun InfoItem(
    title: String,
    body: String,
    sample: @Composable () -> Unit,
) {
    val colors = PicklejotTheme.colors
    val gradient =
        remember(colors.headerTop, colors.headerBottom) {
            Brush.verticalGradient(listOf(colors.headerTop, colors.headerBottom))
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = PicklejotSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier =
                Modifier
                    .defaultMinSize(minWidth = CHIP_MIN_WIDTH, minHeight = CHIP_MIN_HEIGHT)
                    .background(gradient, MaterialTheme.shapes.medium)
                    .padding(horizontal = PicklejotSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.xs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.onHeader) {
                sample()
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun FiltersNote(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)
                .padding(PicklejotSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_filter),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(FILTER_ICON_SIZE),
        )
        Text(
            text = stringResource(R.string.header_info_filters),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
