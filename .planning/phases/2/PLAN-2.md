---
phase: 2
plan: 2
type: implement
wave: 3
depends_on: [".planning/phases/1/PLAN.md", ".planning/phases/2/PLAN-1.md"]
files_modified: [app/src/test/java/com/padelaragon/app/data/local/, app/src/test/java/com/padelaragon/app/data/favorites/, app/src/test/java/com/padelaragon/app/ui/viewmodel/, app/src/main/java/com/padelaragon/app/ui/viewmodel/]
autonomous: true
must_haves:
  observable_truths:
    - "Local persistence/state seams are covered without a device."
    - "Core non-network ViewModel logic is verifiable by local tests."
    - "The must-pass suite remains deterministic and offline."
    - "Once this plan lands on top of the companion Phase 2 parser work, `./gradlew :app:testDebugUnitTest` passes for the combined offline suite."
  artifacts:
    - path: app/src/test/java/com/padelaragon/app/data/local/
      has: [Room DAO/entity tests for LeagueGroup, StandingRow, MatchResult, MatchDetail, Jornada, and TeamDetail cache seams]
    - path: app/src/test/java/com/padelaragon/app/data/favorites/
      has: [FavoritesManager tests for init, toggle, persistence reload, and max-favorites behavior]
    - path: app/src/test/java/com/padelaragon/app/ui/viewmodel/
      has: [logic tests for sorting, default jornada selection, player stats]
  key_links:
    - from: "Room entities and DAOs"
      to: "in-memory/local test database"
      verify: "round-trip reads match inserted models"
    - from: "FavoritesManager toggle/init behavior"
      to: "SharedPreferences-backed persistence and reload seam"
      verify: "tests mutate favorites, re-initialize against the same stored preferences, and observe the reloaded state matches the persisted set and cap rules"
    - from: "ViewModel-owned calculations"
      to: "local tests"
      verify: "tests assert stable outputs without using the live singleton repository"
    - from: "Phase 2 parser coverage plus this plan's persistence/ViewModel coverage"
      to: "Gradle offline unit-test entrypoint"
      verify: "`./gradlew :app:testDebugUnitTest` passes after both Phase 2 plans are present, without network access or an emulator"
---

# Phase 2, Plan 2: Persistence and ViewModel Logic Coverage

## Objective
Build on Phase 2 Plan 1 by covering the Android-backed local seams that are still realistic in this repo today, then make the most important ViewModel calculations locally testable without depending on live network/repository behavior, and finish with the full offline Phase 2 suite gate.

## Context
@.planning/ROADMAP.md
@.planning/phases/2/PLAN-1.md
@.planning/codebase/ARCHITECTURE.md
@.planning/codebase/TESTING.md
@.planning/codebase/CONCERNS.md
@app/src/main/java/com/padelaragon/app/data/local/AppDatabase.kt
@app/src/main/java/com/padelaragon/app/data/favorites/FavoritesManager.kt
@app/src/main/java/com/padelaragon/app/ui/viewmodel/GroupListViewModel.kt
@app/src/main/java/com/padelaragon/app/ui/viewmodel/GroupDetailViewModel.kt
@app/src/main/java/com/padelaragon/app/ui/viewmodel/TeamViewModel.kt

## Task Notes
- If you encounter an authentication/authorization error during execution (OAuth, API key, SSO, etc.), stop immediately and return a checkpoint requesting the user to authenticate.
- Prefer keeping the must-pass suite local; do not make a device or live federation site part of the gate for this task.
- This plan is scheduled after `PLAN-1` so the final verification can prove the combined Phase 2 suite, not just the new tests added here.

## Tasks

### Task 1: Add Room/entity regression coverage
- **type:** auto
- **files:** app/src/test/java/com/padelaragon/app/data/local/, app/src/main/java/com/padelaragon/app/data/local/
- **action:** Add local tests that prove the Room cache seams for `LeagueGroupDao`, `StandingRowDao`, `MatchResultDao`, `MatchDetailDao`, `JornadaDao`, and `TeamDetailDao` round-trip correctly for `LeagueGroupEntity`, `StandingRowEntity`, `MatchResultEntity`, `MatchDetailPairEntity`, `JornadaEntity`, `TeamDetailEntity`, and `PlayerEntity`. Treat parser/network behavior, cache-timestamp policy, and migration/instrumented coverage as out of scope unless a minimal adjustment is required to make these named local DAO tests runnable.
- **verify:** `./gradlew :app:testDebugUnitTest --tests "com.padelaragon.app.data.local.*"`
- **done:** The named cache tables have local regression coverage that catches schema/mapping mistakes without a device, and any intentionally uncovered local-data seam is called out explicitly in the execution summary.

### Task 2: Add `FavoritesManager` coverage
- **type:** auto
- **files:** app/src/test/java/com/padelaragon/app/data/favorites/FavoritesManagerTest.kt, app/src/main/java/com/padelaragon/app/data/favorites/FavoritesManager.kt
- **action:** Add local tests for initialization, add/remove behavior, persistence, reload-after-init, and the max-favorites cap so global state behavior is deterministic, bounded, and proven to survive the SharedPreferences seam used by the app.
- **verify:** `./gradlew :app:testDebugUnitTest --tests "com.padelaragon.app.data.favorites.FavoritesManagerTest"`
- **done:** Favorite toggling behavior is covered, including initialization requirements, persistence/reload behavior across the same stored preferences, and the maximum-size constraint.

### Task 3: Make core ViewModel logic locally testable and cover it
- **type:** auto
- **files:** app/src/main/java/com/padelaragon/app/ui/viewmodel/GroupListViewModel.kt, app/src/main/java/com/padelaragon/app/ui/viewmodel/GroupDetailViewModel.kt, app/src/main/java/com/padelaragon/app/ui/viewmodel/TeamViewModel.kt, app/src/test/java/com/padelaragon/app/ui/viewmodel/
- **action:** Isolate the pure calculations currently embedded in ViewModels—group sorting, default jornada selection, and player-stat aggregation—into forms that local tests can assert without invoking the live repository singleton, then add tests for their expected outputs and edge cases.
- **verify:** `./gradlew :app:testDebugUnitTest --tests "com.padelaragon.app.ui.viewmodel.*"`
- **done:** The suite protects the app’s important non-network state logic even though the ViewModels still depend on global singletons for full integration behavior.

## Dependency Graph
```yaml
dependency_graph:
  task_1:
    needs: [app/build.gradle.kts]
    creates: [app/src/test/java/com/padelaragon/app/data/local/]
  task_2:
    needs: [app/build.gradle.kts]
    creates: [app/src/test/java/com/padelaragon/app/data/favorites/FavoritesManagerTest.kt]
  task_3:
    needs: [app/build.gradle.kts]
    creates: [app/src/test/java/com/padelaragon/app/ui/viewmodel/]
```

## Verification
- Run each task’s test subset independently while iterating.
- After the parser coverage from `@.planning/phases/2/PLAN-1.md` and the tests from this plan are both present, finish with a full offline suite run through `./gradlew :app:testDebugUnitTest`.

## Success Criteria
1. Room/entity coverage exists for the named cache paths: groups, standings, results, match details, jornadas, and team detail/player storage.
2. `FavoritesManager` behavior is regression-tested, including the persistence/reload seam behind `init()` and `toggleFavorite()`.
3. Core ViewModel calculations are protected by local tests without hitting live network paths.
4. With the companion parser work from Phase 2 Plan 1 present, `./gradlew :app:testDebugUnitTest` passes offline for the combined Phase 2 suite.
