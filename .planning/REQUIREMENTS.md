# Requirements

| ID | Requirement | Phase | Priority |
|---|---|---|---|
| REQ-001 | The app must compile from a clean checkout before any test work proceeds, including resolving or preserving the fix for the `LeagueRepository.kt` `cachedMatchDetails` membership ambiguity. | Phase 1 | Must-have |
| REQ-002 | The `:app` module must declare an explicit local test stack sufficient to run deterministic JVM tests in this workspace without requiring a connected device. | Phase 1 | Must-have |
| REQ-003 | The repository must gain stable test source sets and reusable HTML fixtures/resources for deterministic parser coverage. | Phase 1 | Must-have |
| REQ-004 | The automated suite must cover all parser classes in `app/src/main/java/com/padelaragon/app/data/parser/` for both happy-path and fallback/error-tolerance behavior. | Phase 2 | Must-have |
| REQ-005 | The automated suite must cover local persistence/state seams that are realistic for this repo today: Room entities/DAOs and `FavoritesManager`. | Phase 2 | Must-have |
| REQ-006 | Meaningful non-network UI/state logic currently embedded in `GroupListViewModel.kt`, `GroupDetailViewModel.kt`, and `TeamViewModel.kt` must become verifiable by local tests. | Phase 2 | Must-have |
| REQ-007 | The must-pass suite for this task must run via Gradle in the current environment without live-network dependence and without requiring `connectedDebugAndroidTest`. | Phase 2 | Must-have |
| REQ-008 | The patch version must be bumped from `1.2.0` to `1.2.1` only after the final code and tests are passing. | Phase 3 | Must-have |
| REQ-009 | The final change set must be reviewed for unrelated pre-existing working tree changes before commit, and the commit must capture only the intended testing/build/version work. | Phase 3 | Must-have |
| REQ-010 | The final verification procedure must be documented as executable shell commands for downstream coding and verification agents. | Phase 3 | Must-have |
