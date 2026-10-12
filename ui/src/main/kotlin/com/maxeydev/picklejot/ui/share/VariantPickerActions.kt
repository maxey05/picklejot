package com.maxeydev.picklejot.ui.share

import com.maxeydev.picklejot.domain.share.CardDetail
import com.maxeydev.picklejot.domain.share.CardLayout
import com.maxeydev.picklejot.domain.share.CardRatio
import com.maxeydev.picklejot.domain.share.CardTheme

data class VariantPickerActions(
    val onRatioSelected: (CardRatio) -> Unit,
    val onThemeSelected: (CardTheme) -> Unit,
    val onLayoutSelected: (CardLayout) -> Unit,
    val onDetailShownChanged: (CardDetail, Boolean) -> Unit,
)
