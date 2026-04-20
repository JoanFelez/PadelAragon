# Architecture, SOLID & Performance Analysis — Research Summary

## Executive Summary

PadelAragon is a ~6800-line Android app (Kotlin, Jetpack Compose, Room, OkHttp, Jsoup) that scrapes league data from padelfederacion.es. While functional, the codebase has significant architectural debt that impacts maintainability, testability, and data loading performance.

## Current Architecture

```
┌─────────────────────────────────────────────┐
│ UI Layer (Compose Screens)                  │
│  GroupListScreen → GroupDetailScreen → Team  │
├─────────────────────────────────────────────┤
│ ViewModel Layer                             │
│  GroupListVM  GroupDetailVM  TeamVM          │
│  ↕ direct reference to singleton            │
├─────────────────────────────────────────────┤
│ LeagueRepository (SINGLETON OBJECT, 644 LOC)│
│  - Groups, Standings, Matches, Jornadas,    │
│    TeamDetail, MatchDetail, Prefetching,    │
│    Caching (in-memory + Room), URL building │
├─────────────────────────────────────────────┤
│ Data Layer                                  │
│  HtmlFetcher │ Parsers (5x) │ Room DB      │
│  FavoritesManager (singleton)               │
└─────────────────────────────────────────────┘
```

## Clean Architecture Issues

### CA-1: God Object Repository
`LeagueRepository.kt` (644 lines) is a Kotlin `object` (singleton) handling 6 data domains: groups, standings, match results, jornadas, team details, and match details. It also owns caching strategy, prefetching logic, URL construction, and Room persistence — violating separation of concerns.

### CA-2: No Dependency Injection
All 3 ViewModels hardcode `private val repository = LeagueRepository` (the singleton object). `FavoritesManager` uses the same pattern with `object` + `init(context)`. This makes unit testing impossible without Robolectric/mocking frameworks.

### CA-3: Missing Domain Layer / Use Cases
Business logic is spread across:
- **Repository**: finalization logic, cache TTL decisions, URL building, prefetch strategies
- **ViewModels**: group sorting (`sortGroups`), jornada selection (`findDefaultJornada`), player stats computation (`computePlayerStats` — 70 lines of aggregation logic)

### CA-4: No Abstractions (Interfaces)
Zero interfaces in the entire codebase. Every dependency is a concrete class or singleton object.

## SOLID Violations

| Principle | Status | Details |
|---|---|---|
| **SRP** | 🔴 Violated | `LeagueRepository` has 6+ responsibilities; `TeamViewModel` embeds statistics computation |
| **OCP** | 🟡 Weak | Adding new data types requires modifying the monolithic repository |
| **LSP** | ✅ N/A | No inheritance hierarchies to violate |
| **ISP** | 🔴 Violated | No interfaces exist at all; ViewModels see all repository methods |
| **DIP** | 🔴 Violated | High-level ViewModels depend on concrete low-level `LeagueRepository` object |

## Performance Issues

### PERF-1: No Stale-While-Revalidate Pattern
When cache expires, the UI blocks until the network call completes. Users see loading spinners instead of stale (but instant) data with background refresh.

### PERF-2: Duplicate Room Queries in getMatchResults
The cold-start path for `getMatchResults` can query Room up to 3 times for the same data:
1. Line 156: finalized jornada check
2. Line 166: cold-start restoration check  
3. Line 186: TTL-based check

### PERF-3: No OkHttp Disk Cache
`HtmlFetcher` has no disk cache configured. HTTP responses are never cached at the transport layer, even for static content.

### PERF-4: Prefetch Timing
`prefetchAllGroups()` fires only after the groups UI has rendered. With proper architecture, prefetching could start concurrently with the first screen render.

### PERF-5: Sequential Navigation Data Loading
When navigating to a team detail, `getTeamInfoForGroup` fetches standings + results + team detail sequentially in terms of the user flow. With a domain layer, this could be pre-warmed.

## Recommended Stack Additions
- **No new libraries needed** — Kotlin coroutines + manual DI is sufficient for this project size
- Constructor injection via manual factories (no Hilt/Koin — project is small enough)
- Interface-based repository contracts

## Roadmap Implications
1. Interfaces + DI foundation must come first (everything else depends on it)
2. Repository split is the highest-value refactor (unlocks both testability and performance)
3. Use cases layer can be introduced incrementally
4. Performance optimizations (stale-while-revalidate) can be done per-repository after the split
