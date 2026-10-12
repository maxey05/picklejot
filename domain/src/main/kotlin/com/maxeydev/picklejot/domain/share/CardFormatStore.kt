package com.maxeydev.picklejot.domain.share

import kotlinx.coroutines.flow.Flow

interface CardFormatStore {
    fun observeFormat(): Flow<CardFormat>

    suspend fun saveFormat(format: CardFormat)
}
