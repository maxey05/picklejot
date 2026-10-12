package com.maxeydev.picklejot.ui.share

import android.graphics.Bitmap

sealed interface CardRenderResult {
    data class Rendered(
        val bitmap: Bitmap,
        val diagnostics: String,
    ) : CardRenderResult

    data class Failed(
        val reason: CardRenderFailure,
    ) : CardRenderResult
}

enum class CardRenderFailure {
    TEMPLATE_NOT_LOADED,
    LAYOUT_NOT_READY,
    NOTHING_DRAWN,
    RENDERER_CRASHED,
}
