package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object NaiveStreakOracle {
    fun compute(
        matchDates: List<AppDate>,
        today: AppDate,
    ): StreakResult {
        if (matchDates.isEmpty()) {
            return StreakResult.NONE
        }
        val played = matchDates.map { LocalDate.ofEpochDay(it.toEpochDays()) }.toSet()
        val thisMonday = mondayOf(LocalDate.ofEpochDay(today.toEpochDays()))
        val anchor =
            when {
                playedInWeekStarting(thisMonday, played) -> thisMonday
                playedInWeekStarting(thisMonday.minusDays(7), played) -> thisMonday.minusDays(7)
                else -> null
            }
        var current = 0
        if (anchor != null) {
            var monday: LocalDate = anchor
            while (playedInWeekStarting(monday, played)) {
                current += 1
                monday = monday.minusDays(7)
            }
        }
        var longest = 0
        var run = 0
        var monday = mondayOf(played.min())
        val lastMonday = mondayOf(played.max())
        while (!monday.isAfter(lastMonday)) {
            run = if (playedInWeekStarting(monday, played)) run + 1 else 0
            longest = maxOf(longest, run)
            monday = monday.plusDays(7)
        }
        return StreakResult(current = current, longest = longest)
    }

    private fun mondayOf(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private fun playedInWeekStarting(
        monday: LocalDate,
        played: Set<LocalDate>,
    ): Boolean = (0L..6L).any { monday.plusDays(it) in played }
}
