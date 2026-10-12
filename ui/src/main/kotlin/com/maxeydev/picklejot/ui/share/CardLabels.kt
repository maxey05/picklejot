package com.maxeydev.picklejot.ui.share

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.MatchFormat
import kotlin.time.Duration

interface CardLabels {
    val brand: String

    fun meta(
        format: MatchFormat,
        date: AppDate,
    ): String

    fun partner(name: String): CardEntry

    fun time(duration: Duration): CardEntry

    fun opponents(names: List<String>): CardEntry?

    fun games(games: List<GameScore>): CardEntry?
}
