package com.maxeydev.picklejot.ui.paywall

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklejot.domain.entitlement.ProTier
import com.maxeydev.picklejot.ui.R

@Composable
fun UpgradePrompt(
    reason: UpgradeReason,
    onDismiss: () -> Unit,
    onSeePro: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(PaywallTestTags.UPGRADE_PROMPT),
        title = {
            Text(
                when (reason) {
                    UpgradeReason.PHOTO_LIMIT -> stringResource(R.string.upgrade_photo_limit_title)
                    UpgradeReason.PRO_PHOTO_LIMIT ->
                        stringResource(R.string.upgrade_pro_photo_limit_title, ProTier.PHOTOS_PER_MATCH)
                    UpgradeReason.PRO_THEME -> stringResource(R.string.upgrade_pro_theme_title)
                },
            )
        },
        text = {
            Text(
                when (reason) {
                    UpgradeReason.PHOTO_LIMIT -> stringResource(R.string.upgrade_photo_limit_body)
                    UpgradeReason.PRO_PHOTO_LIMIT -> stringResource(R.string.upgrade_pro_photo_limit_body)
                    UpgradeReason.PRO_THEME -> stringResource(R.string.upgrade_pro_theme_body)
                },
            )
        },
        confirmButton = {
            if (onSeePro != null) {
                TextButton(
                    onClick = onSeePro,
                    modifier = Modifier.testTag(PaywallTestTags.UPGRADE_PROMPT_SEE_PRO),
                ) {
                    Text(stringResource(R.string.upgrade_see_pro))
                }
            } else {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag(PaywallTestTags.UPGRADE_PROMPT_DISMISS)) {
                    Text(stringResource(R.string.upgrade_got_it))
                }
            }
        },
        dismissButton =
            if (onSeePro != null) {
                {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag(PaywallTestTags.UPGRADE_PROMPT_DISMISS),
                    ) {
                        Text(stringResource(R.string.upgrade_not_now))
                    }
                }
            } else {
                null
            },
    )
}
