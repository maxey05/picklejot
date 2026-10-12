package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.match.list.filterChipLabel

private val INDICATOR_ICON_SIZE = 18.dp

@Composable
fun FilterIndicator(
    filter: FilterState,
    opponentName: String?,
    modifier: Modifier = Modifier,
) {
    if (!filter.isActive) {
        return
    }
    val description = activeFilterDescription(filter, opponentName)
    val spoken = stringResource(R.string.dashboard_a11y_filtered, description)
    Row(
        modifier =
            modifier
                .testTag(DashboardTestTags.FILTER_INDICATOR)
                .semantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_filter),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(INDICATOR_ICON_SIZE),
        )
        Text(
            text = stringResource(R.string.dashboard_filtered, description),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun activeFilterDescription(
    filter: FilterState,
    opponentName: String?,
): String {
    val separator = stringResource(R.string.dashboard_filter_separator)
    return filter.activeKinds
        .map { kind -> filterChipLabel(kind, filter, opponentName) }
        .joinToString(separator = separator)
}
