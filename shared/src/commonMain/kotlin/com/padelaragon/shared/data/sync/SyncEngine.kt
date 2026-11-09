package com.padelaragon.shared.data.sync

import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.remote.BackendApi
import com.padelaragon.shared.data.remote.Conditional
import com.padelaragon.shared.data.remote.dto.ManifestDto
import com.padelaragon.shared.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

enum class SyncKind(val label: String) {
    BACKEND_UPDATED("Datos actualizados desde el backend"),
    BACKEND_UNCHANGED("Backend sin cambios"),
    SCRAPED("Datos obtenidos de la web de la federación"),
    FAILED("Error de sincronización")
}

data class SyncResult(val kind: SyncKind, val message: String? = null)

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncAtMs: Long = 0L,
    val lastKind: SyncKind? = null,
    val lastMessage: String? = null
)

/**
 * Keeps Room up to date. Order in [DataSourceMode.AUTO]: manifest -> changed bundle -> single-transaction
 * import; if the backend is unconfigured, down or its data older than [maxBackendAgeMs] the
 * on-device scraper ([scrapeLeague]) runs instead. Never throws (except cancellation): the app
 * keeps working from the cache whatever happens.
 */
class SyncEngine(
    private val api: BackendApi,
    private val importer: BundleImporter,
    private val settings: SyncSettings,
    private val scrapeLeague: suspend (League) -> Unit,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val maxBackendAgeMs: Long = DEFAULT_MAX_BACKEND_AGE_MS
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(
        SyncState(lastSyncAtMs = settings.lastSyncAtMs, lastKind = settings.lastSyncKind)
    )
    val state: StateFlow<SyncState> = _state.asStateFlow()

    suspend fun sync(league: League): SyncResult = mutex.withLock {
        _state.value = _state.value.copy(isSyncing = true)
        val result = try {
            run(league)
        } catch (e: CancellationException) {
            _state.value = _state.value.copy(isSyncing = false)
            throw e
        }
        val now = nowMs()
        settings.lastSyncAtMs = now
        settings.lastSyncKind = result.kind
        _state.value = SyncState(false, now, result.kind, result.message)
        result
    }

    /** Syncs [leagues] one after another; returns the worst-case summary (FAILED if any failed). */
    suspend fun syncAll(leagues: List<League>): SyncResult {
        val results = leagues.map { sync(it) }
        return results.firstOrNull { it.kind == SyncKind.FAILED } ?: results.firstOrNull() ?: SyncResult(SyncKind.BACKEND_UNCHANGED)
    }

    /**
     * Whether repositories may revalidate stale cache entries by scraping in the background.
     * Off while a healthy backend is the source of truth (AUTO) or the user chose backend-only.
     */
    fun backgroundScrapingAllowed(): Boolean = when (settings.mode) {
        DataSourceMode.DEVICE -> true
        DataSourceMode.BACKEND -> false
        DataSourceMode.AUTO ->
            !(settings.backendConfig.isComplete && nowMs() - settings.lastBackendOkAtMs < maxBackendAgeMs)
    }

    private suspend fun run(league: League): SyncResult {
        val mode = settings.mode
        val config = settings.backendConfig
        if (mode == DataSourceMode.DEVICE) return scrape(league, null)
        if (!config.isComplete) return scrape(league, "Backend no configurado")

        return try {
            val outcome = fromBackend(league, config)
            if (outcome.stale) {
                if (mode == DataSourceMode.AUTO) scrape(league, "Datos del backend antiguos")
                else SyncResult(outcome.kind, "Datos del backend antiguos")
            } else {
                settings.lastBackendOkAtMs = nowMs()
                SyncResult(outcome.kind)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w("SyncEngine", "Backend sync failed for ${league.id}: ${e.message}")
            if (mode == DataSourceMode.AUTO) scrape(league, e.message)
            else SyncResult(SyncKind.FAILED, e.message ?: "Backend no disponible")
        }
    }

    private class BackendOutcome(val kind: SyncKind, val stale: Boolean)

    private suspend fun fromBackend(league: League, config: BackendConfig): BackendOutcome {
        var generatedAt = settings.manifestGeneratedAt
        var manifest: ManifestDto? = null
        val storedHash = settings.appliedHash(league.id)

        when (val r = api.manifest(config, settings.etag("manifest"))) {
            is Conditional.Fetched -> {
                manifest = r.value
                generatedAt = r.value.generatedAt
                settings.manifestGeneratedAt = generatedAt
                r.etag?.let { settings.putEtag("manifest", it) }
            }
            Conditional.NotModified -> {
                // Without the document we can only trust leagues already imported; otherwise refetch it.
                if (storedHash == null) {
                    val full = (api.manifest(config) as Conditional.Fetched)
                    manifest = full.value
                    generatedAt = full.value.generatedAt
                    settings.manifestGeneratedAt = generatedAt
                    full.etag?.let { settings.putEtag("manifest", it) }
                }
            }
        }
        val stale = isStale(generatedAt)
        if (manifest == null) return BackendOutcome(SyncKind.BACKEND_UNCHANGED, stale)

        val entry = manifest.leagues.firstOrNull { it.id == league.id }
            ?: throw BackendUnavailableLeague(league)
        if (entry.hash == storedHash) return BackendOutcome(SyncKind.BACKEND_UNCHANGED, stale)

        return when (val b = api.bundle(config, league.id, settings.etag("bundle_${league.id}"))) {
            is Conditional.Fetched -> {
                importer.import(b.value, nowMs())
                settings.setApplied(league.id, entry.hash)
                b.etag?.let { settings.putEtag("bundle_${league.id}", it) }
                BackendOutcome(SyncKind.BACKEND_UPDATED, stale)
            }
            Conditional.NotModified -> {
                settings.setApplied(league.id, entry.hash)
                BackendOutcome(SyncKind.BACKEND_UNCHANGED, stale)
            }
        }
    }

    private fun isStale(generatedAt: String?): Boolean {
        val instant = generatedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return true
        return nowMs() - instant.toEpochMilliseconds() > maxBackendAgeMs
    }

    private suspend fun scrape(league: League, reason: String?): SyncResult = try {
        scrapeLeague(league)
        SyncResult(SyncKind.SCRAPED, reason)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.w("SyncEngine", "Scrape fallback failed for ${league.id}: ${e.message}")
        SyncResult(SyncKind.FAILED, e.message ?: "Sin conexión")
    }

    private class BackendUnavailableLeague(league: League) :
        Exception("La liga ${league.displayName} no está en el backend")

    companion object {
        const val DEFAULT_MAX_BACKEND_AGE_MS = 2 * 60 * 60 * 1000L
    }
}
