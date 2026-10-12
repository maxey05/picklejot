package com.maxeydev.picklejot.ui.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.entitlement.CapWarning
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.AnimatedMascot
import com.maxeydev.picklejot.ui.common.Mascot

private val MASCOT_SIZE = 32.dp

@Composable
fun CapWarningBanner(
    warning: CapWarning,
    remainingFreeMatches: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (warning == CapWarning.NONE) {
        return
    }
    val isImminent = warning == CapWarning.IMMINENT
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (isImminent) colors.errorContainer else colors.secondaryContainer,
        contentColor = if (isImminent) colors.onErrorContainer else colors.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().testTag(PaywallTestTags.CAP_BANNER),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        ) {
            if (!isImminent) {
                AnimatedMascot(mascot = Mascot.HEAD_SMILE, modifier = Modifier.size(MASCOT_SIZE))
            }
            Text(
                text =
                    if (remainingFreeMatches == 0) {
                        stringResource(R.string.cap_banner_reached)
                    } else {
                        pluralStringResource(R.plurals.cap_banner_remaining, remainingFreeMatches, remainingFreeMatches)
                    },
                style = if (isImminent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isImminent) FontWeight.SemiBold else null,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            IconButton(onClick = onDismiss, modifier = Modifier.testTag(PaywallTestTags.CAP_BANNER_DISMISS)) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.cap_banner_dismiss),
                )
            }
        }
    }
}
