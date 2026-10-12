package com.maxeydev.picklejot.ui.share

import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.util.concurrent.CopyOnWriteArrayList

const val ALLOWED_URL_PREFIX = "file:///android_asset/"
private const val FORBIDDEN = 403

fun isAllowedCardUrl(url: String): Boolean = url.startsWith(ALLOWED_URL_PREFIX) && !url.contains("/../")

class LockedDownWebViewClient(
    private val onPageLoaded: (String) -> Unit,
    private val onRendererGone: () -> Unit,
) : WebViewClient() {
    val blockedUrls: MutableList<String> = CopyOnWriteArrayList()

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url.toString()
        if (isAllowedCardUrl(url)) {
            return null
        }
        blockedUrls += url
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            FORBIDDEN,
            "Blocked",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0)),
        )
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val url = request.url.toString()
        if (isAllowedCardUrl(url)) {
            return false
        }
        blockedUrls += url
        return true
    }

    override fun onPageFinished(
        view: WebView,
        url: String,
    ) {
        onPageLoaded(url)
    }

    override fun onRenderProcessGone(
        view: WebView,
        detail: RenderProcessGoneDetail,
    ): Boolean {
        onRendererGone()
        return true
    }
}
