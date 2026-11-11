package com.maxeydev.picklejot.ui.share

import com.maxeydev.picklejot.domain.share.CardLayout
import com.maxeydev.picklejot.domain.share.CardRatio
import com.maxeydev.picklejot.domain.share.CardTheme

enum class GoldenContent {
    STANDARD,
    LONG_NAMES,
    EMOJI_NAMES,
}

data class CardGolden(
    val name: String,
    val ratio: CardRatio,
    val theme: CardTheme,
    val layout: CardLayout,
    val content: GoldenContent = GoldenContent.STANDARD,
)

val CARD_GOLDENS: List<CardGolden> =
    listOf(
        CardGolden("tall-classic-photo", CardRatio.TALL, CardTheme.CLASSIC, CardLayout.PHOTO),
        CardGolden("tall-classic-no-photo", CardRatio.TALL, CardTheme.CLASSIC, CardLayout.NO_PHOTO),
        CardGolden("square-classic-photo", CardRatio.SQUARE, CardTheme.CLASSIC, CardLayout.PHOTO),
        CardGolden("square-classic-no-photo", CardRatio.SQUARE, CardTheme.CLASSIC, CardLayout.NO_PHOTO),
        CardGolden("tall-court-photo", CardRatio.TALL, CardTheme.COURT, CardLayout.PHOTO),
        CardGolden("tall-court-no-photo", CardRatio.TALL, CardTheme.COURT, CardLayout.NO_PHOTO),
        CardGolden("tall-sunset-photo", CardRatio.TALL, CardTheme.SUNSET, CardLayout.PHOTO),
        CardGolden("tall-sunset-no-photo", CardRatio.TALL, CardTheme.SUNSET, CardLayout.NO_PHOTO),
        CardGolden("square-court-photo", CardRatio.SQUARE, CardTheme.COURT, CardLayout.PHOTO),
        CardGolden("square-court-no-photo", CardRatio.SQUARE, CardTheme.COURT, CardLayout.NO_PHOTO),
        CardGolden("square-sunset-photo", CardRatio.SQUARE, CardTheme.SUNSET, CardLayout.PHOTO),
        CardGolden("square-sunset-no-photo", CardRatio.SQUARE, CardTheme.SUNSET, CardLayout.NO_PHOTO),
        CardGolden(
            "tall-classic-photo-long-names",
            CardRatio.TALL,
            CardTheme.CLASSIC,
            CardLayout.PHOTO,
            GoldenContent.LONG_NAMES,
        ),
        CardGolden(
            "tall-classic-photo-emoji-names",
            CardRatio.TALL,
            CardTheme.CLASSIC,
            CardLayout.PHOTO,
            GoldenContent.EMOJI_NAMES,
        ),
    )
