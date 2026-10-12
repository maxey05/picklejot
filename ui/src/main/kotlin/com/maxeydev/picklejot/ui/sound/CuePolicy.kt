package com.maxeydev.picklejot.ui.sound

private const val DUCKED_VOLUME = 0.6f

sealed interface CueDecision {
    data object Drop : CueDecision

    data class Play(
        val volume: Float,
        val stopsActiveCelebration: Boolean,
    ) : CueDecision
}

class CuePolicy {
    private val lastPlayedAt = mutableMapOf<Cue, Long>()
    private var activeCelebration: Cue? = null
    private var celebrationEndsAt = 0L

    fun decide(
        cue: Cue,
        nowMillis: Long,
        touchExploration: Boolean,
    ): CueDecision {
        if (cue.kind == CueKind.TAP && touchExploration) {
            return CueDecision.Drop
        }
        val last = lastPlayedAt[cue]
        if (last != null && nowMillis - last < cue.minGapMillis) {
            return CueDecision.Drop
        }
        val playing = activeCelebration?.takeIf { nowMillis < celebrationEndsAt }
        val blocked =
            when (cue.kind) {
                CueKind.TAP -> false
                CueKind.CONFIRM -> playing != null
                CueKind.CELEBRATION -> playing != null && playing.rank >= cue.rank
            }
        if (blocked) {
            return CueDecision.Drop
        }
        lastPlayedAt[cue] = nowMillis
        if (cue.kind == CueKind.CELEBRATION) {
            activeCelebration = cue
            celebrationEndsAt = nowMillis + cue.durationMillis
        }
        val volume = if (cue.kind == CueKind.CELEBRATION && touchExploration) DUCKED_VOLUME else 1f
        return CueDecision.Play(volume = volume, stopsActiveCelebration = playing != null)
    }
}
