plugins {
    // AGP 9 has Kotlin built in — the kotlin-android plugin isn't applied separately.
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "kr.ac.catholic.cukmoa.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "kr.ac.catholic.cukmoa"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        // Default: on the emulator, 10.0.2.2 = the dev PC.
        // Real device (USB): adb reverse tcp:8080 tcp:8080, then -PapiBaseUrl=http://localhost:8080
        // Release: -PapiBaseUrl=https://<server address>
        val apiBaseUrl = providers.gradleProperty("apiBaseUrl").getOrElse("http://10.0.2.2:8080")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }
    buildTypes {
        // Release build: R8 shrinks and optimizes the code (2–3x faster startup).
        // Without a store signing key, sign with the debug key (for device testing). Change signingConfig when publishing to the store.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
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
}
