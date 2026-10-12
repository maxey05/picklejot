package com.maxeydev.picklejot.ui.share

import com.maxeydev.picklejot.domain.share.CardDetail
import com.maxeydev.picklejot.domain.share.CardRatio
import com.maxeydev.picklejot.domain.share.CardTheme

object ShareTestTags {
    const val SCREEN = "share_screen"
    const val PREVIEW = "share_preview"
    const val RENDERING = "share_rendering"
    const val FAILED = "share_failed"
    const val RETRY = "share_retry"
    const val SHARE = "share_send"
    const val VARIANT_PICKER = "share_variant_picker"
    const val STYLE_BUTTON = "share_style_button"
    const val PHOTO_SWITCH = "share_photo_switch"

    fun ratio(ratio: CardRatio): String = "share_ratio_${ratio.name}"

    fun theme(theme: CardTheme): String = "share_theme_${theme.name}"

    fun detail(detail: CardDetail): String = "share_detail_${detail.name}"
}
