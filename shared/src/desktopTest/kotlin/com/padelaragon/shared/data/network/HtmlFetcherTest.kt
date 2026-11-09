package com.padelaragon.shared.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class HtmlFetcherTest {

    private fun fetcher(engine: MockEngine, attempts: Int = 3) =
        HtmlFetcher(HttpClient(engine), maxAttempts = attempts, baseBackoffMs = 1, random = Random(1))

    @Test
    fun `retries 5xx then succeeds`() = runBlocking {
        val calls = AtomicInteger()
        val engine = MockEngine {
            if (calls.incrementAndGet() < 3) respond("", HttpStatusCode.ServiceUnavailable)
            else respond("ok".toByteArray(), HttpStatusCode.OK)
        }

        assertEquals("ok", fetcher(engine).get("https://x.test/a"))
        assertEquals(3, calls.get())
    }

    @Test
    fun `retries IO errors and gives up after max attempts`() = runBlocking {
        val calls = AtomicInteger()
        val engine = MockEngine { calls.incrementAndGet(); throw IOException("boom") }

        try {
            fetcher(engine).get("https://x.test/a")
            fail("expected IOException")
        } catch (_: IOException) {
        }
        assertEquals(3, calls.get())
    }

    @Test
    fun `does not retry 4xx`() = runBlocking {
        val calls = AtomicInteger()
        val engine = MockEngine { calls.incrementAndGet(); respond("", HttpStatusCode.NotFound) }

        try {
            fetcher(engine).get("https://x.test/a")
            fail("expected HttpStatusException")
        } catch (e: HttpStatusException) {
            assertEquals(404, e.statusCode)
        }
        assertEquals(1, calls.get())
    }

    @Test
    fun `backoff grows exponentially with bounded jitter`() {
        val f = HtmlFetcher(HttpClient(MockEngine { respond("") }), baseBackoffMs = 100, random = Random(7))
        for (attempt in 1..3) {
            val base = 100L * (1L shl (attempt - 1))
            val wait = f.backoffMs(attempt)
            assertTrue("attempt $attempt: $wait", wait in base..(base + 100))
        }
    }

    @Test
    fun `concurrent identical requests share one network call`() = runBlocking {
        val calls = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val engine = MockEngine {
            calls.incrementAndGet()
            gate.await()
            respond("shared".toByteArray(), HttpStatusCode.OK)
        }
        val f = fetcher(engine)

        val results = (1..5).map { async { f.get("https://x.test/same") } }
        while (calls.get() == 0) kotlinx.coroutines.yield()
        gate.complete(Unit)

        assertEquals(List(5) { "shared" }, results.awaitAll())
        assertEquals(1, calls.get())
    }

    @Test
    fun `different urls are fetched separately and later calls are not cached`() = runBlocking {
        val calls = AtomicInteger()
        val f = fetcher(MockEngine { calls.incrementAndGet(); respond("x".toByteArray(), HttpStatusCode.OK) })

        f.get("https://x.test/a")
        f.get("https://x.test/b")
        f.get("https://x.test/a")

        assertEquals(3, calls.get())
    }

    @Test
    fun `decodes latin1 and encodes forms as latin1`() = runBlocking {
        var sentBody = ""
        val engine = MockEngine { request ->
            sentBody = String(request.body.toByteArray(), Charsets.ISO_8859_1)
            respond(byteArrayOf(0xE1.toByte(), 0xF1.toByte()), HttpStatusCode.OK)
        }

        val body = fetcher(engine).post("https://x.test/p", mapOf("q" to "año"))

        assertEquals("áñ", body)
        assertEquals("q=a%F1o", sentBody)
    }
}
