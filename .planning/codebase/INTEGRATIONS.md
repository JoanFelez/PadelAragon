# External Integrations
| Integration | Type | Config | Notes |
|---|---|---|---|
| padelfederacion.es Aragón padel pages | HTML website / scraping target | @app/src/main/java/com/padelaragon/app/data/repository/LeagueRepository.kt | Main runtime dependency; repository builds URLs from `BASE_URL` and scrapes live HTML. |
| OkHttp | HTTP client | @app/src/main/java/com/padelaragon/app/data/network/HtmlFetcher.kt | Shared singleton client, custom TLS behavior in debug builds. |
| Jsoup | HTML parser | @app/src/main/java/com/padelaragon/app/data/parser/ | Parsing correctness depends on external page structure. |
| Room | Local database cache | @app/src/main/java/com/padelaragon/app/data/local/AppDatabase.kt | DB version 3, `exportSchema = false`, destructive migration disabled. |
| SharedPreferences | local favorites storage | @app/src/main/java/com/padelaragon/app/data/favorites/FavoritesManager.kt | Global singleton with initialization requirement. |
| Google Play ProviderInstaller | device security provider update | @app/src/main/java/com/padelaragon/app/PadelAragonApp.kt | Makes app startup less pure for tests; may need application-level stubbing in instrumentation. |
