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
        namespace = "kr.ac.catholic.cukmoa.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
        androidResources { enable = true }
    }
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared)
            api(libs.compose.components.resources)
            implementation(libs.compose.ui.backhandler)
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

// The single source of the sample catalog and department list is shared/data. Copied into app resources and bundled as the offline fallback.
val assembleComposeResources = tasks.register<Sync>("assembleComposeResources") {
    from("src/commonMain/composeResources")
    from(rootProject.file("shared/data")) { include("sample-catalog.json", "departments.json"); into("files") }
    into(layout.buildDirectory.dir("assembledComposeResources"))
}

compose.resources {
    packageOfResClass = "kr.ac.catholic.cukmoa.core.resources"
    publicResClass = true
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = assembleComposeResources.map { layout.buildDirectory.dir("assembledComposeResources").get() },
    )
}
