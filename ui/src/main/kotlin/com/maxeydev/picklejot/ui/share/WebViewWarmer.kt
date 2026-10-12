package com.maxeydev.picklejot.ui.share

import android.content.Context
import androidx.annotation.MainThread
import com.maxeydev.picklejot.domain.share.CardRatio

class WebViewWarmer(
    context: Context,
    private val templateUrl: String = CARD_TEMPLATE_URL,
    private val warmRatio: CardRatio = CardRatio.TALL,
) {
    private val appContext = context.applicationContext
    private var instance: CardWebView? = null

    val isWarm: Boolean
        get() = instance?.isAlive == true

    @MainThread
    fun warm() {
        obtain(warmRatio)
    }

    @MainThread
    fun obtain(ratio: CardRatio = warmRatio): CardWebView {
        val height = ratio.heightPx
        instance?.takeIf { it.isAlive && it.height == height }?.let { return it }
        instance?.destroy()
        return CardWebView(appContext, templateUrl, CARD_WIDTH_PX, height).also { instance = it }
    }

    @MainThread
    fun discard() {
        instance?.destroy()
        instance = null
    }
}
