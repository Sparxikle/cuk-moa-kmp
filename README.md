# 가대모아 (CUK Moa) — App

Catholic University of Korea, Songsim Campus · 2026 academic fair, team 404 · **Scattered campus information, gathered in one place for your semester.**
A modular app that shows school, LMS and department board announcements, official major roadmaps, graduation requirements, professors and an AI timetable maker, tailored to your year, majors and career interests. Android (phones, tablets), iOS/iPadOS and desktop from one Kotlin Multiplatform codebase.

Server: [cuk-moa-ktor](https://github.com/Sparxikle/cuk-moa-ktor) · live at `https://54-117-83-53.sslip.io` (API docs: [`/swagger`](https://54-117-83-53.sslip.io/swagger)) · conventions: [CONVENTIONS.md](CONVENTIONS.md)

Announcements, the academic calendar, departments, professors, major roadmaps and this semester's open classes are **real data collected by the server** from school websites and the Public Data Portal (data.go.kr). Graduation requirements are real for 45 departments (students admitted in 2026). Only **senior records are sample data** until the senior survey, and the app labels them "(sample)".

## Structure

```
shared/                 Shared by app and server: API models, credit and recommendation logic, API client, sample catalog (data/)
core/                   Design tokens, common components, app state, module interface (FeatureModule)
feature-announcements/  Announcements module
feature-roadmap/        Roadmap module
feature-graduation/     Graduation requirements module
feature-professors/     Professors module
feature-timetable/      Timetable module (AI timetable maker, manual editing)
composeApp/             App shell: onboarding, dashboard, module manager, tabs (Android · iOS · desktop)
androidApp/             Android launcher module
iosApp/                 iOS app shell: XcodeGen spec (project.yml) + SwiftUI entry point
```

Feature modules don't know about each other and depend only on `core`. Users turn modules on and off during onboarding and on the "Add & manage modules" screen; only enabled modules appear as tabs and home cards.

### Adding a feature module

1. Create `feature-xxx/` like the existing modules (copy `build.gradle.kts` and change only the namespace).
2. Implement `FeatureModule`: `id`, `title`, `summary`, `icon`, `Screen()`, and optionally `homeCards` / `HomeCard()`.
3. Add `include(":feature-xxx")` to `settings.gradle.kts`, the dependency to `composeApp/build.gradle.kts`, and one line to `ModuleRegistry.all`.

## Running

Requires JDK 21+ and the Android SDK (compileSdk 37). `./gradlew` downloads Gradle.

```sh
./gradlew :composeApp:run              # Desktop app (server address: CUK_MOA_API, default http://localhost:8080)
./gradlew :androidApp:assembleDebug    # Android APK (server address: http://localhost:8080; run adb reverse tcp:8080 tcp:8080 first)
./gradlew :composeApp:screenshots      # Renders the main screens headlessly to composeApp/build/screenshots/ (real announcements if the server is running)
```

The app works without the server. If the server can't be reached, it draws the screens from the last catalog it received (or the bundled sample) and shows a "Reconnect" button.

Release APK: `./gradlew :androidApp:assembleRelease -PapiBaseUrl=https://54-117-83-53.sslip.io`, signed with the key from `local.properties` (`release.storeFile`, `release.storePassword`, `release.keyAlias`, `release.keyPassword`; not in git), or the debug key when those are missing. Release builds are HTTPS-only.

iOS (macOS with Xcode 26): `cd iosApp && xcodegen generate`, open `iosApp.xcodeproj`, pick your team, run. Per-machine settings (signing team, server address) go in the untracked `iosApp/local.yml` (see the comment in `project.yml`). The Kotlin side of the iOS build can be checked on Linux too: `./gradlew :composeApp:compileKotlinIosArm64`.

## Tests

```sh
./gradlew :shared:jvmTest
```

27 tests: credit calculation (demo: 3rd-year backend track = 44 credits left), next semester recommendations from official roadmaps, graduation exam calculation, referential integrity of the catalog, reading/rejecting the saved-state format, and backup export/import.

## Relationship to the server repo

The server ([cuk-moa-ktor](https://github.com/Sparxikle/cuk-moa-ktor)) uses this repo's `shared` module as `kr.ac.catholic.cukmoa:shared`. When both repos are cloned side by side, the server build links this project directly, so **changing an API model also recompiles the server** and any mismatch shows up right away.

```
Code/
  cuk-moa-kmp/      ← this repo
  cuk-moa-ktor/
```
