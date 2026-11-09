package com.padelaragon.shared.data.sync

import com.padelaragon.shared.BuildKonfig
import com.russhwolf.settings.Settings

/** Where league data comes from. */
enum class DataSourceMode(val label: String) {
    AUTO("Automático"),
    BACKEND("Solo backend"),
    DEVICE("Solo web federación")
}

data class BackendConfig(val baseUrl: String, val appKey: String) {
    val isComplete: Boolean get() = baseUrl.isNotBlank() && appKey.isNotBlank()
}

/**
 * User-adjustable sync configuration persisted in multiplatform-settings. User overrides win over
 * the BuildKonfig defaults (`PADEL_BACKEND_URL` / `PADEL_APP_KEY`); both blank means scrape-only.
 */
class SyncSettings(
    private val settings: Settings,
    private val defaultUrl: String = BuildKonfig.BACKEND_URL,
    private val defaultKey: String = BuildKonfig.APP_KEY
) {
    var mode: DataSourceMode
        get() = DataSourceMode.entries.firstOrNull { it.name == settings.getString(KEY_MODE, "") }
            ?: DataSourceMode.AUTO
        set(value) = settings.putString(KEY_MODE, value.name)

    var urlOverride: String
        get() = settings.getString(KEY_URL, "")
        set(value) = settings.putString(KEY_URL, value.trim())

    var keyOverride: String
        get() = settings.getString(KEY_APP_KEY, "")
        set(value) = settings.putString(KEY_APP_KEY, value.trim())

    val backendConfig: BackendConfig
        get() = BackendConfig(
            baseUrl = urlOverride.ifBlank { defaultUrl }.trim().trimEnd('/'),
            appKey = keyOverride.ifBlank { defaultKey }.trim()
        )

    /** Hash of the bundle last imported for a league, so unchanged bundles are never re-downloaded. */
    fun appliedHash(leagueId: Int): String? = settings.getStringOrNull("sync_hash_$leagueId")

    fun setApplied(leagueId: Int, hash: String) = settings.putString("sync_hash_$leagueId", hash)

    /** ETags are keyed `manifest`, `leagues` or `bundle_<leagueId>`. */
    fun etag(key: String): String? = settings.getStringOrNull("sync_etag_$key")

    fun putEtag(key: String, etag: String) = settings.putString("sync_etag_$key", etag)

    var manifestGeneratedAt: String?
        get() = settings.getStringOrNull("sync_manifest_generated_at")
        set(value) = if (value == null) settings.remove("sync_manifest_generated_at")
        else settings.putString("sync_manifest_generated_at", value)

    var leaguesJson: String?
        get() = settings.getStringOrNull("sync_leagues_json")
        set(value) = if (value == null) settings.remove("sync_leagues_json")
        else settings.putString("sync_leagues_json", value)

    var lastSyncAtMs: Long
        get() = settings.getLong("sync_last_at", 0L)
        set(value) = settings.putLong("sync_last_at", value)

    var lastSyncKind: SyncKind?
        get() = SyncKind.entries.firstOrNull { it.name == settings.getString("sync_last_kind", "") }
        set(value) = if (value == null) settings.remove("sync_last_kind")
        else settings.putString("sync_last_kind", value.name)

    /** Epoch millis of the last time the backend served fresh data; 0 when it never did. */
    var lastBackendOkAtMs: Long
        get() = settings.getLong("sync_backend_ok_at", 0L)
        set(value) = settings.putLong("sync_backend_ok_at", value)

    private companion object {
        const val KEY_MODE = "sync_mode"
        const val KEY_URL = "sync_backend_url"
        const val KEY_APP_KEY = "sync_app_key"
    }
}
