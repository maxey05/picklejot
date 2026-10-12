package com.maxeydev.picklejot.ui.share

interface CardRendering {
    suspend fun render(data: CardData): CardRenderResult
}
