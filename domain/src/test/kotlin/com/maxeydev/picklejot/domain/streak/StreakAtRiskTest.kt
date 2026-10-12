package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakAtRiskTest {
    private val wednesday = date("2026-09-30")

    private fun date(value: String): AppDate = AppDate.parse(value)

    private fun dates(vararg values: String): List<AppDate> = values.map { date(it) }

    private fun assess(
        today: AppDate,
        proSince: String?,
        vararg matchDates: String,
    ): StreakRisk = StreakAtRisk.assess(dates(*matchDates), today, proSince?.let { date(it) })

    @Test
    fun `a streak that was last played last week is at risk`() {
        assertEquals(StreakRisk.AT_RISK, assess(wednesday, null, "2026-09-08", "2026-09-15", "2026-09-22"))
    }

    @Test
    fun `having played this week is never at risk`() {
        assertEquals(StreakRisk.NONE, assess(wednesday, null, "2026-09-22", "2026-09-28"))
        assertEquals(StreakRisk.NONE, assess(wednesday, "2026-01-01", "2026-09-22", "2026-09-30"))
    }

    @Test
    fun `no matches at all is never at risk`() {
        assertEquals(StreakRisk.NONE, assess(wednesday, null))
        assertEquals(StreakRisk.NONE, assess(wednesday, "2026-01-01"))
    }

    @Test
    fun `a streak that already broke is not at risk`() {
        assertEquals(StreakRisk.NONE, assess(wednesday, null, "2026-08-25", "2026-09-01"))
    }

    @Test
    fun `a pro user with a banked skip is told a skip is available`() {
        assertEquals(
            StreakRisk.AT_RISK_SKIP_AVAILABLE,
            assess(wednesday, "2026-01-01", "2026-09-08", "2026-09-15", "2026-09-22"),
        )
    }

    @Test
    fun `a pro user whose skips are spent is at risk without a skip`() {
        val midMonth = date("2026-09-16")

        assertEquals(StreakRisk.AT_RISK, assess(midMonth, "2026-09-01", "2026-08-25", "2026-09-08"))
    }

    @Test
    fun `the risk follows the calendar week not the days since the last match`() {
        val sundayNight = date("2026-09-27")
        val mondayMorning = date("2026-09-28")

        assertEquals(StreakRisk.NONE, assess(sundayNight, null, "2026-09-21"))
        assertEquals(StreakRisk.AT_RISK, assess(mondayMorning, null, "2026-09-21", "2026-09-14"))
    }
}
