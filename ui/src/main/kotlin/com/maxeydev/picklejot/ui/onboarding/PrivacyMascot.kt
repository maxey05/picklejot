package com.maxeydev.picklejot.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.common.AnimatedMascot
import com.maxeydev.picklejot.ui.common.Mascot

private val MASCOT_TOP_GAP = 16.dp
private val MASCOT_BOTTOM_GAP = 8.dp
private val MASCOT_SIDE_GAP = 48.dp

@Composable
internal fun PrivacyMascot(
    topInset: Dp,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(IntroPage.PRIVACY.illustrationDescription)
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        AnimatedMascot(
            mascot = Mascot.READY,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = MASCOT_SIDE_GAP)
                    .padding(top = topInset + MASCOT_TOP_GAP, bottom = MASCOT_BOTTOM_GAP),
        )
    }
}
