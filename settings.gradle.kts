rootProject.name = "cuk-moa"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":shared")       // Models and calculations shared by app and server (the server repo uses this module)
include(":core")         // Design tokens, common components, app state, module interface
include(":feature-announcements")
include(":feature-roadmap")
include(":feature-graduation")
include(":feature-professors")
include(":composeApp")   // Compose Multiplatform UI (Android, iOS, desktop)
include(":androidApp")   // Android entry point (AGP 9 keeps the application module separate)
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
