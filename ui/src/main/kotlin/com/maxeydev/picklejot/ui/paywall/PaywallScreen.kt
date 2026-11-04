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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.entitlement.FreeTier
import com.maxeydev.picklejot.domain.entitlement.ProTier
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.settings.SettingsCard
import com.maxeydev.picklejot.ui.settings.SettingsRow
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val MINIMUM_TOUCH_TARGET = 48.dp
private val HERO_SHAPE = RoundedCornerShape(20.dp)
private val HERO_BADGE_SHAPE = RoundedCornerShape(18.dp)

private sealed interface Benefit {
    data class Pill(
        val text: String,
    ) : Benefit

    data object ProOnly : Benefit

    data object Both : Benefit
}

private class BenefitRow(
    val icon: Int,
    val label: String,
    val supporting: String,
    val benefit: Benefit,
    val freeSpoken: String,
    val proSpoken: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    state: PaywallUiState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(PaywallTestTags.PAYWALL),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.paywall_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag(PaywallTestTags.PAYWALL_CLOSE)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.paywall_close),
                        )
                    }
                },
            )
        },
        bottomBar = {
            PurchaseActions(state = state, onBuy = onBuy, onRestore = onRestore)
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProHero(savedMatches = state.savedMatches)
            Text(
                text = stringResource(R.string.paywall_table_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp).semantics { heading() },
            )
            BenefitsCard()
        }
    }
}

@Composable
private fun ProHero(savedMatches: Int) {
    val colors = MaterialTheme.colorScheme
    val used = savedMatches.coerceAtMost(FreeTier.MATCH_LIMIT)
    val fraction = used.toFloat() / FreeTier.MATCH_LIMIT
    val value = stringResource(R.string.paywall_usage_value, used, FreeTier.MATCH_LIMIT)
    Surface(
        shape = HERO_SHAPE,
        color = colors.primaryContainer,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(HERO_BADGE_SHAPE).background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_crown),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(R.string.paywall_hero_heading),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.paywall_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .testTag(PaywallTestTags.PAYWALL_RECORD)
                        .semantics(mergeDescendants = true) { },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.paywall_usage_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onPrimaryContainer,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.onPrimaryContainer.copy(alpha = 0.18f))
                            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.primary),
                    )
                }
            }
        }
    }
}

@Composable
private fun benefitRows(): List<BenefitRow> {
    val unlimited = stringResource(R.string.paywall_value_unlimited)
    val included = stringResource(R.string.paywall_included)
    val notIncluded = stringResource(R.string.paywall_not_included)
    val proOnly = stringResource(R.string.paywall_pro_only)
    val freeToo = stringResource(R.string.paywall_free_too)
    val freeMatches = FreeTier.MATCH_LIMIT.toString()
    val freePhotos = FreeTier.PHOTOS_PER_MATCH.toString()
    val proPhotos = ProTier.PHOTOS_PER_MATCH.toString()
    return listOf(
        BenefitRow(
            R.drawable.ic_scoreboard,
            stringResource(R.string.paywall_row_matches),
            stringResource(R.string.paywall_free_value, freeMatches),
            Benefit.Pill(unlimited),
            freeMatches,
            unlimited,
        ),
        BenefitRow(
            R.drawable.ic_photo_camera,
            stringResource(R.string.paywall_row_photos),
            stringResource(R.string.paywall_free_value, freePhotos),
            Benefit.Pill(proPhotos),
            freePhotos,
            proPhotos,
        ),
        BenefitRow(
            R.drawable.ic_trophy,
            stringResource(R.string.paywall_row_stats),
            proOnly,
            Benefit.ProOnly,
            notIncluded,
            included,
        ),
        BenefitRow(
            R.drawable.ic_shield,
            stringResource(R.string.paywall_row_insurance),
            proOnly,
            Benefit.ProOnly,
            notIncluded,
            included,
        ),
        BenefitRow(
            R.drawable.ic_star,
            stringResource(R.string.paywall_row_card_styles),
            proOnly,
            Benefit.ProOnly,
            notIncluded,
            included,
        ),
        BenefitRow(
            R.drawable.ic_download,
            stringResource(R.string.paywall_row_export),
            freeToo,
            Benefit.Both,
            included,
            included,
        ),
    )
}

@Composable
private fun BenefitsCard() {
    val rows = benefitRows()
    SettingsCard {
        rows.forEachIndexed { index, row ->
            val description =
                stringResource(R.string.paywall_row_description, row.label, row.freeSpoken, row.proSpoken)
            SettingsRow(
                icon = row.icon,
                label = row.label,
                supporting = row.supporting,
                showDivider = index > 0,
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                trailing = { BenefitMark(row.benefit) },
            )
        }
    }
}

@Composable
private fun BenefitMark(benefit: Benefit) {
    when (benefit) {
        is Benefit.Pill ->
            Surface(shape = CircleShape, color = PicklejotTheme.colors.countBadge) {
                Text(
                    text = benefit.text,
                    style = MaterialTheme.typography.labelMedium,
                    color = PicklejotTheme.colors.onCountBadge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        Benefit.ProOnly ->
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        Benefit.Both ->
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
    }
}

@Composable
private fun PurchaseActions(
    state: PaywallUiState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
) {
    Column {
        HorizontalDivider(color = PicklejotTheme.colors.cardBorder)
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
}
