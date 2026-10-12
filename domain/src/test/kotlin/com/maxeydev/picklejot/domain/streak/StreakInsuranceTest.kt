package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.profile.Entitlement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StreakInsuranceTest {
    private val today = date("2026-09-30")

    private fun date(value: String): AppDate = AppDate.parse(value)

    private fun dates(vararg values: String): List<AppDate> = values.map { date(it) }

    private fun week(monday: String): WeekKey = WeekKey.of(date(monday))

    private fun insured(
        proSince: String?,
        vararg matchDates: String,
    ): InsuredStreak = StreakInsurance.compute(dates(*matchDates), today, proSince?.let { date(it) })

    @Test
    fun `a missed week in the middle is bridged by a banked skip and adds nothing to the count`() {
        val result = insured("2026-01-01", "2026-09-01", "2026-09-08", "2026-09-15", "2026-09-29")

        assertEquals(4, result.streak.current)
        assertEquals(4, result.streak.longest)
        assertEquals(listOf(week("2026-09-21")), result.skippedWeeks)
        assertEquals(1, result.skipsHeld)
    }

    @Test
    fun `a missed last week is bridged while this week is still open`() {
        val result = insured("2026-01-01", "2026-09-01", "2026-09-08", "2026-09-15")

        assertEquals(3, result.streak.current)
        assertEquals(listOf(week("2026-09-21")), result.skippedWeeks)
    }

    @Test
    fun `a free user gets exactly the engine result and no skips`() {
        val matches = dates("2026-09-01", "2026-09-08", "2026-09-29")

        val result = StreakInsurance.compute(matches, today, proSince = null)

        assertEquals(computeStreak(matches, today), result.streak)
        assertTrue(result.skippedWeeks.isEmpty())
        assertEquals(0, result.skipsHeld)
    }

    @Test
    fun `upgrading grants only the current month's skip so a second missed week breaks the streak`() {
        val result = insured("2026-09-01", "2026-08-25", "2026-09-01", "2026-09-29")

        assertEquals(listOf(week("2026-09-07")), result.skippedWeeks)
        assertEquals(1, result.streak.current)
        assertEquals(2, result.streak.longest)
        assertEquals(0, result.skipsHeld)
    }

    @Test
    fun `no more than two skips are ever held however long they have accrued`() {
        val result = insured("2026-06-01", "2026-07-28")

        assertEquals(listOf(week("2026-08-03"), week("2026-08-10")), result.skippedWeeks)
        assertEquals(0, result.streak.current)
        assertEquals(1, result.skipsHeld)
    }

    @Test
    fun `a back-dated match into a covered week refunds the skip`() {
        val withSkip = insured("2026-01-01", "2026-09-01", "2026-09-08", "2026-09-15", "2026-09-29")
        val afterBackdating =
            insured("2026-01-01", "2026-09-01", "2026-09-08", "2026-09-15", "2026-09-22", "2026-09-29")

        assertEquals(listOf(week("2026-09-21")), withSkip.skippedWeeks)
        assertEquals(1, withSkip.skipsHeld)
        assertTrue(afterBackdating.skippedWeeks.isEmpty())
        assertEquals(2, afterBackdating.skipsHeld)
        assertEquals(5, afterBackdating.streak.current)
    }

    @Test
    fun `a week that ended before pro started is never covered`() {
        val result = insured("2026-09-30", "2026-09-01", "2026-09-08", "2026-09-22")

        assertTrue(result.skippedWeeks.isEmpty())
        assertEquals(1, result.streak.current)
    }

    @Test
    fun `upgrading in the week after a miss still covers that miss`() {
        val result = insured("2026-09-30", "2026-09-01", "2026-09-08", "2026-09-15")

        assertEquals(listOf(week("2026-09-21")), result.skippedWeeks)
        assertEquals(3, result.streak.current)
    }

    @Test
    fun `no skip is spent before there is a streak to protect`() {
        val result = insured("2026-01-01", "2026-09-29")

        assertTrue(result.skippedWeeks.isEmpty())
        assertEquals(1, result.streak.current)
    }

    @Test
    fun `no skip is spent once a streak has already broken`() {
        val result = insured("2026-06-01", "2026-07-28", "2026-09-29")

        assertEquals(listOf(week("2026-08-03"), week("2026-08-10")), result.skippedWeeks)
        assertEquals(1, result.streak.current)
    }

    @Test
    fun `a pro user with no matches holds the skips that accrued`() {
        val result = insured("2026-09-01")

        assertEquals(InsuredStreak.NONE.streak, result.streak)
        assertEquals(1, result.skipsHeld)
    }

    @Test
    fun `a pro start date after today never changes the engine result`() {
        val random = Random(20260930)
        repeat(200) {
            val matches =
                List(random.nextInt(0, 25)) { date("2026-01-05").plusWeeks(random.nextInt(0, 38)).plusDaysOf(random) }

            val result = StreakInsurance.compute(matches, today, date("2027-01-01"))

            assertEquals(NaiveStreakOracle.compute(matches, today), result.streak)
            assertTrue(result.skippedWeeks.isEmpty())
            assertEquals(0, result.skipsHeld)
        }
    }

    @Test
    fun `the start of insurance is the pro start date only for pro`() {
        val free = Entitlement(isPro = false, proSince = AppInstant.fromEpochMilliseconds(5))
        val pro = free.copy(isPro = true)

        assertEquals(null, free.streakInsuranceStart())
        assertEquals(AppInstant.fromEpochMilliseconds(5), pro.streakInsuranceStart())
    }
}

private fun AppDate.plusWeeks(weeks: Int): AppDate = AppDate.fromEpochDays(toEpochDays() + weeks * 7L)

private fun AppDate.plusDaysOf(random: Random): AppDate = AppDate.fromEpochDays(toEpochDays() + random.nextInt(0, 7))
