package com.maxeydev.picklejot.ui.common

object AutocompleteTestTags {
    fun suggestionList(fieldTag: String): String = "${fieldTag}_suggestions"

    fun suggestion(
        fieldTag: String,
        index: Int,
    ): String = "${fieldTag}_suggestion_$index"
}
