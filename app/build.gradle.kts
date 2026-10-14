import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }
val uploadKeystore =
    keystoreProperties.getProperty("storeFile")
        ?.let { rootProject.file(it) }
        ?.takeIf { it.exists() }

// A release without the verifier configured builds fine and then can never sell Pro, so refuse to build it.
// CI has no verifier and never ships, so it opts out with -PallowUnverifiedRelease=true.
val verifierConfigured =
    listOf("licenseServerUrl", "licenseServerPublicKey").none { findProperty(it)?.toString().isNullOrBlank() }
val allowUnverifiedRelease = findProperty("allowUnverifiedRelease") == "true"
if (!verifierConfigured) {
    logger.warn("picklejot: licenseServerUrl/licenseServerPublicKey blank - this build grants NO new Pro purchases.")
}
tasks.matching { it.name == "generateReleaseBuildConfig" }.configureEach {
    doFirst {
        check(verifierConfigured || allowUnverifiedRelease) {
            "Release build refused: set licenseServerUrl and licenseServerPublicKey (see server/README.md). " +
                "Pass -PallowUnverifiedRelease=true only for builds that will never be published."
        }
    }
}

android {
    namespace = "com.maxeydev.picklejot"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.maxeydev.picklejot"
        minSdk = 26
        targetSdk = 37
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()

        // Purchase verifier (server/). Both are public, not secrets. Set them in gradle.properties or
        // ~/.gradle/gradle.properties. Left blank the app grants no new Pro purchases (fail closed).
        buildConfigField("String", "LICENSE_SERVER_URL", "\"${findProperty("licenseServerUrl") ?: ""}\"")
        buildConfigField("String", "LICENSE_SERVER_PUBLIC_KEY", "\"${findProperty("licenseServerPublicKey") ?: ""}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (uploadKeystore != null) {
            create("release") {
                storeFile = uploadKeystore
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                if (uploadKeystore != null) {
                    signingConfigs.getByName("release")
                } else {
                    logger.warn(
                        "picklejot: keystore/keystore.properties absent — release build will be " +
                            "debug-signed and is NOT uploadable to Play. See keystore/README.md.",
                    )
                    signingConfigs.getByName("debug")
                }
        }
        create("benchmark") {
            initWith(getByName("release"))
            applicationIdSuffix = ".benchmark"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":ui"))

    implementation(libs.billing.ktx)
    implementation(libs.work.runtime.ktx)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
