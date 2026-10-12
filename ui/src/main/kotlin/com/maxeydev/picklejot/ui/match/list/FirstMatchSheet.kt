package com.maxeydev.picklejot.ui.match.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.Mascot
import com.maxeydev.picklejot.ui.common.MascotSheet
import com.maxeydev.picklejot.ui.sound.Cue

@Composable
fun FirstMatchSheet(
    onDismiss: () -> Unit,
    onLogAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MascotSheet(
        mascot = Mascot.HATCHLING,
        title = stringResource(R.string.first_match_title),
        body = stringResource(R.string.first_match_body),
        onDismiss = onDismiss,
        sheetTag = MatchListTestTags.FIRST_MATCH_SHEET,
        secondaryLabel = stringResource(R.string.log_another),
        onSecondary = onLogAnother,
        entranceCue = Cue.FIRST_MATCH,
        modifier = modifier,
    )
}
