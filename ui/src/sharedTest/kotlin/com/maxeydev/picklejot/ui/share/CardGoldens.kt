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
        CardGolden("tall-dark-photo", CardRatio.TALL, CardTheme.DARK, CardLayout.PHOTO),
        CardGolden("tall-dark-no-photo", CardRatio.TALL, CardTheme.DARK, CardLayout.NO_PHOTO),
        CardGolden("tall-light-photo", CardRatio.TALL, CardTheme.LIGHT, CardLayout.PHOTO),
        CardGolden("tall-light-no-photo", CardRatio.TALL, CardTheme.LIGHT, CardLayout.NO_PHOTO),
        CardGolden("square-dark-photo", CardRatio.SQUARE, CardTheme.DARK, CardLayout.PHOTO),
        CardGolden("square-dark-no-photo", CardRatio.SQUARE, CardTheme.DARK, CardLayout.NO_PHOTO),
        CardGolden("square-light-photo", CardRatio.SQUARE, CardTheme.LIGHT, CardLayout.PHOTO),
        CardGolden("square-light-no-photo", CardRatio.SQUARE, CardTheme.LIGHT, CardLayout.NO_PHOTO),
        CardGolden("tall-court-photo", CardRatio.TALL, CardTheme.COURT, CardLayout.PHOTO),
        CardGolden("tall-court-no-photo", CardRatio.TALL, CardTheme.COURT, CardLayout.NO_PHOTO),
        CardGolden("tall-sunset-photo", CardRatio.TALL, CardTheme.SUNSET, CardLayout.PHOTO),
        CardGolden("tall-sunset-no-photo", CardRatio.TALL, CardTheme.SUNSET, CardLayout.NO_PHOTO),
        CardGolden("square-court-photo", CardRatio.SQUARE, CardTheme.COURT, CardLayout.PHOTO),
        CardGolden("square-court-no-photo", CardRatio.SQUARE, CardTheme.COURT, CardLayout.NO_PHOTO),
        CardGolden("square-sunset-photo", CardRatio.SQUARE, CardTheme.SUNSET, CardLayout.PHOTO),
        CardGolden("square-sunset-no-photo", CardRatio.SQUARE, CardTheme.SUNSET, CardLayout.NO_PHOTO),
        CardGolden(
            "tall-dark-photo-long-names",
            CardRatio.TALL,
            CardTheme.DARK,
            CardLayout.PHOTO,
            GoldenContent.LONG_NAMES,
        ),
        CardGolden(
            "tall-dark-photo-emoji-names",
            CardRatio.TALL,
            CardTheme.DARK,
            CardLayout.PHOTO,
            GoldenContent.EMOJI_NAMES,
        ),
    )
