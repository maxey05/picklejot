package com.maxeydev.picklejot.ui.settings

import com.maxeydev.picklejot.domain.datetime.AppInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatBackupDate(
    instant: AppInstant,
    zone: ZoneId,
    locale: Locale,
): String =
    java.time.Instant
        .ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(zone)
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
