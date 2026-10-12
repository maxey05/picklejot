package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.share.CardFormat
import com.maxeydev.picklejot.domain.share.CardFormatStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCardFormatStore(
    initial: CardFormat = CardFormat.DEFAULT,
) : CardFormatStore {
    private val format = MutableStateFlow(initial)

    val saved = mutableListOf<CardFormat>()

    override fun observeFormat(): Flow<CardFormat> = format

    fun store(format: CardFormat) {
        this.format.value = format
    }

    override suspend fun saveFormat(format: CardFormat) {
        saved += format
        this.format.value = format
    }
}
