package com.padelaragon.shared.data.network

import com.padelaragon.shared.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlin.random.Random

/** Platform HTTP engine (OkHttp on both JVM targets); the desktop actual adds extra trust roots. */
expect fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient

class HttpStatusException(val statusCode: Int, url: String) : IOException("HTTP $statusCode: $url")

/**
 * Polite HTML fetcher for padelfederacion.es: bounded concurrency, retry with exponential
 * backoff + jitter on I/O errors and 5xx, and in-flight de-duplication so concurrent callers
 * asking for the same request share one network call.
 */
class HtmlFetcher(
    private val client: HttpClient = createDefaultHttpClient(),
    private val maxAttempts: Int = DEFAULT_ATTEMPTS,
    private val baseBackoffMs: Long = DEFAULT_BACKOFF_MS,
    maxConcurrent: Int = MAX_CONCURRENT_REQUESTS,
    private val random: Random = Random.Default,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val permits = Semaphore(maxConcurrent)
    private val inFlightLock = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<HtmlResponse>>()

    suspend fun get(url: String): String = requestOk(HttpMethod.Get, url, null).body

    suspend fun getWithStatus(url: String): HtmlResponse = request(HttpMethod.Get, url, null, failOnError = false)

    suspend fun post(url: String, params: Map<String, String>): String =
        requestOk(HttpMethod.Post, url, encodeLatin1Form(params)).body

    /** Establishes the TLS connection ahead of the first real request. Never throws. */
    suspend fun prewarm(url: String) {
        try {
            client.head(url) { applyDefaultHeaders() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun requestOk(method: HttpMethod, url: String, body: String?): HtmlResponse =
        request(method, url, body, failOnError = true)

    private suspend fun request(
        method: HttpMethod,
        url: String,
        body: String?,
        failOnError: Boolean
    ): HtmlResponse {
        val key = "${method.value} $url ${body.orEmpty()} $failOnError"
        val deferred = inFlightLock.withLock {
            inFlight.getOrPut(key) {
                scope.async {
                    try {
                        executeWithRetry(method, url, body, failOnError)
                    } finally {
                        withContext(NonCancellable) { inFlightLock.withLock { inFlight.remove(key) } }
                    }
                }
            }
        }
        return deferred.await()
    }

    private suspend fun executeWithRetry(
        method: HttpMethod,
        url: String,
        body: String?,
        failOnError: Boolean
    ): HtmlResponse {
        var attempt = 1
        while (true) {
            try {
                val response = permits.withPermit { execute(method, url, body) }
                if (failOnError && response.statusCode >= 400) throw HttpStatusException(response.statusCode, url)
                if (response.statusCode >= 500 && attempt < maxAttempts) throw HttpStatusException(response.statusCode, url)
                return response
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt >= maxAttempts || !isRetryable(e)) throw e
                val wait = backoffMs(attempt)
                Logger.w(TAG, "Attempt $attempt failed for $url (${e.message}); retrying in ${wait}ms")
                delay(wait)
                attempt++
            }
        }
    }

    internal fun backoffMs(attempt: Int): Long =
        baseBackoffMs * (1L shl (attempt - 1)) + random.nextLong(baseBackoffMs + 1)

    private suspend fun execute(method: HttpMethod, url: String, body: String?): HtmlResponse {
        Logger.d(TAG, "${method.value}: $url")
        val response: HttpResponse = client.request(url) {
            this.method = method
            applyDefaultHeaders()
            if (body != null) {
                setBody(TextContent(body, ContentType.Application.FormUrlEncoded))
            }
        }
        val text = decodeLatin1(response.readRawBytes())
        Logger.d(TAG, "Received ${text.length} chars (HTTP ${response.status.value}) from $url")
        return HtmlResponse(statusCode = response.status.value, body = text)
    }

    private fun HttpRequestBuilder.applyDefaultHeaders() {
        header(HttpHeaders.UserAgent, MOBILE_USER_AGENT)
        header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    }

    private fun isRetryable(e: Exception): Boolean = when (e) {
        is HttpStatusException -> e.statusCode >= 500 || e.statusCode == 429 || e.statusCode == 408
        is HttpRequestTimeoutException, is IOException -> true
        else -> false
    }

    data class HtmlResponse(
        val statusCode: Int,
        val body: String
    )

    companion object {
        const val MAX_CONCURRENT_REQUESTS = 4
        private const val DEFAULT_ATTEMPTS = 3
        private const val DEFAULT_BACKOFF_MS = 400L
        private const val TAG = "HtmlFetcher"

        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        fun createDefaultHttpClient(): HttpClient = createPlatformHttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
                requestTimeoutMillis = 30_000
            }
            expectSuccess = false
        }

        /** ISO-8859-1 is the site's encoding; each byte maps 1:1 to a code point. */
        internal fun decodeLatin1(bytes: ByteArray): String =
            buildString(bytes.size) { bytes.forEach { append((it.toInt() and 0xFF).toChar()) } }

        /** application/x-www-form-urlencoded with ISO-8859-1 percent-encoding (what the legacy ASP forms expect). */
        internal fun encodeLatin1Form(params: Map<String, String>): String =
            params.entries.joinToString("&") { (k, v) -> "${encodeLatin1(k)}=${encodeLatin1(v)}" }

        private fun encodeLatin1(value: String): String = buildString {
            for (ch in value) {
                when {
                    ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch in "-_.*" -> append(ch)
                    ch == ' ' -> append('+')
                    ch.code < 256 -> {
                        append('%')
                        append(HEX[ch.code shr 4])
                        append(HEX[ch.code and 0xF])
                    }
                    else -> append("%3F")
                }
            }
        }

        private const val HEX = "0123456789ABCDEF"
    }
}
