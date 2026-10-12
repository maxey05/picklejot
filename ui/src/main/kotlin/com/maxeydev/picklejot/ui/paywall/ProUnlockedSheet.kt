package com.maxeydev.picklejot.ui.paywall

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.Mascot
import com.maxeydev.picklejot.ui.common.MascotSheet
import com.maxeydev.picklejot.ui.sound.Cue

@Composable
fun ProUnlockedSheet(
    restored: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MascotSheet(
        mascot = Mascot.BALL_BUDDY,
        title = stringResource(if (restored) R.string.store_restored else R.string.store_unlocked),
        body = stringResource(R.string.pro_unlocked_body),
        onDismiss = onDismiss,
        sheetTag = PaywallTestTags.UNLOCKED_SHEET,
        entranceCue = if (restored) Cue.PRO_RESTORED else Cue.PRO_UNLOCKED,
        modifier = modifier,
    )
}
