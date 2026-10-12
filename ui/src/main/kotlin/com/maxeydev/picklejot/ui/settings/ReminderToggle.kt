package com.maxeydev.picklejot.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.PermissionPrompt
import com.maxeydev.picklejot.ui.common.PermissionRationale
import com.maxeydev.picklejot.ui.common.PermissionSettingsRedirect
import com.maxeydev.picklejot.ui.common.PermissionState
import com.maxeydev.picklejot.ui.common.rememberPermissionState
import com.maxeydev.picklejot.ui.notification.StreakNotification

@Composable
fun ReminderToggle(
    enabled: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = notificationsAllowed(context) }
    Column(modifier = modifier.fillMaxWidth()) {
        if (StreakNotification.requiresRuntimePermission()) {
            val (permission, prompt) = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS, onEnable)
            ReminderSwitchRow(
                checked = enabled && allowed,
                showDivider = showDivider,
                onTurnOn = permission::request,
                onTurnOff = onDisable,
            )
            RuntimePermissionDialogs(prompt = prompt, permission = permission)
        } else {
            var showSettings by rememberSaveable { mutableStateOf(false) }
            ReminderSwitchRow(
                checked = enabled && allowed,
                showDivider = showDivider,
                onTurnOn = { if (allowed) onEnable() else showSettings = true },
                onTurnOff = onDisable,
            )
            if (showSettings) {
                PermissionSettingsRedirect(
                    title = stringResource(R.string.reminder_settings_title),
                    body = stringResource(R.string.reminder_settings_body),
                    onOpenSettings = {
                        showSettings = false
                        context.startActivity(notificationSettingsIntent(context))
                    },
                    onDismiss = { showSettings = false },
                )
            }
        }
        if (enabled && !allowed) {
            BlockedNotice(onOpenSettings = { context.startActivity(notificationSettingsIntent(context)) })
        }
    }
}

@Composable
private fun ReminderSwitchRow(
    checked: Boolean,
    showDivider: Boolean,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
) {
    SettingsRow(
        icon = R.drawable.ic_notifications,
        label = stringResource(R.string.settings_reminder_label),
        supporting = if (checked) stringResource(R.string.settings_reminder_supporting) else null,
        showDivider = showDivider,
        modifier =
            Modifier
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = { wantsOn -> if (wantsOn) onTurnOn() else onTurnOff() },
                ).testTag(SettingsTestTags.REMINDER_TOGGLE),
    ) {
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun RuntimePermissionDialogs(
    prompt: PermissionPrompt,
    permission: PermissionState,
) {
    when (prompt) {
        PermissionPrompt.NONE -> Unit
        PermissionPrompt.RATIONALE ->
            PermissionRationale(
                title = stringResource(R.string.reminder_permission_title),
                body = stringResource(R.string.reminder_permission_body),
                onContinue = permission::continueFromRationale,
                onDismiss = permission::dismiss,
                icon = R.drawable.ic_notifications,
                confirmLabel = stringResource(R.string.reminder_permission_confirm),
            )
        PermissionPrompt.SETTINGS ->
            PermissionSettingsRedirect(
                title = stringResource(R.string.reminder_settings_title),
                body = stringResource(R.string.reminder_settings_body),
                onOpenSettings = permission::openAppSettings,
                onDismiss = permission::dismiss,
            )
    }
}

@Composable
private fun BlockedNotice(onOpenSettings: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp).testTag(SettingsTestTags.REMINDER_BLOCKED)) {
        Text(
            text = stringResource(R.string.settings_reminder_blocked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.heightIn(min = 48.dp).testTag(SettingsTestTags.REMINDER_OPEN_SETTINGS),
        ) {
            Text(stringResource(R.string.settings_reminder_open_settings))
        }
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
