package com.maxeydev.picklejot.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

private const val MARKET_LISTING = "market://details?id="
private const val WEB_LISTING = "https://play.google.com/store/apps/details?id="

fun openStoreListing(context: Context): Boolean =
    listOf(MARKET_LISTING, WEB_LISTING).any { prefix -> launchView(context, prefix + context.packageName) }

private fun launchView(
    context: Context,
    uri: String,
): Boolean =
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (noHandler: ActivityNotFoundException) {
        false
    }
