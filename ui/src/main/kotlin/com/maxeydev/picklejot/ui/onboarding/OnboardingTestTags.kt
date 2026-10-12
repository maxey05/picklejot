package com.maxeydev.picklejot.ui.onboarding

object OnboardingTestTags {
    const val SCREEN = "onboarding_screen"
    const val PAGER = "onboarding_pager"
    const val PAGE_INDICATOR = "onboarding_page_indicator"
    const val NEXT = "onboarding_next"
    const val SKIP = "onboarding_skip"
    const val NAME_FIELD = "onboarding_name_field"
    const val CONTINUE = "onboarding_continue"
    const val BACKUP_NOTE = "onboarding_backup_note"
    const val SAVE_ERROR = "onboarding_save_error"

    fun title(page: IntroPage): String = "onboarding_title_${page.name.lowercase()}"
}
