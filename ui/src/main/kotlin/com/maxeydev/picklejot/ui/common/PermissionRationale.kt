package com.maxeydev.picklejot.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R

private val ICON_BADGE_SIZE = 56.dp
private val ICON_SIZE = 26.dp
private val MIN_TOUCH_TARGET = 48.dp

object PermissionTestTags {
    const val RATIONALE = "permission_rationale"
    const val RATIONALE_CONTINUE = "permission_rationale_continue"
    const val SETTINGS = "permission_settings"
    const val SETTINGS_OPEN = "permission_settings_open"
}

@Composable
fun PermissionRationale(
    title: String,
    body: String,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
    @DrawableRes icon: Int? = null,
    confirmLabel: String = stringResource(R.string.permission_continue),
) {
    val textAlign = if (icon != null) TextAlign.Center else TextAlign.Start
    AlertDialog(
        onDismissRequest = onDismiss,
        icon =
            icon?.let { res ->
                {
                    Box(
                        modifier =
                            Modifier
                                .size(ICON_BADGE_SIZE)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(res),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(ICON_SIZE),
                        )
                    }
                }
            },
        title = { Text(text = title, textAlign = textAlign, modifier = Modifier.fillMaxWidth()) },
        text = { Text(text = body, textAlign = textAlign, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            if (icon != null) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET).testTag(PermissionTestTags.RATIONALE_CONTINUE),
                ) {
                    Text(confirmLabel)
                }
            } else {
                TextButton(
                    onClick = onContinue,
                    modifier = Modifier.testTag(PermissionTestTags.RATIONALE_CONTINUE),
                ) {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET)) {
                Text(stringResource(R.string.permission_not_now))
            }
        },
        modifier = Modifier.testTag(PermissionTestTags.RATIONALE),
    )
}

@Composable
fun PermissionSettingsRedirect(
    title: String,
    body: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag(PermissionTestTags.SETTINGS_OPEN),
            ) {
                Text(stringResource(R.string.permission_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.permission_not_now))
            }
        },
        modifier = Modifier.testTag(PermissionTestTags.SETTINGS),
    )
}
