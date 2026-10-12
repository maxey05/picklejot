package com.maxeydev.picklejot.ui.settings

data class OpenSourceLibrary(
    val name: String,
    val license: String,
)

private const val APACHE_2 = "Apache License 2.0"
private const val SIL_OFL_1_1 = "SIL Open Font License 1.1"

val OPEN_SOURCE_LIBRARIES: List<OpenSourceLibrary> =
    listOf(
        OpenSourceLibrary("Kotlin", APACHE_2),
        OpenSourceLibrary("kotlinx.coroutines", APACHE_2),
        OpenSourceLibrary("kotlinx.serialization", APACHE_2),
        OpenSourceLibrary("kotlinx-datetime", APACHE_2),
        OpenSourceLibrary("Jetpack Compose and Material 3", APACHE_2),
        OpenSourceLibrary("AndroidX Core, Activity, Lifecycle and Navigation", APACHE_2),
        OpenSourceLibrary("AndroidX Room", APACHE_2),
        OpenSourceLibrary("AndroidX DataStore", APACHE_2),
        OpenSourceLibrary("AndroidX WorkManager", APACHE_2),
        OpenSourceLibrary("Coil", APACHE_2),
        OpenSourceLibrary("Baloo 2 font", SIL_OFL_1_1),
        OpenSourceLibrary("Google Play Billing Library", "Android Software Development Kit License"),
    )
