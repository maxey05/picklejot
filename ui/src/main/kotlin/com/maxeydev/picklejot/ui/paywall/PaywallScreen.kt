package com.maxeydev.picklejot.ui.paywall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.entitlement.FreeTier
import com.maxeydev.picklejot.domain.entitlement.ProTier
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotTheme
import com.maxeydev.picklejot.ui.theme.StatusBarIcons

private val MINIMUM_TOUCH_TARGET = 48.dp
private const val LABEL_WEIGHT = 1.6f

private sealed interface PlanCell {
    data class Value(
        val text: String,
    ) : PlanCell

    data class Included(
        val included: Boolean,
    ) : PlanCell
}

private class PlanRow(
    val label: String,
    val free: PlanCell,
    val pro: PlanCell,
)

@Composable
fun PaywallScreen(
    state: PaywallUiState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onRetryPrice: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusBarIcons(useLightIcons = true)
    Scaffold(
        modifier = modifier.testTag(PaywallTestTags.PAYWALL),
        bottomBar = {
            PurchaseActions(state = state, onBuy = onBuy, onRestore = onRestore, onRetryPrice = onRetryPrice)
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            ProHeader(onClose = onClose)
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PlanTable()
                FreeUsage(savedMatches = state.savedMatches)
            }
        }
    }
}

@Composable
private fun ProHeader(onClose: () -> Unit) {
    Surface(
        color = PicklejotTheme.colors.header,
        contentColor = PicklejotTheme.colors.onHeader,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 28.dp)) {
            IconButton(onClick = onClose, modifier = Modifier.testTag(PaywallTestTags.PAYWALL_CLOSE)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.paywall_close),
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(PicklejotTheme.colors.headerPill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_crown),
                        contentDescription = null,
                        tint = PicklejotTheme.colors.onHeader,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.paywall_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.paywall_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PicklejotTheme.colors.onHeaderMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun planRows(): List<PlanRow> {
    val unlimited = stringResource(R.string.paywall_value_unlimited)
    return listOf(
        PlanRow(
            stringResource(R.string.paywall_row_matches),
            PlanCell.Value(FreeTier.MATCH_LIMIT.toString()),
            PlanCell.Value(unlimited),
        ),
        PlanRow(
            stringResource(R.string.paywall_row_photos),
            PlanCell.Value(FreeTier.PHOTOS_PER_MATCH.toString()),
            PlanCell.Value(ProTier.PHOTOS_PER_MATCH.toString()),
        ),
        PlanRow(stringResource(R.string.paywall_row_stats), PlanCell.Included(false), PlanCell.Included(true)),
        PlanRow(stringResource(R.string.paywall_row_insurance), PlanCell.Included(false), PlanCell.Included(true)),
        PlanRow(stringResource(R.string.paywall_row_card_styles), PlanCell.Included(false), PlanCell.Included(true)),
        PlanRow(stringResource(R.string.paywall_row_export), PlanCell.Included(true), PlanCell.Included(true)),
    )
}

@Composable
private fun PlanTable() {
    val rows = planRows()
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).clearAndSetSemantics { },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderCell(
                    stringResource(R.string.paywall_table_heading),
                    Modifier.weight(LABEL_WEIGHT),
                    TextAlign.Start,
                )
                HeaderCell(stringResource(R.string.paywall_column_free), Modifier.weight(1f), TextAlign.Center)
                HeaderCell(
                    stringResource(R.string.paywall_column_pro),
                    Modifier.weight(1f),
                    TextAlign.Center,
                    highlight = true,
                )
            }
            HorizontalDivider(color = PicklejotTheme.colors.cardBorder)
            rows.forEach { row -> PlanTableRow(row) }
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier,
    align: TextAlign,
    highlight: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        modifier = modifier,
    )
}

@Composable
private fun PlanTableRow(row: PlanRow) {
    val freeSpoken = spoken(row.free)
    val proSpoken = spoken(row.pro)
    val description = stringResource(R.string.paywall_row_description, row.label, freeSpoken, proSpoken)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp)
                .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = row.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(LABEL_WEIGHT))
        PlanCellView(cell = row.free, emphasised = false, modifier = Modifier.weight(1f))
        PlanCellView(cell = row.pro, emphasised = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun spoken(cell: PlanCell): String =
    when (cell) {
        is PlanCell.Value -> cell.text
        is PlanCell.Included ->
            if (cell.included) {
                stringResource(R.string.paywall_included)
            } else {
                stringResource(R.string.paywall_not_included)
            }
    }

@Composable
private fun PlanCellView(
    cell: PlanCell,
    emphasised: Boolean,
    modifier: Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (cell) {
            is PlanCell.Value ->
                Text(
                    text = cell.text,
                    style = if (emphasised) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
                    color =
                        if (emphasised) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    textAlign = TextAlign.Center,
                )
            is PlanCell.Included ->
                Icon(
                    painter = painterResource(if (cell.included) R.drawable.ic_check else R.drawable.ic_remove),
                    contentDescription = null,
                    tint =
                        if (cell.included) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.size(20.dp),
                )
        }
    }
}

@Composable
private fun FreeUsage(savedMatches: Int) {
    val used = savedMatches.coerceAtMost(FreeTier.MATCH_LIMIT)
    val fraction = used.toFloat() / FreeTier.MATCH_LIMIT
    val value = stringResource(R.string.paywall_usage_value, used, FreeTier.MATCH_LIMIT)
    Column(
        modifier = Modifier.testTag(PaywallTestTags.PAYWALL_RECORD).semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.paywall_usage_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(text = value, style = MaterialTheme.typography.titleSmall)
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PicklejotTheme.colors.cardBorder)
                    .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun PurchaseActions(
    state: PaywallUiState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onRetryPrice: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        state.message?.let { message ->
            Text(
                text = stringResource(message.text),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color =
                    if (message == StoreMessage.UNLOCKED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag(PaywallTestTags.STORE_MESSAGE)
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Button(
            onClick = onBuy,
            enabled = state.canBuy,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(PaywallTestTags.PAYWALL_BUY),
        ) {
            Text(
                state.price?.let { stringResource(R.string.paywall_buy_with_price, it) }
                    ?: stringResource(R.string.paywall_buy),
            )
        }
        if (state.price == null && !state.isLoadingPrice) {
            TextButton(
                onClick = onRetryPrice,
                modifier = Modifier.heightIn(min = MINIMUM_TOUCH_TARGET).testTag(PaywallTestTags.PAYWALL_PRICE_TERMS),
            ) {
                Text(stringResource(R.string.paywall_retry_price))
            }
        }
        TextButton(
            onClick = onRestore,
            enabled = !state.isWorking,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = MINIMUM_TOUCH_TARGET)
                    .testTag(PaywallTestTags.PAYWALL_RESTORE),
        ) {
            Text(stringResource(R.string.paywall_restore))
        }
    }
}
