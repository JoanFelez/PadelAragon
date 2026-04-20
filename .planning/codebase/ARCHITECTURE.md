# Codebase Architecture
## Pattern
Single-module Android app with a Compose UI, ViewModel-driven presentation layer, a singleton repository, and local persistence/cache via Room.

## Directory Structure
- `@app/src/main/java/com/padelaragon/app/MainActivity.kt` — app entry activity, sets Compose content.
- `@app/src/main/java/com/padelaragon/app/PadelAragonApp.kt` — application startup, initializes security provider, favorites, Room, and repository.
- `@app/src/main/java/com/padelaragon/app/ui/navigation/NavGraph.kt` — top-level navigation graph.
- `@app/src/main/java/com/padelaragon/app/ui/screen/` — screen composables (`GroupListScreen`, `GroupDetailScreen`, `TeamScreen`).
- `@app/src/main/java/com/padelaragon/app/ui/viewmodel/` — screen-specific ViewModels holding `StateFlow` UI state.
- `@app/src/main/java/com/padelaragon/app/data/repository/LeagueRepository.kt` — singleton data orchestration, cache, Room access, network fetches, HTML parsing.
- `@app/src/main/java/com/padelaragon/app/data/network/HtmlFetcher.kt` — OkHttp wrapper.
- `@app/src/main/java/com/padelaragon/app/data/parser/` — Jsoup-based HTML parsers.
- `@app/src/main/java/com/padelaragon/app/data/local/` — Room database, entities, DAOs.
- `@app/src/main/java/com/padelaragon/app/data/favorites/FavoritesManager.kt` — SharedPreferences-backed favorites state.

## Key Relationships
- `MainActivity` calls `NavGraph()` from Compose in @app/src/main/java/com/padelaragon/app/MainActivity.kt.
- Screens call `viewModel()` directly and collect `StateFlow` state in @app/src/main/java/com/padelaragon/app/ui/screen/GroupListScreen.kt, @app/src/main/java/com/padelaragon/app/ui/screen/GroupDetailScreen.kt, and @app/src/main/java/com/padelaragon/app/ui/screen/TeamScreen.kt.
- ViewModels reference the global `LeagueRepository` singleton directly in @app/src/main/java/com/padelaragon/app/ui/viewmodel/GroupListViewModel.kt, @app/src/main/java/com/padelaragon/app/ui/viewmodel/GroupDetailViewModel.kt, and @app/src/main/java/com/padelaragon/app/ui/viewmodel/TeamViewModel.kt.
- `LeagueRepository` owns networking, parser invocation, in-memory caches, and Room persistence in @app/src/main/java/com/padelaragon/app/data/repository/LeagueRepository.kt.
- `PadelAragonApp` wires startup dependencies manually; there is no DI framework in @app/src/main/java/com/padelaragon/app/PadelAragonApp.kt.

## Architectural Implications for Testing
- The app is testable at the parser and DAO level, because parsers are regular classes and Room is isolated in `data/local`.
- ViewModels are harder to isolate because they create no injectable repository dependency; they bind directly to the singleton `LeagueRepository` and `FavoritesManager`.
- UI tests will need either instrumentation/Compose UI tests or refactoring to pass fake ViewModels/repositories more consistently.
