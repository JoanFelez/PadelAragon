package com.padelaragon.scraper

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class BackendException(message: String) : RuntimeException(message)

/** Minimal Worker client (JDK HttpClient). Never logs bodies or credentials. */
class BackendClient(
    private val baseUrl: String,
    private val appKey: String? = null,
    private val scraperToken: String? = null,
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
) {
    /** Returns the body bytes, or null on 404. Other failures throw. */
    fun get(path: String): ByteArray? {
        val request = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(30))
            .apply { appKey?.let { header("X-App-Key", it) } }
            .GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
        return when (response.statusCode()) {
            200 -> response.body()
            404 -> null
            else -> throw BackendException("GET $path -> HTTP ${response.statusCode()}")
        }
    }

    fun put(path: String, bytes: ByteArray, sha256: String) {
        var lastError = ""
        repeat(3) { attempt ->
            try {
                val request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer $scraperToken")
                    .header("Content-Type", "application/json")
                    .header("X-Content-Sha256", sha256)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes)).build()
                val status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
                if (status in 200..299) return
                lastError = "HTTP $status"
                if (status in 400..499 && status != 429) throw BackendException("PUT $path -> $lastError")
            } catch (e: BackendException) {
                throw e
            } catch (e: java.io.IOException) {
                lastError = e::class.simpleName.orEmpty()
            }
            Thread.sleep(1000L * (attempt + 1))
        }
        throw BackendException("PUT $path failed: $lastError")
    }
}
