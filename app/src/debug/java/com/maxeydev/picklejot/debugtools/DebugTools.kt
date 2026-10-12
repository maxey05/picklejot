@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklejot.debugtools

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.maxeydev.picklejot.AppContainer
import com.maxeydev.picklejot.domain.datetime.AppDate
import com.maxeydev.picklejot.domain.datetime.AppTime
import com.maxeydev.picklejot.domain.datetime.atTimeIn
import com.maxeydev.picklejot.domain.datetime.minusDays
import com.maxeydev.picklejot.domain.datetime.toAppDateIn
import com.maxeydev.picklejot.domain.entitlement.EntitlementSignal
import com.maxeydev.picklejot.domain.match.FilterState
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.streak.InsuredStreakEngine
import com.maxeydev.picklejot.domain.streak.streakInsuranceStart
import com.maxeydev.picklejot.ui.notification.StreakReminderNotifier
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val STATUS = "status"
private const val SET_PRO = "set_pro"
private const val SEED_MATCHES = "seed_matches"
private const val SEED_STREAK = "seed_streak"
private const val LOG_MATCH = "log_match"
private const val CLEAR_MATCHES = "clear_matches"
private const val NOTIFY = "notify"
private const val FINISH_ONBOARDING = "finish_onboarding"
private const val ARG_ON = "on"
private const val ARG_SKIP = "skip"
private const val EXTRA_COUNT = "count"
private const val EXTRA_DAYS_AGO = "days_ago"
private const val EXTRA_WEEKS = "weeks"
private const val EXTRA_END_OFFSET = "end_offset"
private const val EXTRA_GAPS = "gaps"
private const val DEFAULT_SEED_COUNT = 40
private const val DEFAULT_OLD_DAYS = 400
private const val DEFAULT_WEEKS = 3
private const val DEFAULT_END_OFFSET = 1
private const val DAYS_PER_WEEK = 7
private const val DEBUG_PRO_TOKEN = "debug-tools-pro-token"
private const val DEBUG_OPPONENT = "Debug Dave"
private const val DEBUG_LOCATION = "Debug Courts"
private const val DEBUG_NAME = "Tester"
private const val LOSS_EVERY = 3
private const val NOON_HOUR = 12
private val KNOWN_METHODS =
    listOf(STATUS, SET_PRO, SEED_MATCHES, SEED_STREAK, LOG_MATCH, CLEAR_MATCHES, NOTIFY, FINISH_ONBOARDING)

class DebugTools(
    private val container: AppContainer,
    private val context: Context,
) {
    suspend fun run(
        method: String,
        arg: String?,
        extras: Bundle,
    ): Bundle =
        when (method) {
            STATUS -> status()
            SET_PRO -> setPro(enabled = arg == ARG_ON, daysAgo = extras.getInt(EXTRA_DAYS_AGO, 0))
            SEED_MATCHES ->
                seedMatches(
                    count = extras.getInt(EXTRA_COUNT, DEFAULT_SEED_COUNT),
                    daysAgo = extras.getInt(EXTRA_DAYS_AGO, DEFAULT_OLD_DAYS),
                )
            SEED_STREAK ->
                seedStreak(
                    weeks = extras.getInt(EXTRA_WEEKS, DEFAULT_WEEKS),
                    endOffset = extras.getInt(EXTRA_END_OFFSET, DEFAULT_END_OFFSET),
                    gaps = parseGaps(extras.getString(EXTRA_GAPS)),
                )
            LOG_MATCH -> logMatch(daysAgo = extras.getInt(EXTRA_DAYS_AGO, 0))
            CLEAR_MATCHES -> clearMatches()
            NOTIFY -> notifyAtRisk(skipAvailable = arg == ARG_SKIP)
            FINISH_ONBOARDING -> finishOnboarding()
            else -> bundleOf("error" to "Unknown method '$method'. Known: ${KNOWN_METHODS.joinToString()}")
        }

    private suspend fun status(): Bundle {
        val entitlement = container.entitlementRepository.observeEntitlement().first()
        val dates =
            container.matchRepository
                .observeStatLines(FilterState.NONE)
                .first()
                .map { it.date }
        val engine = InsuredStreakEngine(container.clock) { container.currentTimeZone() }
        val insured = engine.compute(dates, entitlement.streakInsuranceStart())
        return bundleOf(
            "today" to today().toString(),
            "isPro" to entitlement.isPro,
            "proSince" to entitlement.proSince?.toString(),
            "matchCount" to dates.size,
            "streakCurrent" to insured.streak.current,
            "streakLongest" to insured.streak.longest,
            "skippedWeeks" to insured.skippedWeeks.joinToString(),
            "skipsHeld" to insured.skipsHeld,
        )
    }

    private suspend fun setPro(
        enabled: Boolean,
        daysAgo: Int,
    ): Bundle {
        val now = container.clock.now()
        val signal =
            if (enabled) {
                EntitlementSignal.PurchaseVerified(
                    purchaseToken = DEBUG_PRO_TOKEN,
                    verifiedAt = now,
                    purchasedAt = now - daysAgo.days,
                )
            } else {
                EntitlementSignal.RefundConfirmed(DEBUG_PRO_TOKEN)
            }
        container.entitlementRepository.record(signal)
        return status()
    }

    private suspend fun seedMatches(
        count: Int,
        daysAgo: Int,
    ): Bundle {
        val date = today().minusDays(daysAgo)
        repeat(count) { index -> saveMatch(date, index) }
        return status()
    }

    private suspend fun seedStreak(
        weeks: Int,
        endOffset: Int,
        gaps: Set<Int>,
    ): Bundle {
        val today = today()
        (endOffset until endOffset + weeks)
            .filterNot { it in gaps }
            .forEachIndexed { index, weekOffset -> saveMatch(today.minusDays(weekOffset * DAYS_PER_WEEK), index) }
        return status()
    }

    private suspend fun logMatch(daysAgo: Int): Bundle {
        saveMatch(today().minusDays(daysAgo), container.matchRepository.observeMatchCount().first())
        return status()
    }

    private suspend fun clearMatches(): Bundle {
        val ids =
            container.matchRepository
                .observeListPage(MatchSort.DATE_NEWEST, Int.MAX_VALUE)
                .first()
                .map { it.id }
        ids.forEach { container.matchRepository.deleteMatch(it) }
        return status()
    }

    private suspend fun finishOnboarding(): Bundle {
        container.onboarding.complete(DEBUG_NAME)
        return status()
    }

    private fun notifyAtRisk(skipAvailable: Boolean): Bundle {
        val posted = StreakReminderNotifier(context).notifyStreakAtRisk(skipAvailable)
        return bundleOf("notificationPosted" to posted)
    }

    private suspend fun saveMatch(
        date: AppDate,
        index: Int,
    ) {
        val opponent = container.personRepository.findOrCreatePerson(DEBUG_OPPONENT)
        val loggedAt = date.atTimeIn(AppTime(NOON_HOUR, 0), container.currentTimeZone()) + index.milliseconds
        val result = if (index % LOSS_EVERY == 0) MatchResult.LOSS else MatchResult.WIN
        container.matchRepository.saveMatch(
            Match(
                id = Uuid.random(),
                format = MatchFormat.SINGLES,
                date = date,
                result = result,
                createdAt = loggedAt,
                updatedAt = loggedAt,
                location = DEBUG_LOCATION,
                opponents = listOf(opponent),
                games = listOf(GameScore(1, 11, 7), GameScore(2, 11, 9)),
            ),
        )
    }

    private fun today(): AppDate = container.clock.now().toAppDateIn(container.currentTimeZone())

    private fun parseGaps(raw: String?): Set<Int> =
        raw
            .orEmpty()
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
}
