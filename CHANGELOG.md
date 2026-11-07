# Changelog

## 3.0.0

Versión mayor: Android (2.0.1) y escritorio (2.1.1) pasan a ser una única aplicación Kotlin Multiplatform.

### Cambios incompatibles
- **App unificada KMP** para Android y escritorio con paridad de funcionalidades (ligas múltiples, ficha de jugador, generador de parejas, maestro-detalle).
- **La caché local se reinicia**: nuevo esquema Room (v6, migración destructiva). Los favoritos se conservan (migración desde las preferencias anteriores de Android y escritorio).
- Identidad conservada: mismo `applicationId` en Android (`com.padelaragon.app`) y mismo `packageName`/instalador en escritorio, así que las instalaciones existentes se actualizan en el sitio (el APK debe firmarse con la clave original).

### Novedades
- Backend opcional gratuito (Cloudflare Worker + KV) alimentado por un scraper en GitHub Actions; la app sincroniza con ETag y vuelve al scraping directo si no hay backend.
- Chat con el reglamento y los datos de la liga (Gemini con fallback a Workers AI).
- Notificaciones de resultados (WorkManager en Android, bandeja en escritorio).
- Pipeline de releases (`release.yml`) con APK firmado, exe, msi y deb.
- Aviso de nueva versión en Ajustes.
- Documentación: README, `docs/SETUP.md`, `docs/ARCHITECTURE.md`.

### Errores corregidos
- Stale-while-revalidate: ambas ramas de la comprobación de TTL devolvían datos obsoletos; ahora se refresca realmente.
- Caché HTTP inútil (las peticiones son POST): eliminada.
- TLS «confiar en todo» eliminado de todas las compilaciones.
- Concurrencia: máximo 4 peticiones simultáneas por host (antes 15), deduplicación de peticiones en vuelo, reintentos con backoff y jitter, y sin escrituras en BD si el contenido no cambia.
