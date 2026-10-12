package com.maxeydev.picklejot.domain.backup

import com.maxeydev.picklejot.domain.datetime.AppInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

object ExportPromptPolicy {
    const val MATCHES_PER_PROMPT = 25

    val PERIODIC_INTERVAL: Duration = 30.days

    fun evaluate(
        state: ExportPromptState,
        matchCount: Int,
        now: AppInstant,
    ): ExportPromptState {
        val since = state.trackingSince ?: return state.copy(trackingSince = now, anchorCount = matchCount)
        val anchor = minOf(state.anchorCount, matchCount)
        val crossed = matchCount - anchor >= MATCHES_PER_PROMPT
        val settled = state.copy(anchorCount = if (crossed) matchCount else anchor)
        if (state.pending != null) {
            return settled
        }
        if (crossed) {
            return settled.copy(lastPromptAt = now, pending = ExportPromptReason.MATCHES_ADDED)
        }
        val reference = listOfNotNull(since, state.lastExportAt, state.lastPromptAt).max()
        if (matchCount > 0 && now - reference >= PERIODIC_INTERVAL) {
            return settled.copy(lastPromptAt = now, pending = ExportPromptReason.PERIODIC)
        }
        return settled
    }

    fun afterExport(
        state: ExportPromptState,
        matchCount: Int,
        now: AppInstant,
    ): ExportPromptState = state.copy(lastExportAt = now, anchorCount = matchCount, pending = null)

    fun afterDismissal(state: ExportPromptState): ExportPromptState = state.copy(pending = null)

    fun afterImport(
        state: ExportPromptState,
        matchCount: Int,
    ): ExportPromptState = state.copy(anchorCount = matchCount)
}
