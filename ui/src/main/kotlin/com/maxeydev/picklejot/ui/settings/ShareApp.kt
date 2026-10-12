package com.maxeydev.picklejot.ui.settings

import android.content.Context
import android.content.Intent
import com.maxeydev.picklejot.ui.R

private const val STORE_URL = "https://play.google.com/store/apps/details?id="

fun shareApp(context: Context) {
    val message = context.getString(R.string.support_share_message, STORE_URL + context.packageName)
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.support_share_chooser_title)))
}
