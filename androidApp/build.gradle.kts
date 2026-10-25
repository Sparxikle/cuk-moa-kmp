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
        // On the emulator, 10.0.2.2 = the dev PC. Release builds switch to the HTTPS server address.
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")
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
}
