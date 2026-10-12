package com.maxeydev.picklejot.domain.match

import com.maxeydev.picklejot.domain.datetime.AppTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private val ONE_DAY: Duration = 24.hours

fun deriveDuration(
    startTime: AppTime?,
    endTime: AppTime?,
): Duration? {
    if (startTime == null || endTime == null) {
        return null
    }
    val start = startTime.toSecondOfDay().seconds
    val end = endTime.toSecondOfDay().seconds
    return if (end >= start) {
        end - start
    } else {
        end + ONE_DAY - start
    }
}

fun crossesMidnight(
    startTime: AppTime?,
    endTime: AppTime?,
): Boolean = startTime != null && endTime != null && endTime < startTime
