package com.maxeydev.picklejot.ui.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxeydev.picklejot.domain.stats.WinLoss
import com.maxeydev.picklejot.domain.streak.StreakInsurance
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.AnimatedMascot
import com.maxeydev.picklejot.ui.common.Mascot
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTextStyles
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val ROW_ICON_SIZE = 24.dp
private val PRO_ICON_SIZE = 20.dp
private val ROW_DIVIDER_INSET = 48.dp
private val PRO_ROW_HEIGHT = 56.dp
private const val PERCENT_SCALE = 100f
private val BAR_WIDTH = 96.dp
private val BAR_HEIGHT = 8.dp
private val PERCENT_MIN_WIDTH = 48.dp
private val EMPTY_ROW_MASCOT_SIZE = 36.dp
private const val STACKED_FONT_SCALE = 1.3f
internal val MIN_ROW_HEIGHT = 64.dp
private val STREAK_VALUE_STYLE = PicklejotTextStyles.hero.copy(fontSize = 24.sp, lineHeight = 28.sp)

private val PRO_SECTIONS =
    listOf(
        R.string.stats_pro_head_to_head,
        R.string.stats_pro_partner,
        R.string.stats_pro_location,
        R.string.stats_pro_monthly,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedStatsScreen(
    state: DashboardUiState,
    onBack: () -> Unit,
    onSeePro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(DashboardTestTags.STATS_SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.stats_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (state.isPro) {
                        ProBadge(modifier = Modifier.padding(end = PicklejotSpacing.gutter))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!state.isLoading) {
            val scrollState = rememberScrollState()
            val reveal = remember(scrollState) { RevealState(scrollState) }
            CompositionLocalProvider(LocalRevealState provides reveal) {
                Column(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .trackViewport(reveal)
                            .verticalScroll(scrollState)
                            .padding(
                                start = PicklejotSpacing.gutter,
                                end = PicklejotSpacing.gutter,
                                top = PicklejotSpacing.xs,
                                bottom = PicklejotSpacing.xxl,
                            ),
                    verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.xl),
                ) {
                    FilterIndicator(filter = state.filter, opponentName = state.filteredOpponentName)
                    RecordSection(state)
                    StreakSection(state)
                    FormatSection(state)
                    if (state.isPro) {
                        AdvancedStatsSection(state)
                    } else {
                        LockedPreview(state = state, onSeePro = onSeePro)
                        LockedProSection(onSeePro = onSeePro)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = PicklejotSpacing.xs).semantics { heading() },
    )
}

@Composable
internal fun StatsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
private fun ProBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.stats_pro_tag),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = PicklejotSpacing.md, vertical = PicklejotSpacing.xs),
        )
    }
}

@Composable
internal fun StatsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm)) {
        SectionTitle(title)
        content()
    }
}

@Composable
internal fun StatRow(
    label: String,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    showDivider: Boolean = false,
    @DrawableRes icon: Int? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(testTag)
                .semantics(mergeDescendants = true) {},
    ) {
        if (showDivider) {
            HorizontalDivider(
                color = PicklejotTheme.colors.cardBorder,
                modifier = Modifier.padding(start = PicklejotSpacing.lg),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_ROW_HEIGHT)
                    .padding(horizontal = PicklejotSpacing.lg, vertical = PicklejotSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.md),
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(ROW_ICON_SIZE),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                supporting?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(text = value, style = valueStyle)
        }
    }
}

@Composable
internal fun percentText(record: WinLoss): String =
    record.winPercent?.let { stringResource(R.string.stats_percent, it) } ?: stringResource(R.string.stats_no_value)

private class RecordCell(
    val value: String,
    val label: String,
    val testTag: String,
    val isAccent: Boolean = false,
)

@Composable
private fun RecordSection(state: DashboardUiState) {
    val overall = state.stats.overall
    val cells =
        listOf(
            RecordCell(
                value = overall.total.toString(),
                label = stringResource(R.string.stats_matches),
                testTag = DashboardTestTags.STATS_MATCHES,
            ),
            RecordCell(
                value = stringResource(R.string.stats_split_record, overall.wins, overall.losses),
                label = stringResource(R.string.stats_wins_losses),
                testTag = DashboardTestTags.STATS_RECORD,
            ),
            RecordCell(
                value = percentText(overall),
                label = stringResource(R.string.stats_win_rate),
                testTag = DashboardTestTags.STATS_WIN_RATE,
                isAccent = true,
            ),
        )
    val isStacked = LocalDensity.current.fontScale > STACKED_FONT_SCALE
    StatsGroup(title = stringResource(R.string.stats_record)) {
        StatsCard {
            if (isStacked) {
                cells.forEachIndexed { index, cell ->
                    if (index > 0) {
                        HorizontalDivider(color = PicklejotTheme.colors.cardBorder)
                    }
                    RecordCellView(cell = cell, modifier = Modifier.fillMaxWidth())
                }
            } else {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    cells.forEachIndexed { index, cell ->
                        if (index > 0) {
                            VerticalDivider(color = PicklejotTheme.colors.cardBorder)
                        }
                        RecordCellView(cell = cell, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCellView(
    cell: RecordCell,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .testTag(cell.testTag)
                .semantics(mergeDescendants = true) {}
                .padding(PicklejotSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = cell.value,
            style = PicklejotTextStyles.hero,
            color = if (cell.isAccent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = cell.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun weeksText(weeks: Int): String =
    if (weeks > 0) {
        pluralStringResource(R.plurals.stats_weeks, weeks, weeks)
    } else {
        stringResource(R.string.stats_no_streak)
    }

@Composable
private fun StreakSection(state: DashboardUiState) {
    val streak = state.streak
    StatsGroup(title = stringResource(R.string.stats_streaks)) {
        StatsCard {
            if (streak.isCurrentTheLongest) {
                StatRow(
                    label = stringResource(R.string.stats_current_streak),
                    value = weeksText(streak.current),
                    supporting = stringResource(R.string.stats_current_is_longest),
                    testTag = DashboardTestTags.STATS_CURRENT_IS_LONGEST,
                    icon = R.drawable.ic_flame,
                    iconTint = PicklejotTheme.colors.streakFlame,
                    valueStyle = STREAK_VALUE_STYLE,
                )
            } else {
                StatRow(
                    label = stringResource(R.string.stats_current_streak),
                    value = weeksText(streak.current),
                    testTag = DashboardTestTags.STATS_CURRENT_STREAK,
                    icon = R.drawable.ic_flame,
                    iconTint = PicklejotTheme.colors.streakFlame,
                    valueStyle = STREAK_VALUE_STYLE,
                )
                StatRow(
                    label = stringResource(R.string.stats_longest_streak),
                    value = weeksText(streak.longest),
                    testTag = DashboardTestTags.STATS_LONGEST_STREAK,
                    showDivider = true,
                    icon = R.drawable.ic_trophy,
                    valueStyle = STREAK_VALUE_STYLE,
                )
            }
            if (state.isPro) {
                StatRow(
                    label = stringResource(R.string.stats_skips_label),
                    value =
                        if (state.skipsHeld > 0) {
                            pluralStringResource(R.plurals.stats_skips_held, state.skipsHeld, state.skipsHeld)
                        } else {
                            stringResource(R.string.stats_skips_none)
                        },
                    supporting = stringResource(R.string.stats_skips_explainer, StreakInsurance.MAX_HELD),
                    testTag = DashboardTestTags.STATS_SKIPS_HELD,
                    showDivider = true,
                    icon = R.drawable.ic_shield,
                )
            }
        }
    }
}

@Composable
private fun FormatSection(state: DashboardUiState) {
    StatsGroup(title = stringResource(R.string.stats_by_format)) {
        StatsCard {
            SplitRow(
                label = stringResource(R.string.format_singles),
                record = state.stats.singles,
                testTag = DashboardTestTags.STATS_SINGLES,
            )
            SplitRow(
                label = stringResource(R.string.format_doubles),
                record = state.stats.doubles,
                testTag = DashboardTestTags.STATS_DOUBLES,
                showDivider = true,
            )
        }
    }
}

@Composable
private fun SplitRow(
    label: String,
    record: WinLoss,
    testTag: String,
    showDivider: Boolean = false,
) {
    val fraction = (record.winPercent ?: 0) / PERCENT_SCALE
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .semantics(mergeDescendants = true) {},
    ) {
        if (showDivider) {
            HorizontalDivider(
                color = PicklejotTheme.colors.cardBorder,
                modifier = Modifier.padding(start = PicklejotSpacing.lg),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_ROW_HEIGHT)
                    .padding(horizontal = PicklejotSpacing.lg, vertical = PicklejotSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.md),
        ) {
            if (!record.hasMatches) {
                AnimatedMascot(mascot = Mascot.HEAD_SMILE, modifier = Modifier.size(EMPTY_ROW_MASCOT_SIZE))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        if (record.hasMatches) {
                            stringResource(R.string.stats_split_record, record.wins, record.losses)
                        } else {
                            stringResource(R.string.stats_split_empty)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier =
                    Modifier
                        .width(BAR_WIDTH)
                        .height(BAR_HEIGHT)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(MaterialTheme.colorScheme.primary),
                )
            }
            Text(
                text = percentText(record),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = PERCENT_MIN_WIDTH),
            )
        }
    }
}

@Composable
private fun LockedProSection(onSeePro: () -> Unit) {
    StatsGroup(
        title = stringResource(R.string.stats_pro_title),
        modifier = Modifier.testTag(DashboardTestTags.STATS_PRO_SECTION),
    ) {
        StatsCard {
            PRO_SECTIONS.forEachIndexed { index, title ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.stats_preview_cta),
                                onClick = onSeePro,
                            ).testTag(DashboardTestTags.proRow(index))
                            .semantics(mergeDescendants = true) {},
                ) {
                    if (index > 0) {
                        HorizontalDivider(
                            color = PicklejotTheme.colors.cardBorder,
                            modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                        )
                    }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = PRO_ROW_HEIGHT)
                                .padding(horizontal = PicklejotSpacing.lg, vertical = PicklejotSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.md),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(PRO_ICON_SIZE),
                        )
                        Text(
                            text = stringResource(title),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.stats_pro_tag),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = PicklejotSpacing.sm, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
