import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library)
    `maven-publish`
}

// The server repo (cuk-moa-ktor) uses shared through these coordinates.
// Cloned side by side, a Gradle composite build links straight to this project; otherwise it comes from a Maven repository.
group = "kr.ac.catholic.cukmoa"
version = "0.1.0"

kotlin {
    android {
        namespace = "kr.ac.catholic.cukmoa.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }
    iosArm64()
    iosSimulatorArm64()
    // Used by both the desktop app and the Ktor server
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        jvmMain.dependencies { implementation(libs.ktor.client.java) }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }
}
