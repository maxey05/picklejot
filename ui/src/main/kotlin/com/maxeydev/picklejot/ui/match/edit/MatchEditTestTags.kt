package com.maxeydev.picklejot.ui.match.edit

object MatchEditTestTags {
    const val SAVE = "match_edit_save"
    const val RESULT_WIN = "match_edit_result_win"
    const val RESULT_LOSS = "match_edit_result_loss"
    const val RESULT_ERROR = "match_edit_result_error"
    const val FORMAT_SINGLES = "match_edit_format_singles"
    const val FORMAT_DOUBLES = "match_edit_format_doubles"
    const val ADD_GAME = "match_edit_add_game"
    const val LOCATION = "match_edit_location"
    const val NOTES = "match_edit_notes"
    const val ADD_PHOTOS = "match_edit_add_photos"
    const val TAKE_PHOTO = "match_edit_take_photo"
    const val PHOTO_ERROR = "match_edit_photo_error"
    const val PHOTO_ERROR_DISMISS = "match_edit_photo_error_dismiss"

    fun photo(key: String): String = "match_edit_photo_$key"

    fun photoMoveEarlier(key: String): String = "match_edit_photo_earlier_$key"

    fun photoMoveLater(key: String): String = "match_edit_photo_later_$key"

    fun photoRemove(key: String): String = "match_edit_photo_remove_$key"

    fun personSlot(slot: PersonSlot): String = "match_edit_person_${slot.name.lowercase()}"
}
