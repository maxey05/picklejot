package com.maxeydev.picklejot.domain.streak

import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.datetime.AppTimeZone
import com.maxeydev.picklejot.domain.datetime.toAppDateIn

@JvmInline
value class WeekKey(
    val ordinal: Long,
) : Comparable<WeekKey> {
    val monday: AppDate
        get() = AppDate.fromEpochDays(ordinal * DAYS_PER_WEEK - EPOCH_DAY_TO_WEEK_OFFSET)

    val sunday: AppDate
        get() = AppDate.fromEpochDays(ordinal * DAYS_PER_WEEK - EPOCH_DAY_TO_WEEK_OFFSET + DAYS_PER_WEEK - 1)

    val isoYear: Int
        get() = thursday().year

    val isoWeek: Int
        get() = (thursday().dayOfYear - 1) / DAYS_PER_WEEK.toInt() + 1

    fun previous(): WeekKey = WeekKey(ordinal - 1)

    fun next(): WeekKey = WeekKey(ordinal + 1)

    override fun compareTo(other: WeekKey): Int = ordinal.compareTo(other.ordinal)

    override fun toString(): String = "$isoYear-W${isoWeek.toString().padStart(2, '0')}"

    private fun thursday(): AppDate =
        AppDate.fromEpochDays(ordinal * DAYS_PER_WEEK - EPOCH_DAY_TO_WEEK_OFFSET + THURSDAY_OFFSET_FROM_MONDAY)

    companion object {
        private const val DAYS_PER_WEEK = 7L
        private const val EPOCH_DAY_TO_WEEK_OFFSET = 3L
        private const val THURSDAY_OFFSET_FROM_MONDAY = 3L

        fun of(date: AppDate): WeekKey =
            WeekKey((date.toEpochDays() + EPOCH_DAY_TO_WEEK_OFFSET).floorDiv(DAYS_PER_WEEK))

        fun containing(
            instant: AppInstant,
            zone: AppTimeZone,
        ): WeekKey = of(localDateOf(instant, zone))

        fun localDateOf(
            instant: AppInstant,
            zone: AppTimeZone,
        ): AppDate = instant.toAppDateIn(zone)
    }
}
