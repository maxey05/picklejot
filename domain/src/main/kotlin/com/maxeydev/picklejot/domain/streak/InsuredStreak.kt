package com.maxeydev.picklejot.domain.streak

data class InsuredStreak(
    val streak: StreakResult,
    val skippedWeeks: List<WeekKey>,
    val skipsHeld: Int,
) {
    companion object {
        val NONE: InsuredStreak = InsuredStreak(streak = StreakResult.NONE, skippedWeeks = emptyList(), skipsHeld = 0)
    }
}
