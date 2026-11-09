package com.padelaragon.shared.data.remote

import com.padelaragon.shared.data.network.createPlatformHttpClient
import com.padelaragon.shared.data.remote.dto.BundleJson
import com.padelaragon.shared.data.remote.dto.LeagueBundleDto
import com.padelaragon.shared.data.remote.dto.LeagueDto
import com.padelaragon.shared.data.remote.dto.ManifestDto
import com.padelaragon.shared.data.sync.BackendConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.builtins.ListSerializer

/** Outcome of a conditional GET: a new document (with its ETag) or "nothing changed". */
sealed interface Conditional<out T> {
    data class Fetched<T>(val value: T, val etag: String?) : Conditional<T>
    data object NotModified : Conditional<Nothing>
}

class BackendException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Client of the Worker data API (`contracts/README.md`): `GET /api/manifest`, `/api/leagues` and
 * `/api/leagues/:id/bundle`, authenticated with `X-App-Key`. Pass the stored ETag as
 * [etag] to get a 304 ([Conditional.NotModified]) when nothing changed. Any other failure,
 * including malformed bodies, is thrown as [BackendException].
 */
class BackendApi(private val client: HttpClient = createDefaultBackendClient()) {

    suspend fun manifest(config: BackendConfig, etag: String? = null): Conditional<ManifestDto> =
        fetch(config, "/api/manifest", etag) { BundleJson.decodeFromString(ManifestDto.serializer(), it) }

    suspend fun leagues(config: BackendConfig, etag: String? = null): Conditional<List<LeagueDto>> =
        fetch(config, "/api/leagues", etag) {
            BundleJson.decodeFromString(ListSerializer(LeagueDto.serializer()), it)
        }

    suspend fun bundle(config: BackendConfig, leagueId: Int, etag: String? = null): Conditional<LeagueBundleDto> =
        fetch(config, "/api/leagues/$leagueId/bundle", etag) {
            BundleJson.decodeFromString(LeagueBundleDto.serializer(), it)
        }

    private suspend fun <T> fetch(
        config: BackendConfig,
        path: String,
        etag: String?,
        parse: (String) -> T
    ): Conditional<T> {
        if (!config.isComplete) throw BackendException("Backend not configured")
        try {
            val response = client.get(config.baseUrl + path) {
                header("X-App-Key", config.appKey)
                header(HttpHeaders.Accept, "application/json")
                if (etag != null) header(HttpHeaders.IfNoneMatch, etag)
            }
            return when {
                response.status == HttpStatusCode.NotModified -> Conditional.NotModified
                response.status.value in 200..299 ->
                    Conditional.Fetched(parse(response.bodyAsText()), response.headers[HttpHeaders.ETag])
                else -> throw BackendException("HTTP ${response.status.value} for $path")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackendException) {
            throw e
        } catch (e: Exception) {
            throw BackendException("Backend request failed for $path: ${e.message}", e)
        }
    }

    companion object {
        fun createDefaultBackendClient(): HttpClient = createPlatformHttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 20_000
                requestTimeoutMillis = 60_000
            }
            expectSuccess = false
        }
    }
}
