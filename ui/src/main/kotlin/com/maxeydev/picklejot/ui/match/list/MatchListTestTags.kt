package com.maxeydev.picklejot.ui.match.list

import com.maxeydev.picklejot.domain.match.FilterKind
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.match.MatchSort

object MatchListTestTags {
    const val SETTINGS = "match_list_settings"
    const val NEW_MATCH = "match_list_new_match"
    const val LIST = "match_list"
    const val EMPTY_STATE = "match_list_empty"
    const val EMPTY_LOG_MATCH = "match_list_empty_log_match"
    const val NO_RESULTS = "match_list_no_results"
    const val NO_RESULTS_CLEAR = "match_list_no_results_clear"
    const val SORT_BUTTON = "match_list_sort"
    const val SEARCH_FIELD = "match_list_search"
    const val SEARCH_CLEAR = "match_list_search_clear"
    const val FILTER_BUTTON = "match_list_filter"
    const val FILTER_CHIPS = "match_list_filter_chips"
    const val CLEAR_ALL_FILTERS = "match_list_filter_clear_all"
    const val FILTER_SHEET = "filter_sheet"
    const val FIRST_MATCH_SHEET = "first_match_sheet"
    const val SHEET_CLEAR_ALL = "filter_sheet_clear_all"
    const val SHEET_DONE = "filter_sheet_done"
    const val DATE_PICKER = "filter_sheet_date"
    const val DATE_CLEAR = "filter_sheet_date_clear"
    const val DATE_CONFIRM = "filter_sheet_date_confirm"
    const val OPPONENT_PICKER = "filter_sheet_opponent"
    const val LOCATION_PICKER = "filter_sheet_location"
    const val RESULT_BADGE = "match_row_result"
    const val TEXT_COLUMN = "match_row_text"
    const val HEADLINE = "match_row_headline"
    const val DETAILS = "match_row_details"
    const val SCORES = "match_row_scores"
    const val SCORE_ENTRY = "match_row_score_entry"
    const val THUMBNAIL = "match_row_thumbnail"
    const val COUNT = "match_list_count"
    const val LOCATION = "match_row_location"
    const val ROW_DATE = "match_row_date"

    fun row(id: String): String = "match_row_$id"

    fun sortOption(sort: MatchSort): String = "match_list_sort_${sort.name.lowercase()}"

    fun filterChip(kind: FilterKind): String = "match_list_filter_chip_${kind.name.lowercase()}"

    fun formatOption(format: MatchFormat?): String = "filter_sheet_format_${format?.name?.lowercase() ?: "any"}"

    fun resultOption(result: MatchResult?): String = "filter_sheet_result_${result?.name?.lowercase() ?: "any"}"

    fun datePreset(preset: DatePreset): String = "filter_sheet_date_${preset.name.lowercase()}"

    fun opponentOption(personId: String): String = "filter_sheet_opponent_$personId"

    fun locationOption(index: Int): String = "filter_sheet_location_$index"
}
