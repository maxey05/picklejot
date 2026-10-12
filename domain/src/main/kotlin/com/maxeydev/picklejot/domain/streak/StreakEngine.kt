package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppTimeZone
import com.maxeydev.picklejot.domain.match.Match
import kotlin.time.Clock

class StreakEngine(
    private val clock: Clock,
    private val timeZone: () -> AppTimeZone,
) {
    fun today(): AppDate = WeekKey.localDateOf(clock.now(), timeZone())

    fun compute(matchDates: Iterable<AppDate>): StreakResult = computeStreak(matchDates, today())

    fun computeForMatches(matches: Iterable<Match>): StreakResult = computeStreakForMatches(matches, today())
}

fun computeStreakForMatches(
    matches: Iterable<Match>,
    today: AppDate,
): StreakResult = computeStreak(matches.map { it.date }, today)

fun computeStreak(
    matchDates: Iterable<AppDate>,
    today: AppDate,
): StreakResult {
    val weeks = matchDates.mapTo(HashSet()) { WeekKey.of(it) }
    if (weeks.isEmpty()) {
        return StreakResult.NONE
    }
    return StreakResult(
        current = currentStreak(weeks, WeekKey.of(today)),
        longest = longestStreak(weeks),
    )
}

internal fun currentStreak(
    weeks: Set<WeekKey>,
    thisWeek: WeekKey,
): Int {
    val lastWeek = thisWeek.previous()
    val anchor =
        when {
            thisWeek in weeks -> thisWeek
            lastWeek in weeks -> lastWeek
            else -> return 0
        }
    var count = 0
    var week = anchor
    while (week in weeks) {
        count += 1
        week = week.previous()
    }
    return count
}

internal fun longestStreak(weeks: Set<WeekKey>): Int {
    var longest = 0
    var run = 0
    var previous: WeekKey? = null
    for (week in weeks.sorted()) {
        run = if (previous != null && week.ordinal == previous.ordinal + 1) run + 1 else 1
        if (run > longest) {
            longest = run
        }
        previous = week
    }
    return longest
}
