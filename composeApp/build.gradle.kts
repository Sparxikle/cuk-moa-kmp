import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "kr.ac.catholic.cukmoa.app"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core) // App(platform: Platform) exposes core types
            implementation(projects.featureAnnouncements)
            implementation(projects.featureRoadmap)
            implementation(projects.featureGraduation)
            implementation(projects.featureProfessors)
            implementation(projects.featureTimetable)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.filekit.dialogs.compose)
        }
        androidMain.dependencies { implementation(libs.androidx.core.ktx) }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

// Desktop server address: -PapiBaseUrl=https://<server address>, baked into run and the installers (default: local server)
val apiBaseUrl: String = providers.gradleProperty("apiBaseUrl").getOrElse("http://localhost:8080")

compose.desktop {
    application {
        mainClass = "kr.ac.catholic.cukmoa.app.MainKt"
        jvmArgs += "-Dcukmoa.api=$apiBaseUrl"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // codesign (macOS) and WiX (Windows MSI, codepage 1252) fail on a Korean name; the window still shows 가대모아
            val os = System.getProperty("os.name")
            packageName = if (os.startsWith("Mac") || os.startsWith("Windows")) "CukMoa" else "가대모아"
            packageVersion = "1.0.0"
            // The installers bundle a trimmed Java runtime. suggestRuntimeModules lists the last five; Ktor's
            // Java HTTP engine needs java.net.http and HTTPS needs jdk.crypto.ec, which it doesn't detect.
            modules("java.net.http", "jdk.crypto.ec", "java.instrument", "java.management", "java.scripting", "jdk.security.auth", "jdk.unsupported")
            // App icon per installer (made by icons.sh from icons/)
            macOS {
                iconFile.set(project.file("icons/AppIcon.icns"))
                dockName = "가대모아"
            }
            windows {
                iconFile.set(project.file("icons/AppIcon.ico"))
                menu = true
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "ea7fc41d-ab08-4f5e-9ff7-f18be881166c"
            }
            linux { iconFile.set(project.file("icons/AppIcon.png")) }
        }
    }
}

// Renders the main screens headlessly to build/screenshots/*.png.
tasks.register<JavaExec>("screenshots") {
    group = "documentation"
    val desktop = kotlin.targets.getByName("desktop").compilations.getByName("main")
    dependsOn(desktop.compileTaskProvider)
    classpath = files(desktop.output.allOutputs, desktop.runtimeDependencyFiles)
    mainClass = "kr.ac.catholic.cukmoa.app.ScreenshotsKt"
    args(layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
}
