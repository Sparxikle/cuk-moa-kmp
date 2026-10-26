# 가대모아 (CUK Moa) — App

Catholic University of Korea, Songsim Campus · College of AI, Electronics & Information Convergence academic fair · **Scattered campus information, gathered in one place for your semester.**
A modular app that shows school, LMS and department board announcements, seniors' course paths, graduation requirements and professor information, tailored to your year, major and career track.

Server: [cuk-moa-ktor](https://github.com/Sparxikle/cuk-moa-ktor)

Announcements, the academic calendar, departments, professors and major roadmaps are **collected live** by the server from school websites and the Public Data Portal (data.go.kr). Courses, graduation requirements and senior records are **sample data for the demo** (based on the requirements for students admitted in 2018).

## Structure

```
shared/                 Shared by app and server: API models, credit and recommendation logic, API client, sample catalog (data/)
core/                   Design tokens, common components, app state, module interface (FeatureModule)
feature-announcements/  Announcements module
feature-roadmap/        Roadmap module
feature-graduation/     Graduation requirements module
feature-professors/     Professors module
composeApp/             App shell: onboarding, dashboard, module manager, tabs (Android · iOS · desktop)
androidApp/             Android launcher module
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
./gradlew :androidApp:assembleDebug    # Android APK (on the emulator, 10.0.2.2:8080 = your dev PC)
./gradlew :composeApp:screenshots      # Renders the main screens headlessly to composeApp/build/screenshots/ (real announcements if the server is running)
```

The app works without the server. If the server can't be reached, it draws the screens from the last catalog it received (or the bundled sample) and shows a "Reconnect" button.

iOS: create an iOS app project in macOS + Xcode and show `MainViewController()` (composeApp/src/iosMain) as the root view. The Kotlin side of the iOS build can be checked on Linux too: `./gradlew :composeApp:compileKotlinIosSimulatorArm64`.

## Tests

```sh
./gradlew :shared:jvmTest
```

Covers credit calculation (demo: 3rd-year backend track = 44 credits left), next semester recommendations, graduation exam calculation, referential integrity of the sample catalog, and reading/rejecting the saved-state format.

## Relationship to the server repo

The server ([cuk-moa-ktor](https://github.com/Sparxikle/cuk-moa-ktor)) uses this repo's `shared` module as `kr.ac.catholic.cukmoa:shared`. When both repos are cloned side by side, the server build links this project directly, so **changing an API model also recompiles the server** and any mismatch shows up right away.

```
Code/
  cuk-moa-kmp/      ← this repo
  cuk-moa-ktor/
```
