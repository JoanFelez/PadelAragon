---
phase: 1
plan: 1
type: implement
wave: 1
depends_on: []
files_modified: [app/build.gradle.kts, app/src/main/java/com/padelaragon/app/data/repository/LeagueRepository.kt, app/src/test/java/com/padelaragon/app/, app/src/test/resources/]
autonomous: false
must_haves:
  observable_truths:
    - "The app compiles before test authoring begins."
    - "A local JVM test stack exists and can run in this workspace without a connected device."
    - "Test fixtures/resources exist so parser tests do not call the live website."
    - "The dirty working tree has an explicit scope/staging boundary so unrelated modifications are not assumed to belong to this phase."
  artifacts:
    - path: app/build.gradle.kts
      has: [explicit test dependencies, local test configuration]
    - path: app/src/main/java/com/padelaragon/app/data/repository/LeagueRepository.kt
      has: [compile-safe cachedMatchDetails membership check]
    - path: app/src/test/
      has: [test source layout, at least one concrete local test source or reusable helper, shared fixtures]
    - path: app/release/
      has: [explicit in-scope or out-of-scope decision before staging]
  key_links:
    - from: "Gradle test tasks"
      to: "new local test dependencies"
      verify: ":app:testDebugUnitTest resolves and executes the new suite locally"
    - from: "LeagueRepository prefetch logic"
      to: "Kotlin compilation"
      verify: ":app:compileDebugKotlin succeeds from the working tree used for the suite"
    - from: "Pre-existing dirty files"
      to: "final staging boundary for this phase"
      verify: "git status/diff review identifies which existing modifications remain excluded from this phase before staging"
---

# Phase 1, Plan 1: Build Stabilization and Test Foundation

## Objective
Unblock the repo for testing by ensuring the app compiles, establishing the explicit local test toolchain in `:app`, and creating the deterministic test layout/fixtures that later plans will fill in.

## Context
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/codebase/ARCHITECTURE.md
@.planning/codebase/STACK.md
@.planning/codebase/TESTING.md
@.planning/codebase/CONCERNS.md
@app/build.gradle.kts
@app/src/main/java/com/padelaragon/app/data/repository/LeagueRepository.kt

## Task Notes
- If you encounter an authentication/authorization error during execution (OAuth, API key, SSO, etc.), stop immediately and return a checkpoint requesting the user to authenticate.
- Because the repo is already dirty, do not assume every existing modification belongs to this task. Review before staging anything.

## Tasks

### Task 1: Preflight the dirty working tree
- **type:** checkpoint:decision
- **files:** app/src/main/java/com/padelaragon/app/data/repository/LeagueRepository.kt, app/src/main/java/com/padelaragon/app/ui/viewmodel/GroupListViewModel.kt, app/src/main/java/com/padelaragon/app/ui/viewmodel/TeamViewModel.kt, app/src/main/java/com/padelaragon/app/ui/screen/TeamScreen.kt, app/src/main/java/com/padelaragon/app/data/network/HtmlFetcher.kt, app/release/
- **action:** Confirm which pre-existing modifications are part of this task, especially any existing local fix for the `cachedMatchDetails` compile blocker and any unrelated release artifacts, and record the edit/staging boundary the downstream agent must preserve through the rest of the plan.
- **verify:** `git --no-pager status --short && git --no-pager diff -- app/src/main/java/com/padelaragon/app/data/repository/LeagueRepository.kt`
- **done:** There is a clear boundary for what the downstream agent may edit and later stage for the final commit, including which dirty files stay out of scope unless the human expands that scope.

### Task 2: Stabilize compilation and wire the local test stack
- **type:** auto
- **files:** app/src/main/java/com/padelaragon/app/data/repository/LeagueRepository.kt, app/build.gradle.kts
- **action:** Ensure the repository compiles cleanly from the current working baseline and expand `:app` with the explicit JVM test dependencies/configuration needed for parser tests, coroutine/state tests, and Android-backed local tests (Room/SharedPreferences) that can run without a device.
- **verify:** `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
- **done:** Compile-blocking ambiguity is resolved or preserved correctly, and `:app:testDebugUnitTest` is operational as the must-pass local suite entrypoint.

### Task 3: Create deterministic test layout and shared fixtures
- **type:** auto
- **files:** app/src/test/java/com/padelaragon/app/, app/src/test/resources/fixtures/
- **action:** Add the local test package structure, at least one concrete local test source or reusable helper under `app/src/test/java`, and representative saved HTML fixtures under `app/src/test/resources/fixtures/` so later tests never hit the live federation site.
- **verify:** `find app/src/test/java -type f | sort && find app/src/test/resources -type f | sort && ./gradlew :app:testDebugUnitTest`
- **done:** The repo has stable local test directories, at least one concrete local test source or reusable helper under `app/src/test/java`, fixture files under `app/src/test/resources/fixtures/`, and the post-Task-3 tree still passes through `:app:testDebugUnitTest`.

## Dependency Graph
```yaml
dependency_graph:
  task_1:
    needs: []
    creates: [task-scope-decision]
  task_2:
    needs: [task-scope-decision]
    creates: [app/build.gradle.kts, app/src/main/java/com/padelaragon/app/data/repository/LeagueRepository.kt]
  task_3:
    needs: [app/build.gradle.kts]
    creates: [app/src/test/java/com/padelaragon/app/, app/src/test/resources/fixtures/]
```

## Verification
- Run the compile gate first.
- Run the local unit-test task after wiring dependencies and rerun it after Task 3 so the final tree proves the new test source/helper and fixtures are wired into the Gradle entrypoint.
- Confirm the created fixture paths match the package structure planned for Phase 2.
- Confirm the scoped dirty-file boundary from Task 1 is still respected before any later staging/commit work.

## Success Criteria
1. `:app:compileDebugKotlin` is green on the same tree that will receive tests.
2. `:app:testDebugUnitTest` is the agreed must-pass suite entrypoint for this task.
3. Test fixtures/resources exist locally and do not require network access, and at least one concrete local test source or reusable helper exists under `app/src/test/java`.
4. The planned work preserves an explicit boundary between in-scope changes and unrelated pre-existing working tree modifications.
