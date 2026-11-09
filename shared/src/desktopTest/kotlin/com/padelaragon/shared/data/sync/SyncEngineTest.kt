package com.padelaragon.shared.data.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.padelaragon.shared.data.local.AppDatabase
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.remote.BackendApi
import com.padelaragon.shared.data.remote.dto.BundleJson
import com.padelaragon.shared.data.remote.dto.LeagueBundleDto
import com.padelaragon.shared.data.remote.dto.ManifestDto
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncEngineTest {
    private val league = League.ABSOLUTA
    private val generatedAt = "2026-11-07T12:00:00Z"
    private val baseNow = 1_794_052_800_000L + 30 * 60_000L // 12:30Z, 30 min after generatedAt

    private lateinit var db: AppDatabase
    private lateinit var settings: SyncSettings
    private lateinit var engine: SyncEngine
    private val requests = mutableListOf<HttpRequestData>()
    private val scraped = mutableListOf<League>()
    private var now = baseNow
    private var manifestStatus = HttpStatusCode.OK
    private var bundleStatus = HttpStatusCode.OK
    private var hash = "h1"
    private var groupName = "Original"
    private var scrapeFails = false

    private fun fixture(name: String): String =
        File(System.getProperty("contracts.dir") ?: "../contracts/fixtures", name).readText()

    private fun bundleJson(): String {
        val bundle = BundleJson.decodeFromString(LeagueBundleDto.serializer(), fixture("league-bundle.sample.json"))
        val renamed = bundle.copy(
            groups = bundle.groups.map { it.copy(group = it.group.copy(name = groupName)) }
        )
        return BundleJson.encodeToString(LeagueBundleDto.serializer(), renamed)
    }

    private fun manifestJson(): String {
        val m = BundleJson.decodeFromString(ManifestDto.serializer(), fixture("manifest.sample.json"))
        return BundleJson.encodeToString(
            ManifestDto.serializer(),
            m.copy(generatedAt = generatedAt, leagues = m.leagues.map { it.copy(hash = if (it.id == league.id) hash else it.hash) })
        )
    }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        settings = SyncSettings(MapSettings(), defaultUrl = "https://api.example.invalid", defaultKey = "k")
        val mock = MockEngine { request ->
            requests += request
            val path = request.url.encodedPath
            val etagHeader = headersOf(HttpHeaders.ETag, "\"$hash\"")
            fun conditional(status: HttpStatusCode, body: () -> String) = when {
                status != HttpStatusCode.OK -> respond("", status)
                request.headers[HttpHeaders.IfNoneMatch] == "\"$hash\"" -> respond("", HttpStatusCode.NotModified)
                else -> respond(body(), HttpStatusCode.OK, etagHeader)
            }
            if (path.endsWith("/manifest")) conditional(manifestStatus, ::manifestJson)
            else conditional(bundleStatus, ::bundleJson)
        }
        engine = SyncEngine(
            api = BackendApi(HttpClient(mock)),
            importer = BundleImporter(db),
            settings = settings,
            scrapeLeague = { l ->
                if (scrapeFails) error("offline")
                scraped += l
            },
            nowMs = { now }
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun groupNames() = db.leagueGroupDao().getByLeagueId(league.id).map { it.name }
    private fun bundleRequests() = requests.count { it.url.encodedPath.endsWith("/bundle") }

    @Test
    fun `200 imports the bundle into Room`() = runBlocking {
        val result = engine.sync(league)

        assertEquals(SyncKind.BACKEND_UPDATED, result.kind)
        assertEquals(listOf("Original"), groupNames())
        assertTrue(db.standingRowDao().getByGroupId(league.id, 90001).isNotEmpty())
        assertTrue(db.matchResultDao().getByGroupId(league.id, 90001).isNotEmpty())
        assertTrue(requests.all { it.headers["X-App-Key"] == "k" })
        assertTrue(scraped.isEmpty())
        assertFalse(engine.backgroundScrapingAllowed())
    }

    @Test
    fun `unchanged manifest is a no-op`() = runBlocking {
        engine.sync(league)
        val before = requests.size

        val result = engine.sync(league)

        assertEquals(SyncKind.BACKEND_UNCHANGED, result.kind)
        assertEquals(before + 1, requests.size)
        assertEquals(1, bundleRequests())
    }

    @Test
    fun `changed hash downloads and replaces the bundle`() = runBlocking {
        engine.sync(league)
        hash = "h2"
        groupName = "Renombrado"

        val result = engine.sync(league)

        assertEquals(SyncKind.BACKEND_UPDATED, result.kind)
        assertEquals(listOf("Renombrado"), groupNames())
        assertEquals(2, bundleRequests())
    }

    @Test
    fun `backend failure falls back to scraping in auto mode`() = runBlocking {
        manifestStatus = HttpStatusCode.InternalServerError

        val result = engine.sync(league)

        assertEquals(SyncKind.SCRAPED, result.kind)
        assertEquals(listOf(league), scraped)
        assertTrue(engine.backgroundScrapingAllowed())
    }

    @Test
    fun `backend-only mode reports failure instead of scraping`() = runBlocking {
        settings.mode = DataSourceMode.BACKEND
        bundleStatus = HttpStatusCode.BadGateway

        val result = engine.sync(league)

        assertEquals(SyncKind.FAILED, result.kind)
        assertTrue(scraped.isEmpty())
    }

    @Test
    fun `missing configuration or device mode never touches the network`() = runBlocking {
        val unconfigured = SyncSettings(MapSettings(), defaultUrl = "", defaultKey = "")
        val offlineEngine = SyncEngine(
            BackendApi(HttpClient(MockEngine { error("no requests expected") })),
            BundleImporter(db), unconfigured, { scraped += it }, { now }
        )
        assertEquals(SyncKind.SCRAPED, offlineEngine.sync(league).kind)

        settings.mode = DataSourceMode.DEVICE
        assertEquals(SyncKind.SCRAPED, engine.sync(league).kind)

        assertEquals(2, scraped.size)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `stale backend data triggers scraping in auto mode`() = runBlocking {
        now = baseNow + 3 * 60 * 60_000L

        val result = engine.sync(league)

        assertEquals(SyncKind.SCRAPED, result.kind)
        assertEquals(listOf(league), scraped)
        assertTrue(engine.backgroundScrapingAllowed())
    }

    @Test
    fun `failing fallback is reported, not thrown`() = runBlocking {
        manifestStatus = HttpStatusCode.ServiceUnavailable
        scrapeFails = true

        assertEquals(SyncKind.FAILED, engine.sync(league).kind)
    }
}
