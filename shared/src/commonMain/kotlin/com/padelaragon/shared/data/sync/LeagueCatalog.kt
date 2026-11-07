package com.padelaragon.shared.data.sync

import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.remote.BackendApi
import com.padelaragon.shared.data.remote.Conditional
import com.padelaragon.shared.data.remote.dto.BundleJson
import com.padelaragon.shared.data.remote.dto.LeagueDto
import com.padelaragon.shared.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer

/**
 * The leagues offered by the app: backend `/api/leagues` (cached in settings, ETag-aware) with the
 * built-in [League.entries] as fallback, so league ids can change each season without a release.
 */
class LeagueCatalog(
    private val api: BackendApi,
    private val settings: SyncSettings
) {
    private val _leagues = MutableStateFlow(readCached() ?: League.entries)
    val leagues: StateFlow<List<League>> = _leagues.asStateFlow()

    fun byId(id: Int): League? = _leagues.value.firstOrNull { it.id == id } ?: League.fromId(id)

    /** Refreshes from the backend when allowed; keeps the current list on any failure. */
    suspend fun refresh() {
        val config = settings.backendConfig
        if (settings.mode == DataSourceMode.DEVICE || !config.isComplete) return
        try {
            val result = api.leagues(config, settings.etag("leagues").takeIf { settings.leaguesJson != null })
            if (result is Conditional.Fetched && result.value.isNotEmpty()) {
                settings.leaguesJson = BundleJson.encodeToString(ListSerializer(LeagueDto.serializer()), result.value)
                result.etag?.let { settings.putEtag("leagues", it) }
                _leagues.value = result.value.map { League(it.id, it.name) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w("LeagueCatalog", "Leagues refresh failed: ${e.message}")
        }
    }

    private fun readCached(): List<League>? = settings.leaguesJson?.let { json ->
        runCatching { BundleJson.decodeFromString(ListSerializer(LeagueDto.serializer()), json) }
            .getOrNull()?.takeIf { it.isNotEmpty() }?.map { League(it.id, it.name) }
    }
}
