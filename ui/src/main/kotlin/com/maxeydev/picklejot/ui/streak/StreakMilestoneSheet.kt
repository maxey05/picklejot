package com.maxeydev.picklejot.ui.streak

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.Mascot
import com.maxeydev.picklejot.ui.common.MascotSheet
import com.maxeydev.picklejot.ui.sound.Cue

private const val NOT_LOADED = -1
private const val NONE_PENDING = 0
private const val MAJOR_MILESTONE_WEEKS = 26

private val MILESTONE_BODIES: Map<Int, Int> =
    mapOf(
        4 to R.string.streak_milestone_body_4,
        8 to R.string.streak_milestone_body_8,
        12 to R.string.streak_milestone_body_12,
        26 to R.string.streak_milestone_body_26,
        52 to R.string.streak_milestone_body_52,
    )

@StringRes
internal fun streakMilestoneBody(weeks: Int): Int? = MILESTONE_BODIES[weeks]

internal fun isStreakMilestoneReached(
    previousWeeks: Int,
    currentWeeks: Int,
): Boolean = currentWeeks > previousWeeks && currentWeeks in MILESTONE_BODIES

@Composable
fun StreakMilestoneHost(
    streakWeeks: Int,
    isLoaded: Boolean,
    isForeground: Boolean,
    modifier: Modifier = Modifier,
) {
    var previousWeeks by rememberSaveable { mutableIntStateOf(NOT_LOADED) }
    var pendingWeeks by rememberSaveable { mutableIntStateOf(NONE_PENDING) }
    LaunchedEffect(streakWeeks, isLoaded) {
        if (isLoaded) {
            if (previousWeeks != NOT_LOADED && isStreakMilestoneReached(previousWeeks, streakWeeks)) {
                pendingWeeks = streakWeeks
            }
            previousWeeks = streakWeeks
        }
    }
    val body = streakMilestoneBody(pendingWeeks)
    if (body != null && isForeground) {
        MascotSheet(
            mascot = Mascot.SMASH,
            title = pluralStringResource(R.plurals.dashboard_streak_weeks, pendingWeeks, pendingWeeks),
            body = stringResource(body),
            titleIcon = R.drawable.ic_flame,
            onDismiss = { pendingWeeks = NONE_PENDING },
            sheetTag = StreakNoticeTestTags.MILESTONE_SHEET,
            entranceCue =
                if (pendingWeeks >= MAJOR_MILESTONE_WEEKS) Cue.STREAK_MILESTONE_MAJOR else Cue.STREAK_MILESTONE,
            modifier = modifier,
        )
    }
}
