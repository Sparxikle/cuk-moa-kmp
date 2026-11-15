# Conventions — app (cuk-moa-kmp)

Team 404's rules for this repo. The server repo ([cuk-moa-ktor](https://github.com/Sparxikle/cuk-moa-ktor)) follows the same commit and code style with its own server rules.

## Git

- **One branch, `main`.** Each finished feature is one commit (or a few), built and tested before it lands. No work-in-progress commits.
- **Commit messages:** an English subject in the imperative or as a short result, capitalised, no trailing period, about 70 characters at most. A blank line, then a body that says *why* and anything a reader can't see in the diff.
  ```
  Server check runs when Settings > Data opens

  The four status endpoints are called on opening the page; Check runs it again.
  ```
- **Never committed:** `local.properties` (SDK path, release signing key and passwords), `iosApp/local.yml` (signing team), keystores, build output. See `.gitignore`.
- A change to the API models in `shared/` is committed together with the matching server change, so both repos always build against each other.

## Modules

```
shared → core → feature-* → composeApp → androidApp / iosApp
```

- **`shared`**: API models, pure logic (credits, recommendations, backup) and the API client. No UI. Compiled by the server too, so its models *are* the API contract.
- **`core`**: design tokens (`theme/`), shared components (`ui/`), app state (`state/AppViewModel`), the `FeatureModule` interface and platform code (web view, share sheet).
- **`feature-*`**: one tab each (announcements, roadmap, graduation, professors, timetable). They depend only on `core` and never on each other; links between features go through `LocalNavigator`.
- **`composeApp`**: the app shell (onboarding, home, settings, tabs) and `ModuleRegistry`, the one list of feature modules.
- New feature = new module + one line in `ModuleRegistry.all` (see the README).

## Kotlin and Compose style

- Kotlin official code style (`kotlin.code.style=official`), 4-space indent, trailing commas in multi-line argument lists.
- **Names:** `PascalCase` composables and types, `camelCase` functions and values, screens end in `Screen` (`GraduationScreen`), rows end in `Row` (`ListRow`, `ProfessorRow`), lazy-list builders are `LazyListScope` extensions in lower camel case (`cardItems`, `departmentPicker`).
- **Comments** are English KDoc on every public or non-obvious declaration. They explain intent and the reason for a choice, not what the next line does.
- **Screens** use the shared building blocks instead of raw Material 3: `ScreenList` (header, pull-to-refresh, lazy list), `Section` / `Group` / `cardItems` (LG UX cards), `ListRow`, `Toggle`, `Choices`, `DetailSheet`. Sizes come from `Space`, `Radius` and `CardMetrics`; colours from `AppTheme.colors`.
- **Wide screens** (840dp or wider, `LocalWideLayout`) get the side tab rail, two-pane `ScreenList` (`startPane` / `endPane`) and side sheets. Phone layouts must stay unchanged when adding a wide layout.
- **State** that outlives a screen lives in `AppViewModel` as `StateFlow`; screens read it with `collectAsStateWithLifecycle()`. Student data is stored only on the device, versioned (`CURRENT_SCHEMA_VERSION`) with migrations.
- **Platform code** goes behind `expect`/`actual` in the module that needs it (`core/src/{androidMain,iosMain,desktopMain}`), with common code first.

## Text and resources

- Every user-visible string is a resource in `core/src/commonMain/composeResources/values*/strings.xml`, in **all four languages** (`values` English, `values-ko`, `values-zh`, `values-ja`), added in the same commit.
- Placeholders are `%1$s` / `%1$d`. Write `%` and `'` as they are; Compose resources don't unescape `%%` or `\'`.
- Korean names of school things (학과, 전공, 졸업요건 …) stay Korean in the Korean strings and in data.
- Bundled assets need a license that allows it; licenses ship next to the asset (e.g. `files/lottie/LICENSE.txt`) and appear in Settings → About.

## Data and network

- All data comes from the server and the device cache; no hard-coded sample data in screens. Sample data must say "(sample)" on screen.
- Release builds are HTTPS-only. Web links open in the in-app browser (`LocalUriHandler`); don't call platform browsers directly.
- Every server endpoint the app relies on has a call in `ApiClient` (shared), and the server's OpenAPI spec lists it.

## Checks before a commit

```sh
./gradlew :shared:jvmTest                     # logic and format tests
./gradlew :androidApp:assembleDebug :composeApp:compileKotlinDesktop
./gradlew :composeApp:compileKotlinIosArm64   # iOS Kotlin side (on Linux too)
```

UI changes are checked on a phone and a tablet (portrait and landscape) before they're committed.
