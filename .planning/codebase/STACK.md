# Codebase Stack
| Layer | Technology | Version | Config File |
|---|---|---|---|
| Build system | Gradle | 9.4.1 wrapper | @gradle/wrapper/gradle-wrapper.properties |
| Android plugin | Android Gradle Plugin | 9.1.0 | @build.gradle.kts |
| Language | Kotlin | 2.3.20 compose plugin; JVM toolchain 21 | @build.gradle.kts, @app/build.gradle.kts |
| UI | Jetpack Compose Material 3 | Compose BOM 2026.03.00 | @app/build.gradle.kts |
| Navigation | Navigation Compose | 2.9.7 | @app/build.gradle.kts |
| State / lifecycle | Lifecycle ViewModel Compose | 2.10.0 | @app/build.gradle.kts |
| Async | Kotlin Coroutines Android | 1.10.2 | @app/build.gradle.kts |
| Networking | OkHttp | 5.3.2 | @app/build.gradle.kts |
| HTML parsing | Jsoup | 1.22.1 | @app/build.gradle.kts |
| Persistence | Room | 2.8.4 | @app/build.gradle.kts, @app/src/main/java/com/padelaragon/app/data/local/AppDatabase.kt |
| Google Play security provider | Play Services Base / ProviderInstaller | 18.10.0 | @app/build.gradle.kts, @app/src/main/java/com/padelaragon/app/PadelAragonApp.kt |

## Modules
- Only one Gradle module is included: `:app` in @settings.gradle.kts.
- There are no dedicated `:data`, `:domain`, `:core`, or test-support modules.

## App metadata
- Application id / namespace: `com.padelaragon.app` in @app/build.gradle.kts.
- SDKs: min 26, target 36, compile 36 in @app/build.gradle.kts.
- Current semantic version source of truth: `versionMajor = 1`, `versionMinor = 2`, `versionPatch = 0` in @app/build.gradle.kts.
