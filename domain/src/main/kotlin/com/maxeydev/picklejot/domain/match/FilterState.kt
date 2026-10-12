@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.datetime.AppDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class FilterKind {
    FORMAT,
    RESULT,
    DATE_RANGE,
    OPPONENT,
    LOCATION,
}

data class FilterState(
    val format: MatchFormat? = null,
    val result: MatchResult? = null,
    val fromDate: AppDate? = null,
    val toDate: AppDate? = null,
    val opponentId: Uuid? = null,
    val location: String? = null,
) {
    init {
        require(fromDate == null || toDate == null || fromDate <= toDate) {
            "A date range must not end before it starts, but was $fromDate to $toDate."
        }
        require(location == null || location.isNotBlank()) {
            "A location filter needs a non-blank location."
        }
    }

    val activeKinds: List<FilterKind>
        get() =
            FilterKind.entries.filter { kind ->
                when (kind) {
                    FilterKind.FORMAT -> format != null
                    FilterKind.RESULT -> result != null
                    FilterKind.DATE_RANGE -> fromDate != null || toDate != null
                    FilterKind.OPPONENT -> opponentId != null
                    FilterKind.LOCATION -> location != null
                }
            }

    val isActive: Boolean
        get() = activeKinds.isNotEmpty()

    fun without(kind: FilterKind): FilterState =
        when (kind) {
            FilterKind.FORMAT -> copy(format = null)
            FilterKind.RESULT -> copy(result = null)
            FilterKind.DATE_RANGE -> copy(fromDate = null, toDate = null)
            FilterKind.OPPONENT -> copy(opponentId = null)
            FilterKind.LOCATION -> copy(location = null)
        }

    companion object {
        val NONE: FilterState = FilterState()
    }
}
