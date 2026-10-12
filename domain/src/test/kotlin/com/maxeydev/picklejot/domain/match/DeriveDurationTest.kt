package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.datetime.AppTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class DeriveDurationTest {
    private fun time(value: String): AppTime = AppTime.parse(value)

    @Test
    fun `a same day range yields the difference`() {
        assertEquals(1.hours + 15.minutes, deriveDuration(time("18:30"), time("19:45")))
    }

    @Test
    fun `a range crossing midnight yields a positive duration ending the next day`() {
        assertEquals(1.hours + 30.minutes, deriveDuration(time("23:15"), time("00:45")))
    }

    @Test
    fun `a range ending one minute after midnight from one minute before is two minutes`() {
        assertEquals(2.minutes, deriveDuration(time("23:59"), time("00:01")))
    }

    @Test
    fun `equal start and end yield zero rather than a full day`() {
        assertEquals(Duration.ZERO, deriveDuration(time("20:00"), time("20:00")))
    }

    @Test
    fun `a missing start yields no duration`() {
        assertNull(deriveDuration(null, time("20:00")))
    }

    @Test
    fun `a missing end yields no duration`() {
        assertNull(deriveDuration(time("20:00"), null))
    }

    @Test
    fun `both endpoints missing yield no duration`() {
        assertNull(deriveDuration(null, null))
    }

    @Test
    fun `no derived duration is ever negative across every quarter hour pair`() {
        val quarterHours = (0 until 96).map { index -> AppTime(index / 4, (index % 4) * 15) }
        quarterHours.forEach { start ->
            quarterHours.forEach { end ->
                val duration = requireNotNull(deriveDuration(start, end))
                assertTrue("$start to $end gave $duration", duration >= Duration.ZERO && duration < 24.hours)
            }
        }
    }

    @Test
    fun `crossing midnight is reported only when the end is earlier than the start`() {
        assertTrue(crossesMidnight(time("23:15"), time("00:45")))
        assertFalse(crossesMidnight(time("18:30"), time("19:45")))
        assertFalse(crossesMidnight(time("20:00"), time("20:00")))
        assertFalse(crossesMidnight(null, time("00:45")))
    }
}
