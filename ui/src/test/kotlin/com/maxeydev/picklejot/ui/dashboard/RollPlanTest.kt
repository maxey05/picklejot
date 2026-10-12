package com.maxeydev.picklejot.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RollPlanTest {
    @Test
    fun `an unchanged value has nothing to roll`() {
        assertEquals(emptyList<Int>(), RollPlan.steps(from = 62, to = 62))
    }

    @Test
    fun `a rise passes through every number in between`() {
        assertEquals(listOf(63, 64), RollPlan.steps(from = 62, to = 64))
    }

    @Test
    fun `a fall passes through every number in between`() {
        assertEquals(listOf(63, 62, 61), RollPlan.steps(from = 64, to = 61))
    }

    @Test
    fun `a long roll is thinned to ten steps and still lands on the target`() {
        val steps = RollPlan.steps(from = 0, to = 100)

        assertEquals(10, steps.size)
        assertEquals(100, steps.last())
        assertTrue(steps.zipWithNext().all { (earlier, later) -> later > earlier })
    }

    @Test
    fun `a long fall is thinned the same way`() {
        val steps = RollPlan.steps(from = 100, to = 0)

        assertEquals(10, steps.size)
        assertEquals(0, steps.last())
        assertTrue(steps.zipWithNext().all { (earlier, later) -> later < earlier })
    }

    @Test
    fun `only the final step eases over the full duration`() {
        val count = 4

        assertEquals(RollPlan.FINAL_MILLIS, RollPlan.durationMillis(index = count - 1, count = count))
    }

    @Test
    fun `an intermediate step lasts exactly its gap so the steps chain without a pause`() {
        val count = 4

        repeat(count - 1) { index ->
            assertEquals(RollPlan.gapMillis(index, count), RollPlan.durationMillis(index, count).toLong())
        }
    }

    @Test
    fun `the gaps widen as the roll settles`() {
        val count = 5

        val gaps = List(count - 1) { index -> RollPlan.gapMillis(index, count) }

        assertTrue(gaps.zipWithNext().all { (earlier, later) -> later >= earlier })
    }

    @Test
    fun `every gap stays inside a range that reads as one continuous motion`() {
        listOf(1, 2, 3, 6, 10).forEach { count ->
            repeat(count) { index ->
                val gap = RollPlan.gapMillis(index, count)

                assertTrue("gap $gap for step $index of $count", gap in 50L..230L)
            }
        }
    }
}
