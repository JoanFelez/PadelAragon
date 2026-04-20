---
phase: 2
plan: 1
type: implement
wave: 2
depends_on: [".planning/phases/1/PLAN.md"]
files_modified: [app/src/test/java/com/padelaragon/app/data/parser/, app/src/test/resources/fixtures/parser/, app/src/main/java/com/padelaragon/app/data/parser/]
autonomous: true
must_haves:
  observable_truths:
    - "Every parser has deterministic local coverage."
    - "Parser tests prove both expected extraction and resilience to partial/malformed HTML."
    - "The suite does not touch the live network."
  artifacts:
    - path: app/src/test/java/com/padelaragon/app/data/parser/GroupParserTest.kt
      has: [direct assertions for GroupParser extraction and malformed/partial fallback]
    - path: app/src/test/java/com/padelaragon/app/data/parser/StandingsParserTest.kt
      has: [direct assertions for StandingsParser extraction and malformed/partial fallback]
    - path: app/src/test/java/com/padelaragon/app/data/parser/MatchResultParserTest.kt
      has: [direct assertions for MatchResultParser extraction and malformed/partial fallback]
    - path: app/src/test/java/com/padelaragon/app/data/parser/MatchDetailParserTest.kt
      has: [direct assertions for MatchDetailParser extraction and malformed/partial fallback]
    - path: app/src/test/java/com/padelaragon/app/data/parser/TeamDetailParserTest.kt
      has: [direct assertions for TeamDetailParser extraction and malformed/partial fallback]
    - path: app/src/test/resources/fixtures/parser/
      has: [parser-specific HTML fixture folders owned by individual tasks]
  key_links:
    - from: "app/src/main/java/com/padelaragon/app/data/parser/GroupParser.kt"
      to: "app/src/test/java/com/padelaragon/app/data/parser/GroupParserTest.kt"
      verify: "GroupParserTest loads only local fixtures from app/src/test/resources/fixtures/parser/group/"
    - from: "app/src/main/java/com/padelaragon/app/data/parser/StandingsParser.kt"
      to: "app/src/test/java/com/padelaragon/app/data/parser/StandingsParserTest.kt"
      verify: "StandingsParserTest loads only local fixtures from app/src/test/resources/fixtures/parser/standings/"
    - from: "app/src/main/java/com/padelaragon/app/data/parser/MatchResultParser.kt"
      to: "app/src/test/java/com/padelaragon/app/data/parser/MatchResultParserTest.kt"
      verify: "MatchResultParserTest loads only local fixtures from app/src/test/resources/fixtures/parser/results/"
    - from: "app/src/main/java/com/padelaragon/app/data/parser/MatchDetailParser.kt"
      to: "app/src/test/java/com/padelaragon/app/data/parser/MatchDetailParserTest.kt"
      verify: "MatchDetailParserTest loads only local fixtures from app/src/test/resources/fixtures/parser/match-detail/"
    - from: "app/src/main/java/com/padelaragon/app/data/parser/TeamDetailParser.kt"
      to: "app/src/test/java/com/padelaragon/app/data/parser/TeamDetailParserTest.kt"
      verify: "TeamDetailParserTest loads only local fixtures from app/src/test/resources/fixtures/parser/team-detail/"
---

# Phase 2, Plan 1: Parser Coverage

## Objective
Add the parser-focused slice of Phase 2: a high-value deterministic suite around the pure HTML parsers. This plan does not own the entire Phase 2 gate by itself; it contributes the parser portion, while the full `./gradlew :app:testDebugUnitTest` phase gate is satisfied jointly with the companion Phase 2 plan.

## Context
@.planning/ROADMAP.md
@.planning/codebase/ARCHITECTURE.md
@.planning/codebase/TESTING.md
@app/src/main/java/com/padelaragon/app/data/parser/GroupParser.kt
@app/src/main/java/com/padelaragon/app/data/parser/StandingsParser.kt
@app/src/main/java/com/padelaragon/app/data/parser/MatchResultParser.kt
@app/src/main/java/com/padelaragon/app/data/parser/MatchDetailParser.kt
@app/src/main/java/com/padelaragon/app/data/parser/TeamDetailParser.kt

## Task Notes
- If you encounter an authentication/authorization error during execution (OAuth, API key, SSO, etc.), stop immediately and return a checkpoint requesting the user to authenticate.
- Use fixture names that describe the scenario, not the source website URL.
- Keep fixture ownership disjoint by task: Task 1 owns `app/src/test/resources/fixtures/parser/group/`, `app/src/test/resources/fixtures/parser/standings/`, and `app/src/test/resources/fixtures/parser/results/`; Task 2 owns `app/src/test/resources/fixtures/parser/match-detail/` and `app/src/test/resources/fixtures/parser/team-detail/`. Do not rename, overwrite, or repurpose another task's fixture folder.
- If a parser needs a shared test helper, place it in test code under `app/src/test/java/...`, not as a shared mutable fixture file.

## Parser Traceability

| Parser source | Test artifact | Fixture ownership | Verification target |
|---|---|---|---|
| `app/src/main/java/com/padelaragon/app/data/parser/GroupParser.kt` | `app/src/test/java/com/padelaragon/app/data/parser/GroupParserTest.kt` | `app/src/test/resources/fixtures/parser/group/` | `./gradlew :app:testDebugUnitTest --tests "com.padelaragon.app.data.parser.GroupParserTest"` |
| `app/src/main/java/com/padelaragon/app/data/parser/StandingsParser.kt` | `app/src/test/java/com/padelaragon/app/data/parser/StandingsParserTest.kt` | `app/src/test/resources/fixtures/parser/standings/` | `./gradlew :app:testDebugUnitTest --tests "com.padelaragon.app.data.parser.StandingsParserTest"` |
| `app/src/main/java/com/padelaragon/app/data/parser/MatchResultParser.kt` | `app/src/test/java/com/padelaragon/app/data/parser/MatchResultParserTest.kt` | `app/src/test/resources/fixtures/parser/results/` | `./gradlew :app:testDebugUnitTest --tests "com.padelaragon.app.data.parser.MatchResultParserTest"` |
| `app/src/main/java/com/padelaragon/app/data/parser/MatchDetailParser.kt` | `app/src/test/java/com/padelaragon/app/data/parser/MatchDetailParserTest.kt` | `app/src/test/resources/fixtures/parser/match-detail/` | `./gradlew :app:testDebugUnitTest --tests "com.padelaragon.app.data.parser.MatchDetailParserTest"` |
| `app/src/main/java/com/padelaragon/app/data/parser/TeamDetailParser.kt` | `app/src/test/java/com/padelaragon/app/data/parser/TeamDetailParserTest.kt` | `app/src/test/resources/fixtures/parser/team-detail/` | `./gradlew :app:testDebugUnitTest --tests "com.padelaragon.app.data.parser.TeamDetailParserTest"` |

## Tasks

### Task 1: Cover group, standings, and jornada/result extraction
- **type:** auto
- **files:** app/src/test/java/com/padelaragon/app/data/parser/GroupParserTest.kt, app/src/test/java/com/padelaragon/app/data/parser/StandingsParserTest.kt, app/src/test/java/com/padelaragon/app/data/parser/MatchResultParserTest.kt, app/src/test/resources/fixtures/parser/group/, app/src/test/resources/fixtures/parser/standings/, app/src/test/resources/fixtures/parser/results/
- **action:** Add local tests for `GroupParser`, `StandingsParser`, and `MatchResultParser` using task-owned saved HTML fixtures. Cover expected extraction plus partial/empty/fallback cases, and keep each parser’s fixtures in its assigned folder so this task does not modify Task 2’s fixture space.
- **verify:** `./gradlew :app:testDebugUnitTest --tests "com.padelaragon.app.data.parser.GroupParserTest" --tests "com.padelaragon.app.data.parser.StandingsParserTest" --tests "com.padelaragon.app.data.parser.MatchResultParserTest"`
- **done:** `GroupParser`, `StandingsParser`, and `MatchResultParser` each have deterministic fixture-based tests, and their fixtures live only in the folders owned by this task.

### Task 2: Cover match-detail and team-detail parsing edge cases
- **type:** auto
- **files:** app/src/test/java/com/padelaragon/app/data/parser/MatchDetailParserTest.kt, app/src/test/java/com/padelaragon/app/data/parser/TeamDetailParserTest.kt, app/src/test/resources/fixtures/parser/match-detail/, app/src/test/resources/fixtures/parser/team-detail/
- **action:** Add local tests for `MatchDetailParser` and `TeamDetailParser` using only the fixture folders owned by this task. Cover pair extraction, set extraction, fallback player-name handling, team metadata extraction, captain detection, and generic table fallback behavior without modifying Task 1’s fixture folders.
- **verify:** `./gradlew :app:testDebugUnitTest --tests "com.padelaragon.app.data.parser.MatchDetailParserTest" --tests "com.padelaragon.app.data.parser.TeamDetailParserTest"`
- **done:** `MatchDetailParser` and `TeamDetailParser` have stable local tests, and their fixtures live only in the folders owned by this task.

## Dependency Graph
```yaml
dependency_graph:
  task_1:
    needs: [app/build.gradle.kts]
    creates: [app/src/test/java/com/padelaragon/app/data/parser/GroupParserTest.kt, app/src/test/java/com/padelaragon/app/data/parser/StandingsParserTest.kt, app/src/test/java/com/padelaragon/app/data/parser/MatchResultParserTest.kt, app/src/test/resources/fixtures/parser/group/, app/src/test/resources/fixtures/parser/standings/, app/src/test/resources/fixtures/parser/results/]
  task_2:
    needs: [app/build.gradle.kts]
    creates: [app/src/test/java/com/padelaragon/app/data/parser/MatchDetailParserTest.kt, app/src/test/java/com/padelaragon/app/data/parser/TeamDetailParserTest.kt, app/src/test/resources/fixtures/parser/match-detail/, app/src/test/resources/fixtures/parser/team-detail/]
```

## Verification
- Run the parser test classes directly while iterating.
- Finish this plan with the five parser test classes only: use `./gradlew :app:testDebugUnitTest --tests "com.padelaragon.app.data.parser.*"` if the Gradle pattern works in the local shell; otherwise invoke the five parser test classes explicitly.
- Treat the full Phase 2 gate `./gradlew :app:testDebugUnitTest` as a joint verification step across Phase 2 Plan 1 and Phase 2 Plan 2, not as a requirement for this parser slice alone.

## Success Criteria
1. All five parser classes have direct test coverage with a clear source-file to test-file mapping.
2. At least one malformed or fallback scenario exists for each parser family.
3. The tests are fully local and fixture-driven, with task-owned fixture folders that do not conflict.
