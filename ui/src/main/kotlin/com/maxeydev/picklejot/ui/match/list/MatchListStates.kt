package com.maxeydev.picklejot.ui.match.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.AnimatedMascot
import com.maxeydev.picklejot.ui.common.Mascot
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing

private val ACTION_MIN_HEIGHT = 48.dp
private val EMPTY_IMAGE_SIZE = 160.dp
private val NO_RESULTS_ICON_CIRCLE = 96.dp
private val NO_RESULTS_MASCOT_SIZE = 72.dp
private val ACTION_ICON_SIZE = 20.dp
private val FAB_CLEARANCE = 96.dp

@Composable
fun MatchListEmptyState(
    onNewMatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PicklejotSpacing.xxl, vertical = PicklejotSpacing.xl)
                    .testTag(MatchListTestTags.EMPTY_STATE),
            verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedMascot(
                mascot = Mascot.READY,
                modifier = Modifier.size(EMPTY_IMAGE_SIZE),
            )
            Text(
                text = stringResource(R.string.list_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = PicklejotSpacing.sm).semantics { heading() },
            )
            Text(
                text = stringResource(R.string.list_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onNewMatch,
                shape = CircleShape,
                modifier = Modifier.heightIn(min = ACTION_MIN_HEIGHT).testTag(MatchListTestTags.EMPTY_LOG_MATCH),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(ACTION_ICON_SIZE),
                )
                Text(
                    text = stringResource(R.string.list_empty_action),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = PicklejotSpacing.sm),
                )
            }
        }
    }
}

@Composable
fun NoResultsState(
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = FAB_CLEARANCE)
                    .testTag(MatchListTestTags.NO_RESULTS),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(NO_RESULTS_ICON_CIRCLE)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedMascot(
                    mascot = Mascot.HEAD_OOPS,
                    modifier = Modifier.size(NO_RESULTS_MASCOT_SIZE),
                )
            }
            Text(
                text = stringResource(R.string.no_results_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.no_results_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = onClear,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.heightIn(min = ACTION_MIN_HEIGHT).testTag(MatchListTestTags.NO_RESULTS_CLEAR),
            ) {
                Text(text = stringResource(R.string.no_results_clear), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
