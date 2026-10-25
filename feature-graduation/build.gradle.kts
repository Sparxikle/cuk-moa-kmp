import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Shared settings for feature modules: Compose libraries for Android, iOS and desktop. (Every module keeps the same shape)
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "kr.ac.catholic.cukmoa.feature.graduation"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }

    }
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
        }
    }
}
