package com.maxeydev.picklejot.ui.navigation

import kotlinx.serialization.Serializable

const val MATCH_ID_ARGUMENT = "matchId"

const val LOG_ANOTHER_FROM_ARGUMENT = "logAnotherFrom"

const val JUST_SAVED_MATCH_ID_KEY = "just_saved_match_id"

@Serializable
data object HomeRoute

@Serializable
data class MatchEditRoute(
    val matchId: String? = null,
    val logAnotherFrom: String? = null,
)

@Serializable
data class MatchDetailRoute(
    val matchId: String,
)

@Serializable
data object StatsRoute

@Serializable
data object PaywallRoute

@Serializable
data object PrivacyRoute

@Serializable
data object TermsRoute

@Serializable
data object BackupRoute

@Serializable
data class ShareRoute(
    val matchId: String,
)

@Serializable
data object OnboardingRoute

@Serializable
data object AboutRoute

@Serializable
data object SupportRoute
