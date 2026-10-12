package com.maxeydev.picklejot.domain.match

enum class MatchSort {
    DATE_NEWEST,
    DATE_OLDEST,
    RESULT_WINS_FIRST,
    RESULT_LOSSES_FIRST,
    OPPONENT_A_TO_Z,
    LOCATION_A_TO_Z,
    DURATION_SHORTEST,
    DURATION_LONGEST,
    ;

    companion object {
        val DEFAULT: MatchSort = DATE_NEWEST
    }
}
