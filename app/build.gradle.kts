import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
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

// A release without a working verifier builds fine and then can never sell Pro (the app reports UNREACHABLE, the
// purchase is acknowledged and the buyer is charged with no Pro), so refuse to build it. CI has no verifier and never
// ships, so it opts out with -PallowUnverifiedRelease=true, which only downgrades the failure to a warning.
// A dedicated task always runs: a doFirst on generateReleaseBuildConfig is skipped when that task is UP-TO-DATE or
// FROM-CACHE. The same strings it validates are the ones written to BuildConfig. Formats are what the app and
// server/ accept: an https origin (lowercase host, optional port) and a standard-base64 X.509 ECDSA P-256 key.
val licenseServerUrl = providers.gradleProperty("licenseServerUrl").getOrElse("")
val licenseServerPublicKey = providers.gradleProperty("licenseServerPublicKey").getOrElse("")
val allowUnverifiedRelease = providers.gradleProperty("allowUnverifiedRelease").map { it == "true" }.getOrElse(false)
val verifyReleaseLicenseServer =
    tasks.register("verifyReleaseLicenseServer") {
        description = "Fails a release build whose licenseServerUrl / licenseServerPublicKey are blank or malformed."
        inputs.property("allowUnverifiedRelease", allowUnverifiedRelease)
        outputs.upToDateWhen { false }
        doLast {
            val label = "[a-z0-9]([a-z0-9-]*[a-z0-9])?"
            val urlOk =
                Regex("""https://$label(\.$label)+(:\d{1,5})?/?""").matches(licenseServerUrl) &&
                    !Regex("""(^|[/.])(example|invalid|localhost)([.:/]|$)|\.\d+(:\d+)?/?$""").containsMatchIn(licenseServerUrl)
            val keyOk =
                licenseServerPublicKey.startsWith("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE") &&
                    Regex("[A-Za-z0-9+/]{122}==").matches(licenseServerPublicKey) &&
                    runCatching {
                        val spec = X509EncodedKeySpec(Base64.getDecoder().decode(licenseServerPublicKey))
                        (KeyFactory.getInstance("EC").generatePublic(spec) as ECPublicKey).params.curve.field.fieldSize == 256
                    }.getOrDefault(false)
            val problems =
                listOfNotNull(
                    "licenseServerUrl must be a lowercase https origin such as https://<your-worker>.workers.dev (no whitespace, quotes, path, query, userinfo, IP or example/localhost host)"
                        .takeUnless { urlOk },
                    "licenseServerPublicKey must be the standard-base64 X.509 ECDSA P-256 public key from `npm run gen-key` (not the private key, no whitespace)"
                        .takeUnless { keyOk },
                )
            if (problems.isEmpty()) return@doLast
            if (allowUnverifiedRelease) {
                logger.warn("picklejot: unverified release allowed. ${problems.joinToString(" ")} This build grants NO new Pro purchases.")
            } else {
                throw GradleException(
                    "Release build refused. ${problems.joinToString(" ")} (see server/README.md). " +
                        "Pass -PallowUnverifiedRelease=true only for builds that will never be published.",
                )
            }
        }
    }
// Hook the release and (release-derived) benchmark builds. named() throws if AGP ever renames a target.
afterEvaluate {
    listOf("generateReleaseBuildConfig", "generateBenchmarkBuildConfig").forEach { tasks.named(it) { dependsOn(verifyReleaseLicenseServer) } }
}

// Property values are interpolated into generated Java source, so escape them.
fun String.javaLiteral() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\n").replace("\r", "\r") + "\""

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
        // ~/.gradle/gradle.properties. Release builds refuse blank/malformed values (verifyReleaseLicenseServer).
        buildConfigField("String", "LICENSE_SERVER_URL", licenseServerUrl.javaLiteral())
        buildConfigField("String", "LICENSE_SERVER_PUBLIC_KEY", licenseServerPublicKey.javaLiteral())
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
    implementation(libs.androidx.core.splashscreen)
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
