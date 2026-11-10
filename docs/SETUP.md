# Puesta en marcha (coste 0 €)

Todo el backend es opcional: sin él la app funciona obteniendo los datos directamente de la web de la federación. Esta guía monta el backend completo con productos gratuitos, sin tarjeta ni facturación.

## 1. Cloudflare (Workers Free + KV)

1. Crea una cuenta gratuita en <https://dash.cloudflare.com> (sin tarjeta). **No actives ningún plan de pago.**
2. `cd backend/worker && npm ci && npx wrangler login`.
3. Crea el namespace KV: `npx wrangler kv namespace create PADEL`. Copia el `id` que imprime y sustituye `REPLACE_WITH_KV_NAMESPACE_ID` en `backend/worker/wrangler.toml` (binding `PADEL`). El binding `AI` (Workers AI, asignación gratuita) ya está declarado.
4. Variables no secretas en `wrangler.toml` (`[vars]`): `GEMINI_MODEL`, `WORKERS_AI_MODEL`, `GITHUB_REPO`, `GITHUB_REF`. Comprueba que los nombres de modelo siguen vigentes en la documentación de cada proveedor.

## 2. Secretos del Worker

Con `npx wrangler secret put NOMBRE` (en `backend/worker`):

| Secreto | Obligatorio | Uso |
|---|---|---|
| `APP_KEY` | sí | Clave que la app envía en `X-App-Key` a `/api/*`. Genera una larga: `openssl rand -hex 32`. |
| `SCRAPER_TOKEN` | sí | Bearer token del scraper para `PUT /internal/*`. Otro valor aleatorio distinto. |
| `GEMINI_API_KEY` | no | Proveedor principal del chat (ver 3). Sin él se usa Workers AI. |
| `GITHUB_DISPATCH_TOKEN` | no | PAT para que el cron del Worker lance `scrape.yml` (ver 4). Sin él no hay cron de dispatch (queda solo el `schedule` de Actions, cada 2 h). |

Para desarrollo local: `cp .dev.vars.example .dev.vars` (gitignored) y rellena `APP_KEY`, `SCRAPER_TOKEN`, `GEMINI_API_KEY`, `GITHUB_DISPATCH_TOKEN`.

Despliegue: `npx wrangler deploy`. Anota la URL (`https://padelaragon-api.<tu-subdominio>.workers.dev`).

## 3. Gemini (Google AI Studio)

Crea una API key en <https://aistudio.google.com/apikey> **sin habilitar facturación** (nivel gratuito; los límites fallan cerrados). Ten en cuenta que en el nivel gratuito los prompts pueden usarse para mejorar los modelos de Google. Guárdala como secreto `GEMINI_API_KEY`.

## 4. PAT de GitHub para el cron

Fine-grained personal access token (Settings → Developer settings), repositorio `JoanFelez/PadelAragon` únicamente, permiso **Actions: Read and write**. Caduca: apunta la fecha. Se guarda como secreto `GITHUB_DISPATCH_TOKEN` del Worker. El cron `*/30 6-22 * * *` llama a `workflow_dispatch` de `scrape.yml` sobre `GITHUB_REF` (por defecto `master`).

## 5. Secretos de Actions del repositorio

Settings → Secrets and variables → Actions:

| Secreto | Workflow | Valor |
|---|---|---|
| `WORKER_URL` | `scrape.yml` | URL del Worker, sin barra final |
| `SCRAPER_TOKEN` | `scrape.yml` | El mismo que en el Worker |
| `APP_KEY` | `scrape.yml` | El mismo que en el Worker (scraping incremental con `--previous-from`) |
| `ANDROID_KEYSTORE_BASE64` | `release.yml` | `base64 -w0 keystore.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | `release.yml` | Contraseña del keystore |
| `ANDROID_KEY_ALIAS` | `release.yml` | Alias de la clave |
| `ANDROID_KEY_PASSWORD` | `release.yml` | Contraseña de la clave |

Sin `WORKER_URL`/`SCRAPER_TOKEN`, `scrape.yml` se salta con un aviso. Sin los secretos del keystore, `release.yml` falla con un mensaje claro: **debe usarse el keystore ORIGINAL de la app**; una clave distinta obliga a desinstalar y pierde los favoritos.

## 6. Configurar la app

En `local.properties` (gitignored):

```
backend.url=https://padelaragon-api.<subdominio>.workers.dev
backend.appKey=<APP_KEY>
```

o variables de entorno `PADEL_BACKEND_URL` y `PADEL_APP_KEY` al compilar (`release.yml` las toma de los secretos `WORKER_URL` y `APP_KEY`, así que los instaladores de release ya vienen configurados y no hay que escribir nada en la app). También se pueden introducir en Ajustes → Backend en la propia app. Sin ellos: modo solo web de la federación.

## 7. Reglamento para el chat

Las fuentes (PDF) y el texto convertido están en `backend/rules/`, **gitignored: nunca se commitean** (copyright).

1. Coloca los PDF en `backend/rules/` (`normativa_tecnica_2026.pdf`, `reglamento_fap_2026.pdf`, `master_fap_2026.pdf`, `veteranos_fap_2026.pdf`, `puntos_veteranos_fap_2026.pdf`, `sistema_puntos_paf_2026.pdf`; opcionalmente `fip-reglas-2026.pdf` y `liga-aragonesa-normativa-2021.pdf`) (necesita `pdftotext`, paquete `poppler-utils`).
2. `backend/scripts/convert-rules` genera `backend/rules/reglamento.md`.
3. `backend/scripts/upload-rules.sh` lo sube a KV (`rules:current`). Para el entorno local de wrangler: `--local`.

Sin reglamento el chat solo responde sobre datos de liga y lo indica.

## 8. Release

1. Fusiona la PR `Release 3.0.0` (develop → master).
2. Etiqueta `git tag v3.0.0 && git push origin v3.0.0` (debe coincidir con `gradle.properties`). `release.yml` genera APK firmado, exe, msi y deb y crea la Release. El workflow nunca se ha ejecutado con un tag real: revisa el resultado.
3. Los instaladores de escritorio mantienen `packageName` y los módulos del instalador anterior, por lo que actualizan en el sitio las instalaciones 2.x.

## 9. Primer scraping y bloqueo de IPs

Tras fusionar a `master` (el workflow debe existir en la rama por defecto):

```
gh workflow run scrape.yml -f dry_run=true
gh run watch
```

Comprueba si `padelfederacion.es` responde a los runners de GitHub. Si bloquea esas IPs, ejecuta el scraper en tu máquina o con cron local:

```
WORKER_URL=... SCRAPER_TOKEN=... APP_KEY=... ./gradlew :scraper:run --args="--out scraper/build/out --upload --previous-from $WORKER_URL"
```

La app sigue funcionando con scraping en el dispositivo.

## 10. Lista de control de coste 0 €

- [ ] Cloudflare: plan Workers **Free**, sin método de pago. Límites: 100 000 peticiones/día, 10 ms CPU por petición; KV 100 000 lecturas y 1 000 escrituras/día (el scraper solo escribe si cambia el hash; la app usa ETag). Workers AI: asignación diaria gratuita (se agota y falla cerrado).
- [ ] Gemini: key de AI Studio sin facturación; si se agota la cuota, el chat cae a Workers AI.
- [ ] GitHub: repositorio público (Actions gratis); el PAT solo con Actions en este repo.
- [ ] Ningún secreto en git; `backend/rules/`, `.dev.vars`, `local.properties` ignorados.
- [ ] Revisa que los cuadros de uso de Cloudflare y AI Studio no muestren cargos.
