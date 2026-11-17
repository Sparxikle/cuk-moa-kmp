import java.util.Properties

plugins {
    // AGP 9 has Kotlin built in — the kotlin-android plugin isn't applied separately.
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Release signing key, from local.properties (not in git) or the same names as Gradle properties / -P flags:
//   release.storeFile=/path/to/cukmoa-release.jks
//   release.storePassword=...
//   release.keyAlias=cukmoa
//   release.keyPassword=...   (PKCS12 keystores use the store password; may be left out)
// Without them, release builds fall back to the debug key (device testing only).
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun releaseSetting(name: String): String? = providers.gradleProperty(name).orNull ?: localProperties.getProperty(name)
val releaseStoreFile = releaseSetting("release.storeFile")?.let { file(it) }?.takeIf { it.exists() }
// Default: localhost. Forward the phone's port to the dev PC with: adb reverse tcp:8080 tcp:8080
// Release: -PapiBaseUrl=https://<server address>
val apiBaseUrl: String = providers.gradleProperty("apiBaseUrl").getOrElse("http://localhost:8080")

android {
    namespace = "kr.ac.catholic.cukmoa.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "kr.ac.catholic.cukmoa"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.9"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }
    signingConfigs {
        if (releaseStoreFile != null) create("release") {
            storeFile = releaseStoreFile
            storePassword = releaseSetting("release.storePassword")
            keyAlias = releaseSetting("release.keyAlias")
            keyPassword = releaseSetting("release.keyPassword") ?: releaseSetting("release.storePassword")
        }
    }
    buildTypes {
        // Plain HTTP: debug builds always (dev servers); release builds only for an http:// API address (e.g. a demo
        // server on the local network). Web pages in the in-app browser load over HTTPS either way.
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        // Release build: R8 shrinks and optimizes the code (2–3x faster startup).
        // Signed with the release key when it is set up (see the top of this file), otherwise with the debug key.
        release {
            manifestPlaceholders["usesCleartextTraffic"] = apiBaseUrl.startsWith("http://").toString()
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    // Apply the startup (baseline) profiles bundled in the Compose libraries at install time → precompiled code from the very first launch
    implementation(libs.androidx.profileinstaller)
    // Home-screen widgets (Glance) and their hourly background refresh (WorkManager)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime)
    // The widget feed picker uses the app's own components (core); widgets take their colors from the app's schemes
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
}
