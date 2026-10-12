package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.ui.share.CardData
import com.maxeydev.picklejot.ui.share.CardRenderFailure
import com.maxeydev.picklejot.ui.share.CardRenderResult
import com.maxeydev.picklejot.ui.share.CardRendering
import java.util.concurrent.CopyOnWriteArrayList

class FakeCardRenderer(
    var result: (CardData) -> CardRenderResult = { CardRenderResult.Failed(CardRenderFailure.LAYOUT_NOT_READY) },
) : CardRendering {
    val rendered: MutableList<CardData> = CopyOnWriteArrayList()

    override suspend fun render(data: CardData): CardRenderResult {
        rendered += data
        return result(data)
    }
}
