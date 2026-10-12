package com.maxeydev.picklejot.ui.streak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.streak.MissedSkipOpportunity
import com.maxeydev.picklejot.domain.streak.WeekKey
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.AnimatedMascot
import com.maxeydev.picklejot.ui.common.Mascot
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val MIN_TARGET = 48.dp
private val MASCOT_SIZE = 36.dp

@Composable
fun SkipUsedNotice(
    skippedWeek: WeekKey?,
    skipsHeld: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (skippedWeek == null) {
        return
    }
    NoticeSurface(
        mascot = Mascot.HEAD_JOY,
        onDismiss = onDismiss,
        dismissTag = StreakNoticeTestTags.SKIP_USED_DISMISS,
        modifier = modifier.testTag(StreakNoticeTestTags.SKIP_USED),
    ) {
        Text(
            text = stringResource(R.string.skip_used_message, weekOfLabel(skippedWeek)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            text = pluralStringResource(R.plurals.skip_used_remaining, skipsHeld, skipsHeld),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun MissedSkipNotice(
    opportunity: MissedSkipOpportunity?,
    onSeePro: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (opportunity == null) {
        return
    }
    val weeks = opportunity.brokenStreakWeeks
    NoticeSurface(
        mascot = Mascot.HEAD_OOPS,
        onDismiss = onDismiss,
        dismissTag = StreakNoticeTestTags.MISSED_SKIP_DISMISS,
        modifier = modifier.testTag(StreakNoticeTestTags.MISSED_SKIP),
    ) {
        Text(
            text =
                pluralStringResource(
                    R.plurals.missed_skip_message,
                    weeks,
                    weeks,
                    weekOfLabel(opportunity.missedWeek),
                ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        TextButton(
            onClick = onSeePro,
            modifier = Modifier.heightIn(min = MIN_TARGET).testTag(StreakNoticeTestTags.MISSED_SKIP_ACTION),
        ) {
            Text(stringResource(R.string.missed_skip_action))
        }
    }
}

@Composable
private fun NoticeSurface(
    mascot: Mascot,
    onDismiss: () -> Unit,
    dismissTag: String,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        ) {
            AnimatedMascot(mascot = mascot, modifier = Modifier.size(MASCOT_SIZE))
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                content()
            }
            IconButton(onClick = onDismiss, modifier = Modifier.testTag(dismissTag)) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.streak_notice_dismiss),
                )
            }
        }
    }
}

private fun weekOfLabel(week: WeekKey): String =
    DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(week.monday.toJavaLocalDate())
