package com.maxeydev.picklejot.ui.share

import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.deriveDuration
import com.maxeydev.picklejot.domain.share.CardDetail
import com.maxeydev.picklejot.domain.share.CardFormat
import com.maxeydev.picklejot.domain.share.CardLayout

fun buildCardData(
    match: Match,
    displayName: String,
    photoDataUri: String?,
    labels: CardLabels,
    format: CardFormat = CardFormat.DEFAULT,
    hidden: Set<CardDetail> = emptySet(),
    showWordmark: Boolean = true,
): CardData =
    CardData(
        brand = if (showWordmark) labels.brand else "",
        displayName = displayName.trim(),
        meta = labels.meta(match.format, match.date),
        partner = match.partner?.takeIf { CardDetail.PARTNER !in hidden }?.let { labels.partner(it.displayName) },
        time = deriveDuration(match.startTime, match.endTime)?.let { labels.time(it) },
        opponents =
            if (CardDetail.OPPONENTS in hidden) null else labels.opponents(match.opponents.map { it.displayName }),
        games =
            if (CardDetail.GAME_SCORES in hidden) null else labels.games(match.games.sortedBy { it.gameNumber }),
        location =
            match.location
                ?.trim()
                ?.takeIf { it.isNotEmpty() && CardDetail.LOCATION !in hidden },
        photo = photoDataUri?.takeIf { format.layoutFor(hasPhoto = true) == CardLayout.PHOTO },
        ratio = format.ratio,
        theme = format.theme,
    )

fun Match.cardDetails(): Set<CardDetail> =
    buildSet {
        if (games.isNotEmpty()) {
            add(CardDetail.GAME_SCORES)
        }
        if (opponents.isNotEmpty()) {
            add(CardDetail.OPPONENTS)
        }
        if (partner != null) {
            add(CardDetail.PARTNER)
        }
        if (!location.isNullOrBlank()) {
            add(CardDetail.LOCATION)
        }
    }
