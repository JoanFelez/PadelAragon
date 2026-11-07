package com.padelaragon.shared.data.remote

import com.padelaragon.shared.data.sync.BackendConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BackendApiTest {
    private val config = BackendConfig("https://api.example.invalid/", "secret")
    private val requests = mutableListOf<HttpRequestData>()

    private fun fixture(name: String): String =
        File(System.getProperty("contracts.dir") ?: "../contracts/fixtures", name).readText()

    private fun api(handler: MockRequestHandler) =
        BackendApi(HttpClient(MockEngine { request -> requests += request; handler(request) }))

    private val jsonHeaders get() = headersOf(HttpHeaders.ContentType to listOf("application/json"), HttpHeaders.ETag to listOf("\"abc\""))

    @Test
    fun `manifest 200 sends the app key and returns the etag`() = runBlocking {
        val api = api { respond(fixture("manifest.sample.json"), HttpStatusCode.OK, jsonHeaders) }

        val result = api.manifest(config.copy(baseUrl = "https://api.example.invalid"))

        val fetched = assertIs<Conditional.Fetched<*>>(result)
        assertEquals("\"abc\"", fetched.etag)
        assertEquals("https://api.example.invalid/api/manifest", requests.single().url.toString())
        assertEquals("secret", requests.single().headers["X-App-Key"])
        assertEquals(null, requests.single().headers[HttpHeaders.IfNoneMatch])
    }

    @Test
    fun `304 maps to NotModified and forwards If-None-Match`() = runBlocking {
        val api = api { respond("", HttpStatusCode.NotModified) }

        val result = api.bundle(config, 27951, etag = "\"abc\"")

        assertEquals(Conditional.NotModified, result)
        assertEquals("\"abc\"", requests.single().headers[HttpHeaders.IfNoneMatch])
        assertTrue(requests.single().url.toString().endsWith("/api/leagues/27951/bundle"))
    }

    @Test
    fun `leagues and bundle decode contract fixtures`() = runBlocking {
        val api = api { request ->
            val body = if (request.url.encodedPath.endsWith("/bundle")) fixture("league-bundle.sample.json")
            else fixture("leagues.sample.json")
            respond(body, HttpStatusCode.OK, jsonHeaders)
        }

        val leagues = assertIs<Conditional.Fetched<List<com.padelaragon.shared.data.remote.dto.LeagueDto>>>(api.leagues(config))
        val bundle = assertIs<Conditional.Fetched<com.padelaragon.shared.data.remote.dto.LeagueBundleDto>>(api.bundle(config, 27951))

        assertEquals(3, leagues.value.size)
        assertEquals(27951, bundle.value.league.id)
    }

    @Test
    fun `server errors, bad bodies and missing config throw BackendException`() = runBlocking {
        assertFailsWith<BackendException> {
            api { respondError(HttpStatusCode.ServiceUnavailable) }.manifest(config)
        }
        assertFailsWith<BackendException> {
            api { respond("not json", HttpStatusCode.OK, jsonHeaders) }.manifest(config)
        }
        assertFailsWith<BackendException> {
            api { respond("{}", HttpStatusCode.OK) }.manifest(BackendConfig("", ""))
        }
        Unit
    }
}
