package com.maxeydev.picklejot.domain.streak

data class StreakResult(
    val current: Int,
    val longest: Int,
) {
    init {
        require(current >= 0) { "current streak cannot be negative: $current" }
        require(longest >= current) { "longest streak $longest cannot be shorter than current $current" }
    }

    val isAlive: Boolean
        get() = current > 0

    val isCurrentTheLongest: Boolean
        get() = current > 0 && current == longest

    companion object {
        val NONE: StreakResult = StreakResult(current = 0, longest = 0)
    }
}
