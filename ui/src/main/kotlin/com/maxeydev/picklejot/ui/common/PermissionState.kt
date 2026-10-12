package com.maxeydev.picklejot.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

enum class PermissionPrompt {
    NONE,
    RATIONALE,
    SETTINGS,
}

@Stable
class PermissionState(
    private val isGranted: () -> Boolean,
    private val showPrompt: (PermissionPrompt) -> Unit,
    private val launchSystemRequest: () -> Unit,
    private val openSettings: () -> Unit,
    private val grantedAction: () -> (() -> Unit),
) {
    fun request() {
        if (isGranted()) {
            grantedAction()()
        } else {
            showPrompt(PermissionPrompt.RATIONALE)
        }
    }

    fun continueFromRationale() {
        showPrompt(PermissionPrompt.NONE)
        launchSystemRequest()
    }

    fun openAppSettings() {
        showPrompt(PermissionPrompt.NONE)
        openSettings()
    }

    fun dismiss() {
        showPrompt(PermissionPrompt.NONE)
    }
}

@Composable
fun rememberPermissionState(
    permission: String,
    onGranted: () -> Unit,
): Pair<PermissionState, PermissionPrompt> {
    val context = LocalContext.current
    val latestOnGranted by rememberUpdatedState(onGranted)
    var prompt by rememberSaveable { mutableStateOf(PermissionPrompt.NONE) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            prompt =
                when {
                    granted -> {
                        latestOnGranted()
                        PermissionPrompt.NONE
                    }
                    context.canAskAgainFor(permission) -> PermissionPrompt.NONE
                    else -> PermissionPrompt.SETTINGS
                }
        }
    val state =
        remember(context, permission, launcher) {
            PermissionState(
                isGranted = {
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                },
                showPrompt = { prompt = it },
                launchSystemRequest = { launcher.launch(permission) },
                openSettings = { context.startActivity(appSettingsIntent(context)) },
                grantedAction = { latestOnGranted },
            )
        }
    return state to prompt
}

private fun Context.canAskAgainFor(permission: String): Boolean {
    val activity = findActivity() ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun appSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
