# Testing Setup
## Existing test source sets
- No `@app/src/test/` directory exists.
- No `@app/src/androidTest/` directory exists.
- A repository-wide file search found no `*Test*` or `*Spec*` source files under the project sources.

## Configured test-related build settings
- `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` is declared in @app/build.gradle.kts.
- `androidTestImplementation(composeBom)` is declared in @app/build.gradle.kts.
- `debugImplementation("androidx.compose.ui:ui-test-manifest")` is declared in @app/build.gradle.kts.

## Missing explicit test dependencies
The build file does **not** currently declare explicit dependencies for:
- JUnit / JUnit Jupiter / `testImplementation`
- `androidx.compose.ui:ui-test-junit4`
- `androidx.test.ext:junit`
- Espresso
- Mockito / MockK
- Robolectric
- Turbine / Truth / coroutine-test

## What this means
- Gradle tasks for unit and instrumentation tests exist (`app:testDebugUnitTest`, `app:connectedDebugAndroidTest`, etc.), confirmed by `./gradlew tasks --all`.
- However, the repository has no actual test sources today.
- Instrumentation is only partially prepared: there is a runner and Compose BOM, but no Compose test runner artifact or AndroidX test assertion stack in @app/build.gradle.kts.

## Current testable seams
- Parsers in `@app/src/main/java/com/padelaragon/app/data/parser/` are the cleanest unit-test targets.
- Room DAOs/entities in `@app/src/main/java/com/padelaragon/app/data/local/` are natural Android/instrumented or Robolectric targets.
- ViewModel logic in `@app/src/main/java/com/padelaragon/app/ui/viewmodel/` contains meaningful state logic, but is tightly coupled to singletons.
- Compose screens in `@app/src/main/java/com/padelaragon/app/ui/screen/` have almost no explicit `Modifier.testTag` hooks; only a few `contentDescription` values exist (for example in @app/src/main/java/com/padelaragon/app/ui/components/MatchCard.kt and @app/src/main/java/com/padelaragon/app/ui/screen/GroupDetailScreen.kt).
