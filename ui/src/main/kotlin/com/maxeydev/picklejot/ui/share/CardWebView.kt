package com.maxeydev.picklejot.ui.share

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.MainThread
import com.maxeydev.picklejot.domain.share.CardRatio
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

const val CARD_TEMPLATE_URL = "file:///android_asset/card/index.html"
const val CARD_WIDTH_PX = 1080
const val TALL_CARD_HEIGHT_PX = 1920
const val SQUARE_CARD_HEIGHT_PX = 1080
private const val FULL_TEXT_SIZE_PERCENT = 100
private const val VISUAL_STATE_TIMEOUT_MILLIS = 1_000L

class CardWebView(
    context: Context,
    private val templateUrl: String,
    val width: Int,
    val height: Int,
) {
    private val templateLoaded = CompletableDeferred<Unit>()
    private var nextVisualStateRequest = 1L

    @Volatile
    var isAlive: Boolean = true
        private set

    val bridge = CardReadyBridge()
    val client =
        LockedDownWebViewClient(
            onPageLoaded = { url -> if (url == templateUrl) templateLoaded.complete(Unit) },
            onRendererGone = {
                isAlive = false
                templateLoaded.completeExceptionally(IllegalStateException("The card renderer stopped."))
            },
        )
    val webView: WebView = createWebView(context)

    suspend fun awaitTemplateLoaded() {
        templateLoaded.await()
    }

    suspend fun awaitNextVisualState(): Boolean {
        val requestId = nextVisualStateRequest++
        return withTimeoutOrNull(VISUAL_STATE_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                webView.postVisualStateCallback(
                    requestId,
                    object : WebView.VisualStateCallback() {
                        override fun onComplete(completedRequestId: Long) {
                            if (completedRequestId == requestId && continuation.isActive) {
                                continuation.resume(true)
                            }
                        }
                    },
                )
                webView.invalidate()
            }
        } ?: false
    }

    @MainThread
    fun capture(): Bitmap {
        layoutAtCardSize()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        webView.draw(Canvas(bitmap))
        return bitmap
    }

    @MainThread
    fun destroy() {
        isAlive = false
        webView.stopLoading()
        webView.removeJavascriptInterface(CARD_BRIDGE_NAME)
        webView.destroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context): WebView {
        enableWholeDocumentDrawOnce()
        val view = WebView(context)
        view.settings.apply {
            javaScriptEnabled = true
            textZoom = FULL_TEXT_SIZE_PERCENT
            useWideViewPort = false
            loadWithOverviewMode = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = false
            // INTERNET is now declared (purchase verifier), so the old "no permission, no network"
            // backstop is gone. This restores it for the card renderer; the client and CSP are the rest.
            blockNetworkLoads = true
            domStorageEnabled = false
            setGeolocationEnabled(false)
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = true
            offscreenPreRaster = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(false)
            setAcceptThirdPartyCookies(view, false)
        }
        view.isVerticalScrollBarEnabled = false
        view.isHorizontalScrollBarEnabled = false
        view.setBackgroundColor(Color.TRANSPARENT)
        view.webViewClient = client
        view.webChromeClient = LockedDownChromeClient()
        view.addJavascriptInterface(bridge, CARD_BRIDGE_NAME)
        layoutAtCardSize(view)
        view.loadUrl(templateUrl)
        return view
    }

    private fun layoutAtCardSize(view: WebView = webView) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
    }

    private companion object {
        @Volatile
        private var wholeDocumentDrawEnabled = false

        fun enableWholeDocumentDrawOnce() {
            if (!wholeDocumentDrawEnabled) {
                WebView.enableSlowWholeDocumentDraw()
                wholeDocumentDrawEnabled = true
            }
        }
    }
}

val CardRatio.heightPx: Int
    get() =
        when (this) {
            CardRatio.TALL -> TALL_CARD_HEIGHT_PX
            CardRatio.SQUARE -> SQUARE_CARD_HEIGHT_PX
        }
