package com.maxeydev.picklejot.ui.match

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant

fun AppDate.toUtcEpochMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun utcEpochMillisToAppDate(epochMillis: Long): AppDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.UTC).date

fun formatMatchDate(
    date: AppDate,
    locale: Locale,
): String = date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

private const val SHORT_DATE_PATTERN = "MMM d"
private const val SHORT_DATE_WITH_YEAR_PATTERN = "MMM d, yyyy"

fun todayInDeviceZone(): AppDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun formatMatchDateShort(
    date: AppDate,
    today: AppDate,
    locale: Locale,
): String {
    val pattern = if (date.year == today.year) SHORT_DATE_PATTERN else SHORT_DATE_WITH_YEAR_PATTERN
    return date.toJavaLocalDate().format(DateTimeFormatter.ofPattern(pattern, locale))
}

fun formatMatchDateLong(
    date: AppDate,
    locale: Locale,
): String = date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))

fun formatMatchTime(
    time: AppTime,
    locale: Locale,
): String = time.toJavaLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
