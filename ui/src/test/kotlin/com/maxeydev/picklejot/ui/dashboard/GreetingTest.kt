package com.maxeydev.picklejot.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class GreetingTest {
    private fun pick(
        hour: Int = 10,
        day: DayOfWeek = DayOfWeek.WEDNESDAY,
        bucket: Int,
        variant: Int = 0,
    ): Greeting = pickGreeting(hour = hour, dayOfWeek = day, bucket = bucket, variant = variant)

    @Test
    fun `the first buckets greet by time of day`() {
        assertEquals(Greeting.GOOD_MORNING, pick(hour = 5, bucket = 0))
        assertEquals(Greeting.GOOD_MORNING, pick(hour = 11, bucket = 0))
        assertEquals(Greeting.GOOD_AFTERNOON, pick(hour = 12, bucket = 0))
        assertEquals(Greeting.GOOD_AFTERNOON, pick(hour = 17, bucket = 0))
        assertEquals(Greeting.GOOD_EVENING, pick(hour = 18, bucket = 0))
    }

    @Test
    fun `late night still reads as evening`() {
        assertEquals(Greeting.GOOD_EVENING, pick(hour = 23, bucket = 0))
        assertEquals(Greeting.GOOD_EVENING, pick(hour = 2, bucket = 0))
    }

    @Test
    fun `the middle buckets greet by day of week`() {
        val expected =
            mapOf(
                DayOfWeek.MONDAY to Greeting.HAPPY_MONDAY,
                DayOfWeek.TUESDAY to Greeting.HAPPY_TUESDAY,
                DayOfWeek.WEDNESDAY to Greeting.HAPPY_WEDNESDAY,
                DayOfWeek.THURSDAY to Greeting.HAPPY_THURSDAY,
                DayOfWeek.FRIDAY to Greeting.HAPPY_FRIDAY,
                DayOfWeek.SATURDAY to Greeting.HAPPY_SATURDAY,
                DayOfWeek.SUNDAY to Greeting.HAPPY_SUNDAY,
            )

        DayOfWeek.entries.forEach { day ->
            assertEquals(expected.getValue(day), pick(day = day, bucket = 40))
        }
    }

    @Test
    fun `the last buckets pick a casual greeting by variant`() {
        val casual =
            setOf(Greeting.WHATS_UP, Greeting.HEY_THERE, Greeting.WELCOME_BACK, Greeting.READY_TO_PLAY)

        val picked = (0 until 4).map { pick(bucket = 99, variant = it) }

        assertEquals(casual, picked.toSet())
    }

    @Test
    fun `a negative variant still lands on a casual greeting`() {
        assertEquals(Greeting.READY_TO_PLAY, pick(bucket = 99, variant = -1))
    }

    @Test
    fun `time of day and day of week outweigh the casual greetings`() {
        val weights = (0 until 100).groupingBy { pick(bucket = it, variant = 0) }.eachCount()

        val contextual = weights.filterKeys { it != Greeting.WHATS_UP }.values.sum()

        assertTrue(contextual > weights.getValue(Greeting.WHATS_UP))
        assertEquals(100, weights.values.sum())
    }
}
