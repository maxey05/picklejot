package com.maxeydev.picklejot.domain.stats

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult

data class MatchStatLine(
    val date: AppDate,
    val format: MatchFormat,
    val result: MatchResult,
)
