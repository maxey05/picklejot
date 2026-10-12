package com.maxeydev.picklejot.ui.share

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.MatchFormat
import kotlin.time.Duration

class FakeCardLabels : CardLabels {
    override val brand: String = "Picklejot"

    override fun meta(
        format: MatchFormat,
        date: AppDate,
    ): String = "${format.name} · $date"

    override fun partner(name: String): CardEntry = CardEntry("With", listOf(name))

    override fun time(duration: Duration): CardEntry = CardEntry("Time", listOf("${duration.inWholeMinutes}m"))

    override fun opponents(names: List<String>): CardEntry? =
        names.takeIf { it.isNotEmpty() }?.let { CardEntry("Against", it) }

    override fun games(games: List<GameScore>): CardEntry? =
        games.takeIf { it.isNotEmpty() }?.let { list ->
            CardEntry("Games", list.map { "${it.myScore}–${it.opponentScore}" })
        }
}
