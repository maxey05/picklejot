package com.maxeydev.picklejot.domain.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

typealias AppDate = kotlinx.datetime.LocalDate

typealias AppTime = kotlinx.datetime.LocalTime

typealias AppInstant = kotlin.time.Instant

typealias AppTimeZone = kotlinx.datetime.TimeZone

fun AppInstant.toAppDateIn(zone: AppTimeZone): AppDate = toLocalDateTime(zone).date

fun AppDate.atTimeIn(
    time: AppTime,
    zone: AppTimeZone,
): AppInstant = LocalDateTime(this, time).toInstant(zone)

fun AppDate.plusDays(days: Int): AppDate = plus(days, DateTimeUnit.DAY)

fun AppDate.minusDays(days: Int): AppDate = minus(days, DateTimeUnit.DAY)
