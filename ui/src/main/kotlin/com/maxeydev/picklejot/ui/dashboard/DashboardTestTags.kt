package com.maxeydev.picklejot.ui.dashboard

object DashboardTestTags {
    const val HEADER = "dashboard_header"
    const val NAME = "dashboard_name"
    const val RECORD = "dashboard_record"
    const val WIN_PERCENT = "dashboard_win_percent"
    const val STREAK = "dashboard_streak"
    const val FILTER_INDICATOR = "dashboard_filter_indicator"
    const val INFO_BUTTON = "dashboard_info_button"
    const val INFO_SHEET = "dashboard_info_sheet"
    const val INFO_DISMISS = "dashboard_info_dismiss"
    const val STATS_SCREEN = "stats_screen"
    const val STATS_MATCHES = "stats_matches"
    const val STATS_RECORD = "stats_record"
    const val STATS_WIN_RATE = "stats_win_rate"
    const val STATS_CURRENT_STREAK = "stats_current_streak"
    const val STATS_LONGEST_STREAK = "stats_longest_streak"
    const val STATS_CURRENT_IS_LONGEST = "stats_current_is_longest"
    const val STATS_SINGLES = "stats_singles"
    const val STATS_DOUBLES = "stats_doubles"
    const val STATS_PRO_SECTION = "stats_pro_section"
    const val STATS_SKIPS_HELD = "stats_skips_held"
    const val STATS_ADVANCED_SECTION = "stats_advanced_section"
    const val STATS_ADVANCED_EMPTY = "stats_advanced_empty"
    const val STATS_LOCKED_PREVIEW = "stats_locked_preview"
    const val STATS_LOCKED_PREVIEW_NAME = "stats_locked_preview_name"
    const val STATS_LOCKED_PREVIEW_RECORD = "stats_locked_preview_record"
    const val GROUP_HEAD_TO_HEAD = "head_to_head"
    const val GROUP_PARTNER = "partner"
    const val GROUP_LOCATION = "location"
    const val GROUP_PADDLE = "paddle"
    const val GROUP_MONTH = "month"
    const val STATS_PRO_TREND = "stats_pro_trend"
    const val STATS_PRO_PER_WEEK = "stats_pro_per_week"
    const val STATS_PRO_DAYS = "stats_pro_days"
    const val STATS_PRO_LAST_TEN = "stats_pro_last_ten"
    const val STATS_PRO_HEATMAP = "stats_pro_heatmap"
    const val STATS_PRO_HEATMAP_SIX_MONTHS = "stats_pro_heatmap_six_months"
    const val STATS_PRO_HEATMAP_YEAR = "stats_pro_heatmap_year"
    const val STATS_PRO_FORM = "stats_pro_form"
    const val STATS_PRO_WEEKDAYS = "stats_pro_weekdays"

    fun proRow(index: Int): String = "stats_pro_row_$index"

    fun advancedRow(
        group: String,
        index: Int,
    ): String = "stats_advanced_${group}_$index"

    fun advancedEmpty(group: String): String = "stats_advanced_${group}_empty"

    fun advancedToggle(group: String): String = "stats_advanced_${group}_toggle"
}
