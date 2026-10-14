package com.maxeydev.picklejot.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object LegalLinks {
    const val PRIVACY_URL = "https://maxey05.github.io/picklelog/privacy.html"
    const val TERMS_URL = "https://maxey05.github.io/picklelog/terms.html"
}

fun openExternalLink(
    context: Context,
    url: String,
): Boolean =
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (noHandler: ActivityNotFoundException) {
        false
    }
