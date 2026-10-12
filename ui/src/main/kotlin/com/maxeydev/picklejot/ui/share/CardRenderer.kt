@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.share

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val TEMPLATE_TIMEOUT_MILLIS = 5_000L
const val LAYOUT_READY_TIMEOUT_MILLIS = 5_000L
private const val DRAW_ATTEMPTS = 4
private const val FRAME_MILLIS = 32L
private const val BLANK_SAMPLES_PER_SIDE = 16

class CardRenderer(
    private val warmer: WebViewWarmer,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : CardRendering {
    private val mutex = Mutex()

    override suspend fun render(data: CardData): CardRenderResult =
        mutex.withLock {
            withContext(mainDispatcher) { renderOnMainThread(data) }
        }

    private suspend fun renderOnMainThread(data: CardData): CardRenderResult {
        val card = warmer.obtain(data.ratio)
        val loaded = withTimeoutOrNull(TEMPLATE_TIMEOUT_MILLIS) { runCatching { card.awaitTemplateLoaded() } }
        if (loaded == null || loaded.isFailure) {
            warmer.discard()
            return CardRenderResult.Failed(
                if (card.isAlive) CardRenderFailure.TEMPLATE_NOT_LOADED else CardRenderFailure.RENDERER_CRASHED,
            )
        }
        val token = Uuid.random().toString()
        val ready = card.bridge.expect(token)
        card.webView.evaluateJavascript(CardDataSerializer.renderCall(data, token), null)
        val diagnostics = withTimeoutOrNull(LAYOUT_READY_TIMEOUT_MILLIS) { ready.await() }
        if (diagnostics == null) {
            val crashed = !card.isAlive
            warmer.discard()
            return CardRenderResult.Failed(
                if (crashed) CardRenderFailure.RENDERER_CRASHED else CardRenderFailure.LAYOUT_NOT_READY,
            )
        }
        card.awaitNextVisualState()
        repeat(DRAW_ATTEMPTS) {
            val bitmap = card.capture()
            if (!bitmap.isBlank()) {
                return CardRenderResult.Rendered(bitmap, diagnostics)
            }
            bitmap.recycle()
            delay(FRAME_MILLIS)
        }
        return CardRenderResult.Failed(CardRenderFailure.NOTHING_DRAWN)
    }
}

internal fun Bitmap.isBlank(): Boolean {
    val stepX = (width / BLANK_SAMPLES_PER_SIDE).coerceAtLeast(1)
    val stepY = (height / BLANK_SAMPLES_PER_SIDE).coerceAtLeast(1)
    for (y in stepY / 2 until height step stepY) {
        for (x in stepX / 2 until width step stepX) {
            if (Color.alpha(getPixel(x, y)) != 0) {
                return false
            }
        }
    }
    return true
}
