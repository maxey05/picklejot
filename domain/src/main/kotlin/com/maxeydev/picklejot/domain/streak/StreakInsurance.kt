package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.profile.Entitlement

private const val MONTHS_PER_YEAR = 12

internal data class SkipCoverage(
    val weeks: List<WeekKey>,
    val balance: Int,
    val grantedThroughMonth: Int,
)

object StreakInsurance {
    const val MAX_HELD = 2

    fun compute(
        matchDates: Iterable<AppDate>,
        today: AppDate,
        proSince: AppDate?,
    ): InsuredStreak {
        val dates = matchDates.toList()
        if (proSince == null) {
            return InsuredStreak(streak = computeStreak(dates, today), skippedWeeks = emptyList(), skipsHeld = 0)
        }
        val played = dates.mapTo(HashSet()) { WeekKey.of(it) }
        val thisWeek = WeekKey.of(today)
        val coverage = skipCoverage(played, thisWeek.previous(), proSince)
        val skipped = coverage.weeks.toSet()
        val counted = played + skipped
        return InsuredStreak(
            streak =
                StreakResult(
                    current = playedCurrent(counted, skipped, thisWeek),
                    longest = playedLongest(counted, skipped),
                ),
            skippedWeeks = coverage.weeks,
            skipsHeld = heldOn(coverage, today),
        )
    }

    fun canCover(
        matchDates: Iterable<AppDate>,
        week: WeekKey,
        proSince: AppDate,
    ): Boolean {
        val played = matchDates.mapTo(HashSet()) { WeekKey.of(it) }
        return week in skipCoverage(played, week, proSince).weeks
    }

    internal fun skipCoverage(
        played: Set<WeekKey>,
        through: WeekKey,
        proSince: AppDate,
    ): SkipCoverage {
        var grantedThroughMonth = monthIndex(proSince) - 1
        var balance = 0
        val covered = ArrayList<WeekKey>()
        var week = played.minOrNull() ?: return SkipCoverage(emptyList(), balance, grantedThroughMonth)
        var alive = false
        while (week <= through) {
            if (week in played) {
                alive = true
            } else if (alive) {
                val following = week.next()
                val isEligible = proSince <= following.sunday
                if (isEligible) {
                    val resolvedMonth = monthIndex(maxOf(following.monday, proSince))
                    if (resolvedMonth > grantedThroughMonth) {
                        balance = minOf(MAX_HELD, balance + (resolvedMonth - grantedThroughMonth))
                        grantedThroughMonth = resolvedMonth
                    }
                }
                if (isEligible && balance > 0) {
                    balance -= 1
                    covered += week
                } else {
                    alive = false
                }
            }
            week = week.next()
        }
        return SkipCoverage(weeks = covered, balance = balance, grantedThroughMonth = grantedThroughMonth)
    }

    private fun heldOn(
        coverage: SkipCoverage,
        today: AppDate,
    ): Int = minOf(MAX_HELD, coverage.balance + maxOf(0, monthIndex(today) - coverage.grantedThroughMonth))

    private fun playedCurrent(
        weeks: Set<WeekKey>,
        skipped: Set<WeekKey>,
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
            if (week !in skipped) {
                count += 1
            }
            week = week.previous()
        }
        return count
    }

    private fun playedLongest(
        weeks: Set<WeekKey>,
        skipped: Set<WeekKey>,
    ): Int {
        var longest = 0
        var run = 0
        var previous: WeekKey? = null
        for (week in weeks.sorted()) {
            val continuesRun = previous != null && week.ordinal == previous.ordinal + 1
            if (!continuesRun) {
                run = 0
            }
            if (week !in skipped) {
                run += 1
            }
            if (run > longest) {
                longest = run
            }
            previous = week
        }
        return longest
    }

    private fun monthIndex(date: AppDate): Int = date.year * MONTHS_PER_YEAR + date.month.ordinal
}

fun Entitlement.streakInsuranceStart(): AppInstant? = if (isPro) proSince ?: lastVerifiedAt else null
