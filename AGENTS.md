# AGENTS.md: EarnIt Android Project

This file contains repo-specific guidance for OpenCode agents working on the EarnIt project.

## Build & Test Commands

| Command | Description |
|---|---|
| `./gradlew lintDebug` | Run Android Lint (step 1 of the verification order) |
| `./gradlew test` | Run unit tests (local JVM tests) |
| `./gradlew assembleDebug` | Build the debug APK |
| `./gradlew assembleRelease` | Build the release APK (requires sign config) |
| `./gradlew connectedAndroidTest` | Run instrumented tests on a device/emulator (needs a connected device) |
| `./gradlew tasks` | List all available Gradle tasks |

**Order matters**: Run `lint -> test -> assembleDebug` in that order (running tests alone may miss lint errors).

## Build Stack (pinned — do not "fix" casually)

| Piece | Version | Notes |
|---|---|---|
| Gradle wrapper | 9.6.0 | `gradle/wrapper/gradle-wrapper.properties` |
| AGP | 9.4.1 | Requires Gradle >= 9.x |
| Kotlin | 2.2.10 | **Built into AGP 9** — there is *no* `org.jetbrains.kotlin.android` plugin |
| KSP | 2.2.10-2.0.2 | Needs `android.disallowKotlinSourceSets=false` (already set) |
| Room | 2.7.2 | Older Room (2.6.x) crashes with KSP2 (`unexpected jvm signature V`) |
| Compose BOM | 2026.02.01 | Pulls foundation/material3 1.10.x — see quirks below |

**Hard rules for this repo** (each one caused a build break in the past):

- `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to **JDK 25 (Adoptium 25.0.3)** and is downloaded by Gradle itself. Never delete it, and never add `org.gradle.java.home` to `gradle.properties` (it is silently ignored and desyncs the daemon).
- Do **not** add `org.jetbrains.kotlin.android` / `kotlin("android")` — AGP 9 owns Kotlin. Keep `com.android.application`, `org.jetbrains.kotlin.plugin.compose` and `com.google.devtools.ksp` only.
- All versions live in `gradle/libs.versions.toml` and are referenced in `app/build.gradle.kts` through `libs.*` aliases. Do not hardcode dependency versions in the build script.
- Kotlin compiles to **JVM 11** (`compileOptions` + `kotlin { compilerOptions }` block). Changing one without the other breaks JVM-target consistency.
- Harmless config-time warning you will see: `Kotlin does not yet support 25 JDK target, falling back to Kotlin JVM_24`. Compiled classes are still bytecode major 55 (Java 11) — ignore it.

## Project Structure

This is a Clean-architecture-style Android app (Kotlin + Jetpack Compose).

- `domain/` — pure Kotlin: models (`Habit`, `HabitCompletion`, `Frequency`, `HabitStatus`), repository interfaces, use cases (e.g., `CreateHabitUseCase`, `ToggleCompletionUseCase`).
- `data/` — Room database, DAOs, entities, repository implementations (`HabitRepositoryImpl`).
- `feature/` — UI screens and ViewModels (`HomeViewModel`, `HabitDetailViewModel`).
- `core/di/AppContainer` — manual dependency injection container; creates `AppDatabase`, provides `DateProvider`, and instantiates repositories & use cases.
- `core/ui/components/` — reusable Compose UI helpers (e.g., `IconHelper` maps icon names to `Icons.Filled` constants).

**Entry points**: `MainActivity.kt` → `AppNavHost` → feature screens (`HomeScreen`, `HabitDetailScreen`).

## Key Conventions & Quirks

- **Database migration**: `AppDatabase.getInstance()` uses `fallbackToDestructiveMigration()` — schema changes will wipe user data. Bump `version` and write a proper migration if needed.
- **DI**: No Hilt. `AppContainer(context)` wires everything manually, and `core/di/ViewModelFactories.kt` holds the three manual `ViewModelProvider.Factory` classes (`HomeViewModelFactory`, `HabitFormViewModelFactory`, `HabitDetailViewModelFactory`). Keep factories and container in sync — `MainActivity` wires them via `by viewModels { ... }`.
- **Room entities vs. domain models**: `HabitEntity` (persisted) ↔ `Habit` (domain) are converted via `toEntity()` / `toDomain()` in `HabitRepositoryImpl`. Fields differ: `createdAt` is unix ms in entity, `LocalDate` in domain; `frequency` and `status` are enums in both but mapped separately.
- **Icon mapping**: `core/ui/components/IconHelper.getIcon()` uses a `when` chain for icon names. If adding a new icon, add a case here *and* register the icon import.
- **Material icons are extension properties**: you must `import androidx.compose.material.icons.filled.X` and write `Icons.Filled.X`. Fully-qualified access (`androidx.compose.material.icons.Icons.Filled.X`) **does not compile**. `material-icons-extended` is on the classpath for the long-tail icons.
- **Foundation 1.10 package moves**: `rememberScrollState` / `verticalScroll` live in `androidx.compose.foundation` (there is no `androidx.compose.foundation.scroll` anymore). `LazyHorizontalGrid` takes `rows = GridCells.Fixed(n)` (not `cells`); `LazyVerticalGrid` takes `columns`.
- **No `Locale.getDefault()` inside `@Composable`** — Lint fails the build with `NonObservableLocale`. Read `LocalConfiguration.current.locales[0]` instead.
- **Concurrency**: repository methods that *return a `Flow`* are **not** `suspend` (only `insert`/`update`/`getById`/`upsert` are). Use cases are `operator suspend fun invoke(...)`; call them from `viewModelScope.launch { }` in ViewModels or from a `rememberCoroutineScope().launch { }` in a screen. Long-running mutations funnel through the callbacks that `AppNavHost` passes down, so the screens stay side-effect free.
- **Dependency Order**: `lint -> test -> assembleDebug`. Running tests alone may miss lint errors.
- **Version control**: `local.properties` is auto-generated and git-ignoded — do not commit changes to SDK paths.

## Testing Quirks

- Unit tests: `./gradlew test` runs local JVM tests (e.g., `ExampleUnitTest.kt`).
- Instrumented tests: `./gradlew connectedAndroidTest` runs on device/emulator.
- Mockito is used for mocking; Robolectric for Android context simulation; Turbine for Flow testing.
- Turbine must stay on `1.0.0` — the originally pinned `1.0.1` does not exist on Maven Central and fails resolution only for `test*` configurations (main builds still pass).
- Instrumented tests require a connected device/emulator; there is usually none, so `connectedAndroidTest` is normally skipped.
- Test fixtures/data: keep an eye on `data/local/entity/` vs `domain/model/` mapping — tests that cross this boundary need `toEntity()` / `toDomain()` logic.

## Adding a New Use Case / Feature

1. Define model in `domain/model/` (data class + enums).
2. Add repository method to `domain/repository/` interface + impl in `data/repository/`**.
3. Create use case in `domain/usecase/` as a class with a single `operator suspend fun invoke(...)` (return a `Flow` from a non-suspend method if you just observe data).
4. Wire in `AppContainer`.
5. Add UI in `feature/`, a factory in `core/di/ViewModelFactories.kt` if it is a new ViewModel, and navigation in `AppNavHost`.
6. Run `./gradlew lintDebug test assembleDebug` to verify.

****: Repository impls convert between Entity ↔ Model via private `toEntity()` / `toDomain()`.
