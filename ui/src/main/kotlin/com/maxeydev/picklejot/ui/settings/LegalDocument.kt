package com.maxeydev.picklejot.ui.settings

import androidx.annotation.StringRes
import com.maxeydev.picklejot.ui.R

enum class LegalDocument(
    @StringRes val titleRes: Int,
    val assetPath: String,
    val onlineUrl: String,
    val relativeLink: String,
    val screenTag: String,
) {
    PRIVACY(
        titleRes = R.string.privacy_title,
        assetPath = "privacy-policy.md",
        onlineUrl = LegalLinks.PRIVACY_URL,
        relativeLink = "privacy-policy.html",
        screenTag = LegalTestTags.PRIVACY_SCREEN,
    ),
    TERMS(
        titleRes = R.string.terms_title,
        assetPath = "terms-of-service.md",
        onlineUrl = LegalLinks.TERMS_URL,
        relativeLink = "terms-of-service.html",
        screenTag = LegalTestTags.TERMS_SCREEN,
    ),
    ;

    companion object {
        fun fromLink(link: String): LegalDocument? = entries.firstOrNull { document -> document.relativeLink == link }
    }
}
