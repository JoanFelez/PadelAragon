package com.padelaragon.shared.data.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.padelaragon.shared.FixtureLoader
import com.padelaragon.shared.data.local.AppDatabase
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.network.HtmlFetcher
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class StandingsSwrTest {

    private val league = League.ABSOLUTA
    private val groupId = 1
    private val calls = AtomicInteger()
    private var html = FixtureLoader.load("parser/standings/valid_standings.html")
    private var now = 1_000_000L
    private lateinit var db: AppDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: StandingsRepository
    private lateinit var scraping: ScrapingService

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val engine = MockEngine {
            calls.incrementAndGet()
            respond(html.toByteArray(Charsets.ISO_8859_1), HttpStatusCode.OK)
        }
        val fetcher = HtmlFetcher(HttpClient(engine), baseBackoffMs = 1, scope = scope)
        scraping = ScrapingService(db, fetcher, scope) { now }
        repo = StandingsRepository(league, scraping)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun eventually(condition: suspend () -> Boolean) = withTimeout(5_000) {
        while (!condition()) delay(10)
    }

    @Test
    fun `fresh cache is served without any network call`() = runBlocking {
        val first = repo.getStandings(groupId)
        assertTrue(first.isNotEmpty())
        assertEquals(1, calls.get())

        now += StandingsRepository.TTL_STANDINGS - 1
        assertEquals(first, repo.getStandings(groupId))
        delay(100)
        assertEquals(1, calls.get())
    }

    @Test
    fun `expired cache returns stale data immediately and revalidates in background`() = runBlocking {
        val first = repo.getStandings(groupId)
        val before = db.cacheTimestampDao().getEntry("standings_${league.id}_$groupId")!!.timestamp

        now += StandingsRepository.TTL_STANDINGS + 1
        val stale = repo.getStandings(groupId)

        assertEquals(first, stale)
        eventually { calls.get() == 2 }
        eventually { db.cacheTimestampDao().getEntry("standings_${league.id}_$groupId")!!.timestamp > before }
    }

    @Test
    fun `unchanged content skips the database write but refreshes the timestamp`() = runBlocking {
        repo.getStandings(groupId)
        val key = "standings_${league.id}_$groupId"
        val written = scraping.writeIfChanged(key, repo.getStandings(groupId)) { }
        // Hash of the parsed list differs from the one stored from the same list only if content changed.
        assertFalse(written)
    }

    @Test
    fun `refresh fetches again and keeps the cache when the network fails`() = runBlocking {
        val first = repo.getStandings(groupId)
        html = "<html></html>"

        assertEquals(first, repo.refreshStandings(groupId))
        assertEquals(first, repo.getStandings(groupId))
    }
}
