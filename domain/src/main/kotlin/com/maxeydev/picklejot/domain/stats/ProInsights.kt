package com.maxeydev.picklejot.domain.stats

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.streak.WeekKey

private const val WEEKDAY_COUNT = 7
private const val DAYS_PER_WEEK = 7L
private const val EPOCH_DAY_TO_MONDAY_OFFSET = 3L
private const val TREND_WINDOW_DAYS = 90L
private const val RECENT_MATCH_COUNT = 10
private const val FORM_WINDOW = 10
private const val FORM_POINTS = 30
private const val WHOLE_PERCENT = 100

data class WeekdayRecord(
    val dayIndex: Int,
    val record: WinLoss,
)

data class ProInsights(
    val today: AppDate,
    val overall: WinLoss,
    val doubles: WinLoss,
    val lastTen: WinLoss,
    val trendPoints: Int?,
    val matchesPerWeek: Double,
    val daysPlayed: Int,
    val longestDayRun: Int,
    val form: List<Int>,
    val weekdays: List<WeekdayRecord>,
    val matchesByDay: Map<AppDate, Int>,
) {
    fun matchesOn(date: AppDate): Int = matchesByDay[date] ?: 0

    companion object {
        const val FORM_WINDOW_SIZE = FORM_WINDOW

        val EMPTY: ProInsights =
            ProInsights(
                today = AppDate.fromEpochDays(0L),
                overall = WinLoss.NONE,
                doubles = WinLoss.NONE,
                lastTen = WinLoss.NONE,
                trendPoints = null,
                matchesPerWeek = 0.0,
                daysPlayed = 0,
                longestDayRun = 0,
                form = emptyList(),
                weekdays = (0 until WEEKDAY_COUNT).map { WeekdayRecord(it, WinLoss.NONE) },
                matchesByDay = emptyMap(),
            )

        fun from(
            lines: List<AdvancedMatchLine>,
            today: AppDate,
        ): ProInsights {
            if (lines.isEmpty()) {
                return EMPTY.copy(today = today)
            }
            val ordered = lines.sortedBy { it.date }
            val matchesByDay = ordered.groupingBy { it.date }.eachCount()
            return ProInsights(
                today = today,
                overall = winLossOf(ordered),
                doubles = winLossOf(ordered.filter { it.format == MatchFormat.DOUBLES }),
                lastTen = winLossOf(ordered.takeLast(RECENT_MATCH_COUNT)),
                trendPoints = trendPoints(ordered, today),
                matchesPerWeek = matchesPerWeek(ordered, today),
                daysPlayed = matchesByDay.size,
                longestDayRun = longestDayRun(matchesByDay.keys),
                form = formSeries(ordered),
                weekdays = weekdayRecords(ordered),
                matchesByDay = matchesByDay,
            )
        }

        fun weekdayIndexOf(date: AppDate): Int =
            (date.toEpochDays() + EPOCH_DAY_TO_MONDAY_OFFSET).mod(DAYS_PER_WEEK).toInt()

        private fun winLossOf(lines: List<AdvancedMatchLine>): WinLoss =
            WinLoss(
                wins = lines.count { it.result == MatchResult.WIN },
                losses = lines.count { it.result == MatchResult.LOSS },
            )

        private fun trendPoints(
            ordered: List<AdvancedMatchLine>,
            today: AppDate,
        ): Int? {
            val todayDay = today.toEpochDays()
            val recentStart = todayDay - TREND_WINDOW_DAYS + 1
            val priorStart = todayDay - 2 * TREND_WINDOW_DAYS + 1
            val recent = ordered.filter { it.date.toEpochDays() in recentStart..todayDay }
            val prior = ordered.filter { it.date.toEpochDays() in priorStart until recentStart }
            val recentPercent = winLossOf(recent).percentIfEnoughMatches() ?: return null
            val priorPercent = winLossOf(prior).percentIfEnoughMatches() ?: return null
            return recentPercent - priorPercent
        }

        private fun matchesPerWeek(
            ordered: List<AdvancedMatchLine>,
            today: AppDate,
        ): Double {
            val firstWeek = WeekKey.of(ordered.first().date)
            val lastWeek = WeekKey.of(maxOf(ordered.last().date, today))
            val weeks = (lastWeek.ordinal - firstWeek.ordinal + 1).coerceAtLeast(1L)
            return ordered.size.toDouble() / weeks
        }

        private fun longestDayRun(days: Set<AppDate>): Int {
            val sortedDays = days.map { it.toEpochDays() }.sorted()
            var longest = 0
            var current = 0
            var previous: Long? = null
            for (day in sortedDays) {
                current = if (previous != null && day == previous + 1) current + 1 else 1
                longest = maxOf(longest, current)
                previous = day
            }
            return longest
        }

        private fun formSeries(ordered: List<AdvancedMatchLine>): List<Int> {
            if (ordered.size <= FORM_WINDOW) {
                return emptyList()
            }
            val wins = ordered.map { if (it.result == MatchResult.WIN) 1 else 0 }
            val firstEnd = maxOf(FORM_WINDOW - 1, wins.size - FORM_POINTS)
            return (firstEnd until wins.size).map { end ->
                val windowWins = wins.subList(end - FORM_WINDOW + 1, end + 1).sum()
                windowWins * WHOLE_PERCENT / FORM_WINDOW
            }
        }

        private fun weekdayRecords(ordered: List<AdvancedMatchLine>): List<WeekdayRecord> {
            val byDay = ordered.groupBy { weekdayIndexOf(it.date) }
            return (0 until WEEKDAY_COUNT).map { index ->
                WeekdayRecord(dayIndex = index, record = winLossOf(byDay[index].orEmpty()))
            }
        }
    }
}

fun WinLoss.pointsAbove(baseline: WinLoss): Int? {
    val own = percentIfEnoughMatches() ?: return null
    val reference = baseline.winPercent ?: return null
    return own - reference
}
