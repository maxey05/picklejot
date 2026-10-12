@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.dashboard

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTextStyles
import com.maxeydev.picklejot.ui.theme.PicklejotTheme
import kotlin.uuid.ExperimentalUuidApi

private const val MASK_ALPHA = 0.28f
private const val STACKED_FONT_SCALE = 1.3f
private val MASK_WIDTH = 72.dp
private val MASK_HEIGHT = 20.dp
private val BLUR_RADIUS = 7.dp
private val CTA_ICON_SIZE = 20.dp
private val CTA_HEIGHT = 40.dp

@Composable
fun LockedPreview(
    state: DashboardUiState,
    onSeePro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = state.advanced.topOpponent ?: return
    val name = state.nameOf(top.personId) ?: return
    val description = stringResource(R.string.stats_preview_a11y, name)
    StatsGroup(title = stringResource(R.string.stats_advanced_head_to_head), modifier = modifier) {
        StatsCard(
            modifier =
                Modifier
                    .clip(MaterialTheme.shapes.large)
                    .clickable(role = Role.Button, onClick = onSeePro)
                    .testTag(DashboardTestTags.STATS_LOCKED_PREVIEW)
                    .semantics(mergeDescendants = true) { contentDescription = description },
        ) {
            val isStacked = LocalDensity.current.fontScale >= STACKED_FONT_SCALE
            if (isStacked) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(PicklejotSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.md),
                ) {
                    PreviewText(name = name)
                    UnlockPill()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(PicklejotSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.md),
                ) {
                    PreviewText(name = name, modifier = Modifier.weight(1f))
                    UnlockPill()
                }
            }
        }
    }
}

@Composable
private fun PreviewText(
    name: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.xs)) {
        Text(
            text = stringResource(R.string.stats_preview_title, name),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag(DashboardTestTags.STATS_LOCKED_PREVIEW_NAME),
        )
        HiddenRecord()
    }
}

@Composable
private fun HiddenRecord() {
    val tag = Modifier.testTag(DashboardTestTags.STATS_LOCKED_PREVIEW_RECORD).clearAndSetSemantics {}
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Text(
            text = stringResource(R.string.stats_preview_sample),
            style = PicklejotTextStyles.display,
            modifier = tag.blur(BLUR_RADIUS, BlurredEdgeTreatment.Unbounded),
        )
    } else {
        Box(
            modifier =
                tag
                    .size(width = MASK_WIDTH, height = MASK_HEIGHT)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = MASK_ALPHA)),
        )
    }
}

@Composable
private fun UnlockPill() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
    ) {
        Row(
            modifier = Modifier.heightIn(min = CTA_HEIGHT).padding(horizontal = PicklejotSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                modifier = Modifier.size(CTA_ICON_SIZE),
            )
            Text(text = stringResource(R.string.stats_preview_cta), style = MaterialTheme.typography.labelLarge)
        }
    }
}
