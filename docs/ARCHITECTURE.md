# Arquitectura

## Módulos

```
:shared       KMP (commonMain + androidMain + desktopMain): lógica y UI Compose
:androidApp   entrada Android, WorkManager (NotificationWorker)
:desktopApp   entrada Compose Desktop, bandeja del sistema, instaladores
:scraper      CLI JVM sobre :shared (sin Room)
backend/worker  Cloudflare Worker (Hono, KV, Workers AI)
contracts     contrato LeagueBundle + fixtures sintéticos
```

Todo el código de negocio y UI vive en `:shared/commonMain`; las plataformas solo aportan puntos de entrada, ubicación de BD/ajustes, notificaciones, bandeja y color dinámico. Pila: Ktor (OkHttp), Ksoup, kotlinx.serialization, Room KMP (+ sqlite-bundled; esquema v6, caché con migración destructiva), multiplatform-settings, navigation-compose, material3-adaptive, BuildKonfig.

Dentro de `:shared` (`com.padelaragon.shared`): `data/{model,parser,network,local,repository,sync,remote,chat,favorites}`, `domain/{usecase,notification}`, `di` (`MultiLeagueContainer` → un `AppContainer` por liga), `ui/**`, `update`.

## Flujo de datos

```
padelfederacion.es ──(scraper CLI en GitHub Actions, cada ~30 min)──▶ Worker (PUT /internal/*, KV)
                                                                          │ GET /api/* (X-App-Key, ETag/304)
                                                                          ▼
                                                              App: SyncEngine ─▶ BundleImporter ─▶ Room ─▶ Flow ─▶ UI
```

- **Scraper** (`scrape.yml`): descarga ligas de forma educada (concurrencia baja, reintentos con backoff, incremental con `--previous-from`), genera `manifest.json`, `leagues.json` y `<id>.json` con SHA-256 y los sube; el Worker solo escribe en KV si el hash cambia. El cron del Worker lanza `workflow_dispatch` (más fiable que `schedule`); hay además un `schedule` cada 2 h.
- **App**: `SyncEngine` compara el manifest (ETag) y descarga solo los bundles cambiados. Las pantallas observan Flows de Room; los repositorios aplican stale-while-revalidate (devuelven caché y refrescan si caducó el TTL, sin escribir si el hash del contenido no cambia).

## Modos de datos (fallback)

Ajustes → Origen: `AUTO` (backend si está configurado y responde; si no, web de la federación), `BACKEND` (solo backend), `DEVICE` (solo scraping en el dispositivo). Sin URL/clave de backend la app es solo-dispositivo. El scraping directo está limitado (máx. 4 peticiones simultáneas por host, deduplicación de peticiones en vuelo, reintentos con jitter, sin TLS inseguro).

## Chat (RAG + herramientas)

`POST /api/chat` (SSE) recibe los últimos 10 mensajes (1000 caracteres c/u), `leagueId` y los grupos favoritos. El Worker prepara el prompt con el reglamento (Markdown en KV `rules:current`; si pasa de ~250k caracteres (hoy se envía entero, ~175k) solo se envían las secciones relevantes a la pregunta), y llama a Gemini (`GEMINI_MODEL`, streaming). Ante 429/5xx/red o sin clave cae a Workers AI. Hasta 4 rondas de herramientas (`listLeagues`, `findGroups`, `getStandings`, `getTeam`, `getResults`, `getPlayerStats`, `possibleCouples`) que leen los bundles de KV. Respuesta: `data:{"delta"}`, `data:{"citations":[…]}`, `data:[DONE]`, o `data:{"error"}`. La app (`ChatApi`/`SseParser`/`ChatViewModel`) pinta Markdown y citas, y guarda el historial en Room. Si el backend no está configurado muestra un mensaje claro y nada más se ve afectado.

## Notificaciones

Por sondeo, sin Firebase. `ResultNotifier` sincroniza los grupos favoritos (backend o scraping limitado a una vez cada 3 h) y `ChangeDetector` compara con el último estado guardado (`notif_state_<liga>_<grupo>`) para emitir eventos (resultados nuevos, cambios de clasificación). Android: `NotificationWorker` (WorkManager, intervalo 1/3/6/12 h, permiso `POST_NOTIFICATIONS`). Escritorio: bucle de 60 s mientras la app está abierta y `Tray`.

## Contrato

`contracts/README.md` define `Manifest`, `League[]` y `LeagueBundle` con `schemaVersion` (actualmente 1). Dentro de una versión solo hay cambios aditivos y los consumidores ignoran claves desconocidas. Kotlin (`BundleContractTest`) y TypeScript validan el fixture sintético `contracts/fixtures/league-bundle.sample.json`. Los bundles reales contienen nombres y edades: solo se sirven tras `X-App-Key` y nunca se commitean.

## Seguridad

`/api/*` exige `X-App-Key`; `/internal/*` exige `Authorization: Bearer <SCRAPER_TOKEN>`. Sin secretos en git. R8 desactivado.

## Release y actualizaciones

`release.yml` (tags `v*`) verifica que el tag coincide con `gradle.properties`, firma el APK con el keystore original, genera exe/msi/deb y publica la Release. La app consulta `releases/latest` de GitHub como mucho una vez al día (`update/UpdateChecker`) y, si la versión es mayor, muestra «Nueva versión disponible» en Ajustes con enlace a la Release; falla en silencio.
