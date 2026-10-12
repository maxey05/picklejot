package com.maxeydev.picklejot.domain.share

enum class CardRatio {
    TALL,
    SQUARE,
}

enum class CardTheme(
    val requiresPro: Boolean,
) {
    DARK(requiresPro = false),
    LIGHT(requiresPro = false),
    COURT(requiresPro = true),
    SUNSET(requiresPro = true),
}

enum class CardLayout {
    PHOTO,
    NO_PHOTO,
}

data class CardFormat(
    val ratio: CardRatio = CardRatio.TALL,
    val theme: CardTheme = CardTheme.DARK,
    val layoutOverride: CardLayout? = null,
) {
    fun layoutFor(hasPhoto: Boolean): CardLayout =
        when {
            !hasPhoto -> CardLayout.NO_PHOTO
            else -> layoutOverride ?: CardLayout.PHOTO
        }

    fun forEntitlement(isPro: Boolean): CardFormat =
        if (theme.requiresPro && !isPro) copy(theme = CardTheme.DARK) else this

    companion object {
        val DEFAULT: CardFormat = CardFormat()
    }
}
