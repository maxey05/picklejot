package com.maxeydev.picklejot.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklejot.ui.R
import java.time.DayOfWeek
import java.time.LocalDateTime
import kotlin.random.Random

internal enum class Greeting(
    @get:StringRes val label: Int,
) {
    GOOD_MORNING(R.string.greeting_good_morning),
    GOOD_AFTERNOON(R.string.greeting_good_afternoon),
    GOOD_EVENING(R.string.greeting_good_evening),
    HAPPY_MONDAY(R.string.greeting_happy_monday),
    HAPPY_TUESDAY(R.string.greeting_happy_tuesday),
    HAPPY_WEDNESDAY(R.string.greeting_happy_wednesday),
    HAPPY_THURSDAY(R.string.greeting_happy_thursday),
    HAPPY_FRIDAY(R.string.greeting_happy_friday),
    HAPPY_SATURDAY(R.string.greeting_happy_saturday),
    HAPPY_SUNDAY(R.string.greeting_happy_sunday),
    WHATS_UP(R.string.greeting_whats_up),
    HEY_THERE(R.string.greeting_hey_there),
    WELCOME_BACK(R.string.greeting_welcome_back),
    READY_TO_PLAY(R.string.greeting_ready_to_play),
}

private const val BUCKET_COUNT = 100
private const val TIME_OF_DAY_BUCKETS = 40
private const val DAY_OF_WEEK_BUCKETS = 35
private const val MORNING_START_HOUR = 5
private const val AFTERNOON_START_HOUR = 12
private const val EVENING_START_HOUR = 18

private val CASUAL_GREETINGS =
    listOf(Greeting.WHATS_UP, Greeting.HEY_THERE, Greeting.WELCOME_BACK, Greeting.READY_TO_PLAY)

internal fun pickGreeting(
    hour: Int,
    dayOfWeek: DayOfWeek,
    bucket: Int,
    variant: Int,
): Greeting =
    when {
        bucket < TIME_OF_DAY_BUCKETS -> timeOfDayGreeting(hour)
        bucket < TIME_OF_DAY_BUCKETS + DAY_OF_WEEK_BUCKETS -> dayOfWeekGreeting(dayOfWeek)
        else -> CASUAL_GREETINGS[variant.mod(CASUAL_GREETINGS.size)]
    }

private fun timeOfDayGreeting(hour: Int): Greeting =
    when (hour) {
        in MORNING_START_HOUR until AFTERNOON_START_HOUR -> Greeting.GOOD_MORNING
        in AFTERNOON_START_HOUR until EVENING_START_HOUR -> Greeting.GOOD_AFTERNOON
        else -> Greeting.GOOD_EVENING
    }

private fun dayOfWeekGreeting(dayOfWeek: DayOfWeek): Greeting =
    when (dayOfWeek) {
        DayOfWeek.MONDAY -> Greeting.HAPPY_MONDAY
        DayOfWeek.TUESDAY -> Greeting.HAPPY_TUESDAY
        DayOfWeek.WEDNESDAY -> Greeting.HAPPY_WEDNESDAY
        DayOfWeek.THURSDAY -> Greeting.HAPPY_THURSDAY
        DayOfWeek.FRIDAY -> Greeting.HAPPY_FRIDAY
        DayOfWeek.SATURDAY -> Greeting.HAPPY_SATURDAY
        DayOfWeek.SUNDAY -> Greeting.HAPPY_SUNDAY
    }

@Composable
internal fun rememberGreeting(): Greeting {
    val ordinal =
        rememberSaveable {
            val now = LocalDateTime.now()
            pickGreeting(
                hour = now.hour,
                dayOfWeek = now.dayOfWeek,
                bucket = Random.nextInt(BUCKET_COUNT),
                variant = Random.nextInt(CASUAL_GREETINGS.size),
            ).ordinal
        }
    return Greeting.entries[ordinal]
}

@Composable
internal fun greetingText(
    greeting: Greeting,
    hasName: Boolean,
): String {
    val label = stringResource(greeting.label)
    return if (hasName) stringResource(R.string.dashboard_greeting_named, label) else label
}
