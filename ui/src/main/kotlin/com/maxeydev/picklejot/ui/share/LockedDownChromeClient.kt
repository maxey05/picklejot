package com.maxeydev.picklejot.ui.share

import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient

class LockedDownChromeClient : WebChromeClient() {
    override fun onPermissionRequest(request: PermissionRequest) {
        request.deny()
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback,
    ) {
        callback.invoke(origin, false, false)
    }
}
