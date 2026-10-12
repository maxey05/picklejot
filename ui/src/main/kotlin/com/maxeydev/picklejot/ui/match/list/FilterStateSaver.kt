@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.ui.match.list

import androidx.lifecycle.SavedStateHandle
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
private data class SavedFilterState(
    val format: String? = null,
    val result: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val opponentId: String? = null,
    val location: String? = null,
)

fun observeHomeFilter(homeEntryState: SavedStateHandle): Flow<FilterState> =
    homeEntryState
        .getStateFlow<String?>(FILTER_STATE_KEY, null)
        .map(::decodeFilterState)

internal fun encodeFilterState(filter: FilterState): String? {
    if (!filter.isActive) {
        return null
    }
    val saved =
        SavedFilterState(
            format = filter.format?.name,
            result = filter.result?.name,
            fromDate = filter.fromDate?.toString(),
            toDate = filter.toDate?.toString(),
            opponentId = filter.opponentId?.toString(),
            location = filter.location,
        )
    return Json.encodeToString(SavedFilterState.serializer(), saved)
}

internal fun decodeFilterState(encoded: String?): FilterState {
    if (encoded == null) {
        return FilterState.NONE
    }
    val saved = Json.decodeFromString(SavedFilterState.serializer(), encoded)
    return FilterState(
        format = saved.format?.let(MatchFormat::valueOf),
        result = saved.result?.let(MatchResult::valueOf),
        fromDate = saved.fromDate?.let(AppDate::parse),
        toDate = saved.toDate?.let(AppDate::parse),
        opponentId = saved.opponentId?.let(Uuid::parse),
        location = saved.location,
    )
}
