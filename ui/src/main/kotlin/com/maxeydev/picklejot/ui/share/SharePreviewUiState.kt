package com.maxeydev.picklejot.ui.share

import android.graphics.Bitmap
import com.maxeydev.picklejot.domain.share.CardDetail
import com.maxeydev.picklejot.domain.share.CardFormat
import com.maxeydev.picklejot.domain.share.CardLayout
import com.maxeydev.picklejot.ui.paywall.UpgradeReason

data class SharePreviewUiState(
    val isRendering: Boolean = true,
    val card: Bitmap? = null,
    val cardDescription: String = "",
    val hasFailed: Boolean = false,
    val isGone: Boolean = false,
    val hasShareFailed: Boolean = false,
    val format: CardFormat = CardFormat.DEFAULT,
    val hasPhoto: Boolean = false,
    val hiddenDetails: Set<CardDetail> = emptySet(),
    val availableDetails: Set<CardDetail> = emptySet(),
    val isPro: Boolean = false,
    val upgradeReason: UpgradeReason? = null,
) {
    val layout: CardLayout
        get() = format.layoutFor(hasPhoto)

    val canShare: Boolean
        get() = card != null && !isRendering
}
