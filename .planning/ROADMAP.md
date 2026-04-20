# Roadmap

## Completed Phases (Testing Initiative)

### Phase 1: Stabilize the Build and Test Harness ✅
### Phase 2: Add the Realistic "Complete Suite" ✅ (partial — Plan 1 done)
### Phase 3: Final Verification, Versioning, and Commit ✅

---

## Architecture, SOLID & Performance Refactoring

> **Research:** `.planning/research/SUMMARY-architecture.md`
> **Scope:** Refactor for clean architecture, SOLID compliance, and maximum data loading speed
> **Constraint:** No new DI frameworks (manual constructor injection). No breaking changes to UI behavior.

---

## Phase 4: Repository Interfaces & Dependency Injection Foundation
**Goal:** Introduce interfaces for all data sources and wire ViewModels via constructor injection, eliminating singleton coupling.
**Requirements:** REQ-ARCH-001 (DIP), REQ-ARCH-002 (ISP), REQ-ARCH-003 (testability)
**Success Criteria:**
1. Each data domain (groups, standings, match results, jornadas, team details, match details, favorites) has a dedicated interface.
2. `LeagueRepository` singleton is refactored to a class implementing those interfaces.
3. All ViewModels receive their dependencies via constructor parameters (no `= LeagueRepository` hardcoding).
4. `FavoritesManager` is accessed via interface, not singleton reference.
5. `PadelAragonApp` and ViewModel factories wire everything together (manual DI).
6. All existing tests still pass. App builds successfully with `./gradlew :app:assembleDebug`.
**Depends on:** None (builds on current codebase)

## Phase 5: Split Monolithic Repository (SRP)
**Goal:** Break `LeagueRepository` (644 LOC) into focused, single-responsibility data sources.
**Requirements:** REQ-ARCH-004 (SRP), REQ-ARCH-005 (OCP)
**Success Criteria:**
1. Separate repository classes: `GroupRepository`, `StandingsRepository`, `MatchResultRepository`, `JornadaRepository`, `TeamDetailRepository`, `MatchDetailRepository`.
2. Each repository owns only its domain's caching (in-memory + Room), network fetching, and persistence.
3. Shared infrastructure (HtmlFetcher, semaphore, URL building, cache timestamps) is extracted to shared utilities/services.
4. Cross-domain operations (e.g., `getTeamInfo` which needs standings + results + details) are extracted to a use case / coordinator.
5. No repository exceeds ~150 LOC.
6. All existing tests pass. App builds and runs correctly.
**Depends on:** Phase 4

## Phase 6: Use Cases & Business Logic Extraction
**Goal:** Move business logic out of ViewModels and repositories into dedicated use case classes.
**Requirements:** REQ-ARCH-006 (clean arch layers), REQ-ARCH-007 (SRP for ViewModels)
**Success Criteria:**
1. `computePlayerStats` logic extracted from `TeamViewModel` into a use case.
2. `sortGroups` logic extracted from `GroupListViewModel` into a use case.
3. `findDefaultJornada` logic extracted from `GroupDetailViewModel` into a use case.
4. Prefetch orchestration (`prefetchAllGroups`, `prefetchGroups`) extracted into a use case.
5. `getTeamInfo` / `getTeamInfoForGroup` aggregation extracted into a `GetTeamInfoUseCase`.
6. ViewModels are thin — they only manage UI state and delegate to use cases.
7. All existing tests pass (test subjects may move but logic coverage is preserved).
**Depends on:** Phase 5

## Phase 7: Performance Optimizations
**Goal:** Maximize data loading speed with stale-while-revalidate, smarter caching, and parallel prefetching.
**Requirements:** REQ-PERF-001 (instant UI), REQ-PERF-002 (background refresh), REQ-PERF-003 (efficient caching)
**Success Criteria:**
1. **Stale-while-revalidate**: All repositories return cached/Room data immediately and refresh in background. UI never blocks on network when any cache exists.
2. **Eliminate duplicate Room queries**: `getMatchResults` cold-start path queries Room at most once.
3. **OkHttp disk cache**: Configured for the HtmlFetcher with appropriate cache size.
4. **Eager prefetching**: Prefetch starts concurrently with initial groups load, not after UI render.
5. **Flow-based reactive updates**: Repositories expose `StateFlow`/`Flow` so UI updates automatically when background refresh completes.
6. All existing tests pass. App builds and runs correctly.
7. Measurable improvement: data appears on screen faster on cold start and navigation.
**Depends on:** Phase 6
