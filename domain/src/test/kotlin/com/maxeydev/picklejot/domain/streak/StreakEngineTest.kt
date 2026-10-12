@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.datetime.AppTimeZone
import com.maxeydev.picklejot.domain.datetime.atTimeIn
import com.maxeydev.picklejot.domain.datetime.minusDays
import com.maxeydev.picklejot.domain.datetime.plusDays
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StreakEngineTest {
    private val manila = AppTimeZone.of("Asia/Manila")

    private fun date(value: String): AppDate = AppDate.parse(value)

    private fun dates(vararg values: String): List<AppDate> = values.map { date(it) }

    private fun streak(
        today: String,
        vararg matchDates: String,
    ): StreakResult = computeStreak(dates(*matchDates), date(today))

    private fun localInstant(
        value: String,
        zone: AppTimeZone,
    ): AppInstant = date(value.substringBefore('T')).atTimeIn(AppTime.parse(value.substringAfter('T')), zone)

    private fun engineAt(
        instant: AppInstant,
        zone: AppTimeZone,
    ): StreakEngine {
        val clock =
            object : Clock {
                override fun now(): AppInstant = instant
            }
        return StreakEngine(clock) { zone }
    }

    private fun match(
        date: String,
        createdAt: AppInstant,
    ): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.SINGLES,
            date = date(date),
            result = MatchResult.WIN,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

    @Test
    fun `no matches gives a zero streak without throwing`() {
        assertEquals(StreakResult.NONE, computeStreak(emptyList(), date("2026-09-22")))
        assertEquals(0, StreakResult.NONE.current)
        assertEquals(0, StreakResult.NONE.longest)
        assertFalse(StreakResult.NONE.isAlive)
    }

    @Test
    fun `the engine with an injected clock handles no matches`() {
        val engine = engineAt(localInstant("2026-09-22T09:00:00", manila), manila)
        assertEquals(StreakResult.NONE, engine.compute(emptyList()))
        assertEquals(StreakResult.NONE, engine.computeForMatches(emptyList()))
    }

    @Test
    fun `playing this week counts this week`() {
        assertEquals(StreakResult(current = 1, longest = 1), streak("2026-09-23", "2026-09-22"))
    }

    @Test
    fun `playing only last week keeps the streak alive`() {
        val result = streak("2026-09-23", "2026-09-17")
        assertEquals(1, result.current)
        assertTrue(result.isAlive)
    }

    @Test
    fun `on Tuesday with no match yet this week a streak built through last week is alive and unchanged`() {
        val matchDates = arrayOf("2026-09-01", "2026-09-10", "2026-09-15", "2026-09-20")
        val lastSunday = streak("2026-09-20", *matchDates)
        val tuesday = streak("2026-09-22", *matchDates)
        assertEquals(StreakResult(current = 3, longest = 3), lastSunday)
        assertEquals(lastSunday, tuesday)
    }

    @Test
    fun `a streak survives until the end of Sunday of the week with no match`() {
        val matchDates = arrayOf("2026-09-08", "2026-09-17")
        assertEquals(2, streak("2026-09-27", *matchDates).current)
    }

    @Test
    fun `a streak breaks on the Monday after a full calendar week with no match`() {
        val matchDates = arrayOf("2026-09-08", "2026-09-17")
        val result = streak("2026-09-28", *matchDates)
        assertEquals(0, result.current)
        assertEquals(2, result.longest)
        assertFalse(result.isAlive)
    }

    @Test
    fun `a gap week earlier in history ends the current run there`() {
        val result = streak("2026-09-23", "2026-09-22", "2026-09-15", "2026-09-01", "2026-08-25", "2026-08-18")
        assertEquals(2, result.current)
        assertEquals(3, result.longest)
    }

    @Test
    fun `the streak follows the match date and ignores when it was created`() {
        val createdThisWeek = localInstant("2026-09-22T20:00:00", manila)
        val matches =
            listOf(
                match("2026-08-11", createdThisWeek),
                match("2026-08-18", createdThisWeek),
                match("2026-08-25", createdThisWeek),
            )
        val result = computeStreakForMatches(matches, date("2026-09-22"))
        assertEquals(0, result.current)
        assertEquals(3, result.longest)
    }

    @Test
    fun `matches created long ago but dated recently count as recent play`() {
        val createdLongAgo = localInstant("2024-01-01T08:00:00", manila)
        val matches = listOf(match("2026-09-14", createdLongAgo), match("2026-09-21", createdLongAgo))
        val engine = engineAt(localInstant("2026-09-23T12:00:00", manila), manila)
        assertEquals(StreakResult(current = 2, longest = 2), engine.computeForMatches(matches))
    }

    @Test
    fun `back dating a match into a gap week joins the runs on either side`() {
        val before = listOf("2026-09-22", "2026-09-15", "2026-09-01", "2026-08-25")
        val gapWeekMatch = "2026-09-09"
        assertEquals(StreakResult(current = 2, longest = 2), streak("2026-09-23", *before.toTypedArray()))
        assertEquals(
            StreakResult(current = 5, longest = 5),
            streak("2026-09-23", *(before + gapWeekMatch).toTypedArray()),
        )
    }

    @Test
    fun `back dating into a gap revives a broken streak`() {
        val before = listOf("2026-09-17", "2026-09-01")
        assertEquals(1, streak("2026-09-23", *before.toTypedArray()).current)
        assertEquals(3, streak("2026-09-23", *(before + "2026-09-08").toTypedArray()).current)
    }

    @Test
    fun `two matches in the same week count that week once`() {
        assertEquals(StreakResult(current = 1, longest = 1), streak("2026-09-24", "2026-09-21", "2026-09-24"))
    }

    @Test
    fun `many matches on the same day count that week once`() {
        val matchDates = List(25) { "2026-09-23" } + List(10) { "2026-09-16" }
        assertEquals(StreakResult(current = 2, longest = 2), streak("2026-09-23", *matchDates.toTypedArray()))
    }

    @Test
    fun `deleting the only match in a middle week splits the streak and current keeps only the latest run`() {
        val all = listOf("2026-09-22", "2026-09-15", "2026-09-08", "2026-09-01", "2026-08-25", "2026-08-18")
        assertEquals(StreakResult(current = 6, longest = 6), streak("2026-09-23", *all.toTypedArray()))
        val afterDeletion = all - "2026-09-08"
        assertEquals(StreakResult(current = 2, longest = 3), streak("2026-09-23", *afterDeletion.toTypedArray()))
    }

    @Test
    fun `deleting matches can lower the longest streak because it is not a high water mark`() {
        val all = listOf("2026-06-01", "2026-06-08", "2026-06-15", "2026-06-22", "2026-09-22")
        assertEquals(4, streak("2026-09-23", *all.toTypedArray()).longest)
        val afterDeletion = all - "2026-06-15"
        assertEquals(2, streak("2026-09-23", *afterDeletion.toTypedArray()).longest)
    }

    @Test
    fun `editing a match date recomputes both current and longest`() {
        val original = listOf("2026-09-22", "2026-09-15", "2026-09-08")
        assertEquals(StreakResult(current = 3, longest = 3), streak("2026-09-23", *original.toTypedArray()))
        val edited = listOf("2026-09-22", "2026-09-15", "2026-07-08")
        assertEquals(StreakResult(current = 2, longest = 2), streak("2026-09-23", *edited.toTypedArray()))
    }

    @Test
    fun `current and longest are both exposed and the equal case is distinguishable`() {
        val equal = streak("2026-09-23", "2026-09-22", "2026-09-15")
        assertEquals(2, equal.current)
        assertEquals(2, equal.longest)
        assertTrue(equal.isCurrentTheLongest)

        val shorter = streak("2026-09-23", "2026-09-22", "2026-08-04", "2026-08-11", "2026-08-18")
        assertEquals(1, shorter.current)
        assertEquals(3, shorter.longest)
        assertFalse(shorter.isCurrentTheLongest)

        val broken = streak("2026-09-23", "2026-08-04")
        assertEquals(0, broken.current)
        assertEquals(1, broken.longest)
        assertFalse(broken.isCurrentTheLongest)

        assertFalse(StreakResult.NONE.isCurrentTheLongest)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a result whose longest is shorter than current cannot be built`() {
        StreakResult(current = 3, longest = 2)
    }

    @Test
    fun `a match at 23 59 on a Sunday counts for the week that is ending`() {
        val sundayNight = localInstant("2026-09-27T23:59:00", manila)
        val engine = engineAt(sundayNight, manila)
        assertEquals(date("2026-09-27"), engine.today())
        val result = engine.compute(dates("2026-09-14", "2026-09-27"))
        assertEquals(StreakResult(current = 2, longest = 2), result)
    }

    @Test
    fun `a match at 00 00 on a Monday starts a new week rather than joining the previous one`() {
        val mondayMidnight = localInstant("2026-09-28T00:00:00", manila)
        val engine = engineAt(mondayMidnight, manila)
        assertEquals(date("2026-09-28"), engine.today())
        assertEquals(StreakResult(current = 1, longest = 1), engine.compute(dates("2026-09-28")))
        assertEquals(StreakResult(current = 2, longest = 2), engine.compute(dates("2026-09-27", "2026-09-28")))
        assertEquals(StreakResult(current = 1, longest = 1), engine.compute(dates("2026-09-14", "2026-09-28")))
    }

    @Test
    fun `the Sunday to Monday rollover moves the anchor one week without breaking the streak`() {
        val matchDates = dates("2026-09-14", "2026-09-21")
        val sunday = engineAt(localInstant("2026-09-27T23:59:59", manila), manila).compute(matchDates)
        val monday = engineAt(localInstant("2026-09-28T00:00:00", manila), manila).compute(matchDates)
        assertEquals(StreakResult(current = 2, longest = 2), sunday)
        assertEquals(sunday, monday)
    }

    @Test
    fun `a streak crossing from ISO week 52 into week 1 of the next year is continuous`() {
        val weeklyMondays = arrayOf("2025-12-08", "2025-12-15", "2025-12-22", "2025-12-29", "2026-01-05", "2026-01-12")
        val result = streak("2026-01-13", *weeklyMondays)
        assertEquals(StreakResult(current = 6, longest = 6), result)
    }

    @Test
    fun `a streak through ISO week 53 has no off by one`() {
        val result = streak("2027-01-05", "2026-12-14", "2026-12-21", "2026-12-28", "2027-01-04")
        assertEquals(StreakResult(current = 4, longest = 4), result)
    }

    @Test
    fun `a missed ISO week 53 breaks the streak`() {
        val result = streak("2027-01-05", "2026-12-14", "2026-12-21", "2027-01-04")
        assertEquals(StreakResult(current = 1, longest = 2), result)
    }

    @Test
    fun `on the first days of a new year last year's week 53 still keeps the streak alive`() {
        val result = streak("2027-01-05", "2026-12-21", "2026-12-31")
        assertEquals(StreakResult(current = 2, longest = 2), result)
    }

    @Test
    fun `a year with only 52 weeks does not invent a week 53`() {
        assertEquals(StreakResult(current = 2, longest = 2), streak("2025-12-30", "2025-12-22", "2025-12-29"))
    }

    @Test
    fun `a DST spring forward week counts once and every moment in it gives the same streak`() {
        val berlin = AppTimeZone.of("Europe/Berlin")
        val matchDates = dates("2026-03-09", "2026-03-18", "2026-03-23", "2026-03-29")
        assertDstWeekIsStable(berlin, "2026-03-23T00:00:00", "2026-03-30T00:00:00", matchDates, 3)
    }

    @Test
    fun `a DST fall back week counts once and every moment in it gives the same streak`() {
        val newYork = AppTimeZone.of("America/New_York")
        val matchDates = dates("2026-10-12", "2026-10-21", "2026-10-26", "2026-11-01")
        assertDstWeekIsStable(newYork, "2026-10-26T00:00:00", "2026-11-02T00:00:00", matchDates, 3)
    }

    private fun assertDstWeekIsStable(
        zone: AppTimeZone,
        weekStartLocal: String,
        nextWeekStartLocal: String,
        matchDates: List<AppDate>,
        expectedCurrent: Int,
    ) {
        val start = localInstant(weekStartLocal, zone)
        val end = localInstant(nextWeekStartLocal, zone)
        val dstWeek = WeekKey.of(date(weekStartLocal.substringBefore('T')))
        var instant = start
        var checked = 0
        while (instant < end) {
            val engine = engineAt(instant, zone)
            assertEquals("week at $instant", dstWeek, WeekKey.of(engine.today()))
            assertEquals(
                "streak at $instant",
                StreakResult(current = expectedCurrent, longest = expectedCurrent),
                engine.compute(matchDates),
            )
            instant += 15.minutes
            checked += 1
        }
        assertTrue("checked $checked quarter hours", checked in 7 * 96 - 4..7 * 96 + 4)
        val nextWeek = engineAt(end, zone)
        assertEquals(dstWeek.next(), WeekKey.of(nextWeek.today()))
        assertEquals(expectedCurrent, nextWeek.compute(matchDates).current)
    }

    @Test
    fun `flying from Manila to Los Angeles early on Monday does not break the streak`() {
        val matchDates = dates("2026-09-07", "2026-09-14", "2026-09-21")
        val instant = localInstant("2026-09-28T01:00:00", manila)
        val losAngeles = AppTimeZone.of("America/Los_Angeles")
        val inManila = engineAt(instant, manila).compute(matchDates)
        val inLosAngeles = engineAt(instant, losAngeles).compute(matchDates)
        assertEquals(StreakResult(current = 3, longest = 3), inManila)
        assertEquals(inManila, inLosAngeles)
    }

    @Test
    fun `flying east across the date line on Sunday does not throw and shifts at most one week`() {
        val matchDates = dates("2026-09-07", "2026-09-14")
        val instant = localInstant("2026-09-27T20:00:00", AppTimeZone.of("America/Los_Angeles"))
        val kiritimati = AppTimeZone.of("Pacific/Kiritimati")
        val home = engineAt(instant, AppTimeZone.of("America/Los_Angeles"))
        val away = engineAt(instant, kiritimati)
        assertEquals(1, WeekKey.of(away.today()).ordinal - WeekKey.of(home.today()).ordinal)
        assertEquals(StreakResult(current = 2, longest = 2), home.compute(matchDates))
        assertEquals(StreakResult(current = 0, longest = 2), away.compute(matchDates))
    }

    @Test
    fun `any zone change changes today by at most one week and the streak only by what that week justifies`() {
        val zones =
            listOf(
                "Pacific/Kiritimati",
                "Pacific/Auckland",
                "Asia/Manila",
                "Asia/Kolkata",
                "Europe/London",
                "UTC",
                "America/New_York",
                "America/Los_Angeles",
                "Pacific/Pago_Pago",
            ).map { AppTimeZone.of(it) }
        val random = Random(7)
        val matchDates = List(60) { date("2026-06-01").plusDays(random.nextInt(0, 140)) }
        var instant = localInstant("2026-09-01T00:00:00", AppTimeZone.UTC)
        val end = localInstant("2026-10-31T00:00:00", AppTimeZone.UTC)
        while (instant < end) {
            for (from in zones) {
                for (to in zones) {
                    val before = engineAt(instant, from)
                    val after = engineAt(instant, to)
                    val weekBefore = WeekKey.of(before.today())
                    val weekAfter = WeekKey.of(after.today())
                    assertTrue(abs(weekAfter.ordinal - weekBefore.ordinal) <= 1)
                    val justified =
                        listOf(weekBefore.previous(), weekBefore, weekBefore.next()).map {
                            computeStreak(matchDates, it.monday)
                        }
                    assertTrue("$instant $from -> $to", after.compute(matchDates) in justified)
                    assertEquals(before.compute(matchDates).longest, after.compute(matchDates).longest)
                }
            }
            instant += (6 * 60 + 17).minutes
        }
    }

    @Test
    fun `a 1000 match history always agrees with a day by day recomputation`() {
        listOf(1, 2, 3, 42, 2026).forEach { seed ->
            val random = Random(seed)
            val start = date("2023-01-01")
            val matchDates = mutableListOf<AppDate>()
            var cursor = start
            while (matchDates.size < 1000) {
                val gap = if (random.nextInt(10) == 0) random.nextInt(8, 30) else random.nextInt(0, 6)
                cursor = cursor.plusDays(gap)
                matchDates += cursor
            }
            val shuffled = matchDates.shuffled(random)
            val lastDate = matchDates.last()
            for (offset in 0..20) {
                val today = lastDate.plusDays(offset)
                assertEquals(
                    "seed $seed today $today",
                    NaiveStreakOracle.compute(shuffled, today),
                    computeStreak(shuffled, today),
                )
            }
            val trimmed = shuffled.drop(random.nextInt(1, 500))
            assertEquals(
                "seed $seed after deletions",
                NaiveStreakOracle.compute(trimmed, lastDate),
                computeStreak(trimmed, lastDate),
            )
        }
    }

    @Test
    fun `recomputing twice over the same matches gives the same answer`() {
        val matchDates = dates("2026-09-22", "2026-09-15", "2026-08-01")
        val today = date("2026-09-23")
        assertEquals(computeStreak(matchDates, today), computeStreak(matchDates, today))
        assertEquals(computeStreak(matchDates, today), computeStreak(matchDates.reversed(), today))
    }

    @Test
    fun `future dated matches do not create a current streak on their own`() {
        val result = streak("2026-09-23", "2026-10-14")
        assertEquals(0, result.current)
        assertEquals(1, result.longest)
    }

    @Test
    fun `a streak older than the anchor does not leak into current`() {
        val today = date("2026-09-23")
        val oldRun = (0 until 10).map { date("2026-01-05").plusDays(7 * it) }
        val result = computeStreak(oldRun + today.minusDays(1), today)
        assertEquals(1, result.current)
        assertEquals(10, result.longest)
    }
}
