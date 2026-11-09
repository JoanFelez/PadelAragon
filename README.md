# PadelAragon

Aplicación para seguir las ligas de pádel de la Federación Aragonesa: clasificaciones, jornadas, resultados, equipos, jugadores, generador de posibles parejas, chat con el reglamento y avisos de resultados. Una única base de código Kotlin Multiplatform para **Android** y **Windows/Linux (escritorio)**. Interfaz en español.

Versión actual: **3.0.0** (ver [CHANGELOG](CHANGELOG.md)).

## Funcionalidades

- Varias ligas (lista del backend o fallback integrado), grupos, clasificación, jornadas, resultados y detalle de partido.
- Ficha de equipo y de jugador, y generador de posibles parejas (veteranos).
- Favoritos por liga (máximo 3 grupos).
- Chat con el reglamento y los datos de la liga (opcional, necesita backend).
- Notificaciones de resultados nuevos en tus grupos favoritos (WorkManager en Android, bandeja del sistema en escritorio).
- Aviso de nueva versión (consulta a GitHub Releases como mucho una vez al día).
- Interfaz adaptable: barra inferior en móvil, raíl lateral y vista maestro-detalle en pantallas anchas. Tema claro/oscuro y color dinámico en Android 12+.
- Funciona sin backend: si no hay backend configurado o falla, los datos se obtienen directamente de la web de la federación.

## Capturas

_Pendiente: añadir capturas de Android y escritorio._

## Arquitectura

| Módulo | Contenido |
|---|---|
| `:shared` | Kotlin Multiplatform (commonMain): modelos, parsers (Ksoup), cliente Ktor, Room KMP, repositorios, sincronización, chat, notificaciones, ViewModels y UI Compose. |
| `:androidApp` | Punto de entrada Android (`com.padelaragon.app`), WorkManager. |
| `:desktopApp` | Compose Desktop (`PadelAragon`), instaladores exe/msi/deb. |
| `:scraper` | CLI JVM que reutiliza `:shared` para generar los bundles JSON y subirlos al Worker. |
| `backend/worker` | Cloudflare Worker (Hono + KV): API de datos y `/api/chat`. |
| `contracts` | Contrato `LeagueBundle` (`schemaVersion`) y fixtures sintéticos. |

Detalles en [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). La puesta en marcha del backend (coste 0 €) está en [docs/SETUP.md](docs/SETUP.md).

## Compilar

Requisitos: JDK 21 y, para Android, el SDK (plataforma 37, build-tools 36.1). Crea `local.properties` con `sdk.dir=...` si hace falta.

```
./gradlew check                          # tests
./gradlew :androidApp:assembleDebug      # APK de depuración
./gradlew :desktopApp:run                # ejecutar escritorio
./gradlew :desktopApp:createDistributable
./gradlew :scraper:run --args="--out scraper/build/out"
cd backend/worker && npm ci && npm test  # Worker
```

Opcional, para usar tu backend: `backend.url` y `backend.appKey` en `local.properties` (o variables `PADEL_BACKEND_URL` / `PADEL_APP_KEY`). Sin ellos la app compila en modo solo-web-federación. La versión se define una vez en `gradle.properties` (`appVersionMajor/Minor/Patch`).

## Releases

Al publicar un tag `v*` el workflow `release.yml` construye el APK firmado, el exe/msi de Windows y el deb de Linux y crea la GitHub Release. Ver [docs/SETUP.md](docs/SETUP.md#8-release).
