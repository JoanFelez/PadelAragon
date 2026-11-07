package com.padelaragon.shared.update

import com.padelaragon.shared.appVersion
import com.padelaragon.shared.data.network.createPlatformHttpClient
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

data class UpdateInfo(val version: String, val url: String)

/** Dotted numeric version (`3.0.1`, optional leading `v` and `-suffix`/`+build` ignored). */
object Semver {
    fun parse(raw: String): List<Int>? {
        val core = raw.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
        val parts = core.split('.').map { it.toIntOrNull() ?: return null }
        return parts.takeIf { it.isNotEmpty() }
    }

    /** True when [candidate] is strictly newer than [current]; unparsable input is never newer. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate) ?: return false
        val b = parse(current) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

/**
 * Looks for a newer GitHub release at most once every [CHECK_INTERVAL_MS]; the last answer is
 * cached in [settings]. Failures are silent (no update shown, retried next day).
 */
class UpdateChecker(
    private val settings: Settings,
    private val currentVersion: String = appVersion,
    private val clientFactory: () -> HttpClient = ::createUpdateClient,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    /** Cached result; no network. */
    fun cached(): UpdateInfo? {
        val version = settings.getStringOrNull(KEY_VERSION) ?: return null
        val url = settings.getStringOrNull(KEY_URL) ?: return null
        return UpdateInfo(version, url).takeIf { Semver.isNewer(version, currentVersion) }
    }

    suspend fun checkIfDue(): UpdateInfo? {
        val now = nowMs()
        val last = settings.getLong(KEY_LAST_CHECK, 0L)
        if (now - last in 0 until CHECK_INTERVAL_MS) return cached()
        settings.putLong(KEY_LAST_CHECK, now)
        try {
            val client = clientFactory()
            val body = try {
                val response = client.get(LATEST_RELEASE_URL) {
                    header(HttpHeaders.Accept, "application/vnd.github+json")
                }
                if (!response.status.isSuccess()) return cached()
                response.bodyAsText()
            } finally {
                client.close()
            }
            val info = parseRelease(body)
            if (info != null) {
                settings.putString(KEY_VERSION, info.version)
                settings.putString(KEY_URL, info.url)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
        return cached()
    }

    companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/JoanFelez/PadelAragon/releases/latest"
        const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
        private const val KEY_LAST_CHECK = "update_last_check"
        private const val KEY_VERSION = "update_latest_version"
        private const val KEY_URL = "update_latest_url"

        fun createUpdateClient(): HttpClient = createPlatformHttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 20_000
            }
            expectSuccess = false
        }

        fun parseRelease(body: String): UpdateInfo? {
            val json = Json.parseToJsonElement(body).jsonObject
            if (json.flag("draft") || json.flag("prerelease")) return null
            val tag = json["tag_name"]?.jsonPrimitive?.content ?: return null
            val url = json["html_url"]?.jsonPrimitive?.content ?: return null
            if (Semver.parse(tag) == null || !url.startsWith("https://")) return null
            return UpdateInfo(tag.removePrefix("v"), url)
        }

        private fun JsonObject.flag(key: String) = this[key]?.jsonPrimitive?.boolean ?: false
    }
}
