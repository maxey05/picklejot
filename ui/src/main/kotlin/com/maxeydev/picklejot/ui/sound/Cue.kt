package com.maxeydev.picklejot.ui.sound

import androidx.annotation.RawRes
import com.maxeydev.picklejot.ui.R

enum class CueKind {
    TAP,
    CONFIRM,
    CELEBRATION,
}

private const val DEFAULT_MIN_GAP_MILLIS = 80L
private const val DUCK_MIN_GAP_MILLIS = 120L

enum class Cue(
    @RawRes val resId: Int,
    val kind: CueKind,
    val durationMillis: Long,
    val rank: Int = 0,
    val minGapMillis: Long = DEFAULT_MIN_GAP_MILLIS,
) {
    POCK(R.raw.sfx_pock, CueKind.TAP, 100),
    ONBOARDING_STEP_1(R.raw.sfx_onboarding_step_1, CueKind.TAP, 477),
    ONBOARDING_STEP_2(R.raw.sfx_onboarding_step_2, CueKind.TAP, 476),
    ONBOARDING_STEP_3(R.raw.sfx_onboarding_step_3, CueKind.TAP, 476),
    ONBOARDING_STEP_4(R.raw.sfx_onboarding_step_4, CueKind.TAP, 478),
    NAME_REQUIRED(R.raw.sfx_name_required, CueKind.CONFIRM, 380),
    WELCOME(R.raw.sfx_welcome, CueKind.CELEBRATION, 1359, rank = 1),
    NEW_MATCH(R.raw.sfx_new_match, CueKind.TAP, 226),
    LOG_ANOTHER(R.raw.sfx_log_another, CueKind.TAP, 83),
    TALLY(R.raw.sfx_tally, CueKind.TAP, 1016),
    STREAK_UP(R.raw.sfx_streak_up, CueKind.CELEBRATION, 1291, rank = 2),
    FIRST_MATCH(R.raw.sfx_first_match, CueKind.CELEBRATION, 1638, rank = 3),
    STREAK_MILESTONE(R.raw.sfx_streak_milestone, CueKind.CELEBRATION, 1989, rank = 4),
    STREAK_MILESTONE_MAJOR(R.raw.sfx_streak_milestone_major, CueKind.CELEBRATION, 1991, rank = 4),
    PRO_RESTORED(R.raw.sfx_pro_restored, CueKind.CELEBRATION, 1779, rank = 5),
    PRO_UNLOCKED(R.raw.sfx_pro_unlocked, CueKind.CELEBRATION, 2306, rank = 6),
    SHUFFLE(R.raw.sfx_shuffle, CueKind.TAP, 122),
    TICK_SELECT(R.raw.sfx_tick_select, CueKind.TAP, 46),
    TICK_CLEAR(R.raw.sfx_tick_clear, CueKind.TAP, 48),
    SWEEP(R.raw.sfx_sweep, CueKind.TAP, 200),
    DRAWER_OPEN(R.raw.sfx_drawer_open, CueKind.TAP, 150),
    DRAWER_CLOSE(R.raw.sfx_drawer_close, CueKind.TAP, 150),
    RESULT_WIN(R.raw.sfx_result_win, CueKind.TAP, 454),
    RESULT_LOSS(R.raw.sfx_result_loss, CueKind.TAP, 326),
    FORMAT_SINGLES(R.raw.sfx_format_singles, CueKind.TAP, 79),
    FORMAT_DOUBLES(R.raw.sfx_format_doubles, CueKind.TAP, 141),
    SOFT_ERROR(R.raw.sfx_soft_error, CueKind.CONFIRM, 140),
    MATCH_SAVED(R.raw.sfx_match_saved, CueKind.CONFIRM, 919),
    MATCH_UPDATED(R.raw.sfx_match_updated, CueKind.TAP, 91),
    POP_IN(R.raw.sfx_pop_in, CueKind.TAP, 90),
    POP_OUT(R.raw.sfx_pop_out, CueKind.TAP, 90),
    SNAP(R.raw.sfx_snap, CueKind.TAP, 70),
    PHOTO_ADDED(R.raw.sfx_photo_added, CueKind.TAP, 80),
    WHISK(R.raw.sfx_whisk, CueKind.TAP, 120),
    DELETE(R.raw.sfx_delete, CueKind.CONFIRM, 260),
    SHEET_UP(R.raw.sfx_sheet_up, CueKind.TAP, 200),
    CARD_READY(R.raw.sfx_card_ready, CueKind.CONFIRM, 855),
    CARD_SWAP(R.raw.sfx_card_swap, CueKind.TAP, 90),
    SHARE_SEND(R.raw.sfx_share_send, CueKind.CONFIRM, 259),
    TOGGLE_ON(R.raw.sfx_toggle_on, CueKind.TAP, 49),
    TOGGLE_OFF(R.raw.sfx_toggle_off, CueKind.TAP, 51),
    REMINDER_ON(R.raw.sfx_reminder_on, CueKind.CONFIRM, 720),
    ERASE_DONE(R.raw.sfx_erase_done, CueKind.CONFIRM, 420),
    EXPORT_READY(R.raw.sfx_export_ready, CueKind.CONFIRM, 245),
    IMPORT_DONE(R.raw.sfx_import_done, CueKind.CONFIRM, 940),
    DUCK_PET_1(R.raw.sfx_duck_pet_1, CueKind.TAP, 100, minGapMillis = DUCK_MIN_GAP_MILLIS),
    DUCK_PET_2(R.raw.sfx_duck_pet_2, CueKind.TAP, 180, minGapMillis = DUCK_MIN_GAP_MILLIS),
    DUCK_PET_3(R.raw.sfx_duck_pet_3, CueKind.TAP, 170, minGapMillis = DUCK_MIN_GAP_MILLIS),
    DUCK_PET_4(R.raw.sfx_duck_pet_4, CueKind.TAP, 290, minGapMillis = DUCK_MIN_GAP_MILLIS),
    ;

    companion object {
        val ONBOARDING_STEPS: List<Cue> =
            listOf(ONBOARDING_STEP_1, ONBOARDING_STEP_2, ONBOARDING_STEP_3, ONBOARDING_STEP_4)

        val DUCK_PETS: List<Cue> = listOf(DUCK_PET_1, DUCK_PET_2, DUCK_PET_3, DUCK_PET_4)
    }
}
