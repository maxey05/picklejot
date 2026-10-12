package com.maxeydev.picklejot.ui.share

import android.webkit.JavascriptInterface
import kotlinx.coroutines.CompletableDeferred

const val CARD_BRIDGE_NAME = "PicklejotCard"

class CardReadyBridge {
    private val lock = Any()
    private var expectedToken: String? = null
    private var ready: CompletableDeferred<String>? = null

    fun expect(token: String): CompletableDeferred<String> {
        val deferred = CompletableDeferred<String>()
        synchronized(lock) {
            ready?.cancel()
            expectedToken = token
            ready = deferred
        }
        return deferred
    }

    @JavascriptInterface
    fun onLayoutReady(
        token: String,
        diagnostics: String,
    ) {
        val deferred =
            synchronized(lock) {
                if (token != expectedToken) {
                    null
                } else {
                    expectedToken = null
                    ready.also { ready = null }
                }
            }
        deferred?.complete(diagnostics)
    }
}
