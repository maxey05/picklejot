package com.maxeydev.picklejot.ui.share

import android.content.Context
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.match.formatMatchDate
import kotlin.time.Duration

private const val MINUTES_PER_HOUR = 60L

class ResourceCardLabels(
    private val context: Context,
) : CardLabels {
    private val resources = context.resources

    override val brand: String
        get() = resources.getString(R.string.card_brand)

    override fun meta(
        format: MatchFormat,
        date: AppDate,
    ): String {
        val formatName =
            when (format) {
                MatchFormat.SINGLES -> resources.getString(R.string.format_singles)
                MatchFormat.DOUBLES -> resources.getString(R.string.format_doubles)
            }
        val locale = resources.configuration.locales[0]
        return resources.getString(R.string.list_format_and_date, formatName, formatMatchDate(date, locale))
    }

    override fun partner(name: String): CardEntry =
        CardEntry(caption = resources.getString(R.string.card_caption_with), values = listOf(name))

    override fun time(duration: Duration): CardEntry {
        val totalMinutes = duration.inWholeMinutes
        val hours = (totalMinutes / MINUTES_PER_HOUR).toInt()
        val minutes = (totalMinutes % MINUTES_PER_HOUR).toInt()
        val text =
            if (hours > 0) {
                resources.getString(R.string.duration_short_hours_minutes, hours, minutes)
            } else {
                resources.getString(R.string.duration_short_minutes, minutes)
            }
        return CardEntry(caption = resources.getString(R.string.card_caption_time), values = listOf(text))
    }

    override fun opponents(names: List<String>): CardEntry? =
        if (names.isEmpty()) {
            null
        } else {
            CardEntry(caption = resources.getString(R.string.card_caption_against), values = names)
        }

    override fun games(games: List<GameScore>): CardEntry? =
        if (games.isEmpty()) {
            null
        } else {
            CardEntry(
                caption = resources.getString(R.string.card_caption_games),
                values = games.map { resources.getString(R.string.list_score, it.myScore, it.opponentScore) },
            )
        }
}
