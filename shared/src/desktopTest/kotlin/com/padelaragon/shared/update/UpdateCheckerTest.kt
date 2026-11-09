package com.padelaragon.shared.update

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateCheckerTest {
    @Test
    fun semverComparison() {
        assertTrue(Semver.isNewer("v3.0.1", "3.0.0"))
        assertTrue(Semver.isNewer("3.1.0", "3.0.9"))
        assertTrue(Semver.isNewer("10.0.0", "9.9.9"))
        assertTrue(Semver.isNewer("3.0.0.1", "3.0.0"))
        assertFalse(Semver.isNewer("3.0.0", "3.0.0"))
        assertFalse(Semver.isNewer("v2.9.9", "3.0.0"))
        assertFalse(Semver.isNewer("latest", "3.0.0"))
        assertFalse(Semver.isNewer("3.0.0", "garbage"))
        assertFalse(Semver.isNewer("3.0", "3.0.0"))
    }

    @Test
    fun parseReleaseSkipsPrereleasesAndBadTags() {
        val ok = """{"tag_name":"v3.1.0","html_url":"https://github.com/x/y/releases/tag/v3.1.0"}"""
        assertEquals(UpdateInfo("3.1.0", "https://github.com/x/y/releases/tag/v3.1.0"), UpdateChecker.parseRelease(ok))
        assertNull(UpdateChecker.parseRelease("""{"tag_name":"v3.1.0","html_url":"https://a","prerelease":true}"""))
        assertNull(UpdateChecker.parseRelease("""{"tag_name":"nightly","html_url":"https://a"}"""))
        assertNull(UpdateChecker.parseRelease("""{"tag_name":"v3.1.0","html_url":"http://a"}"""))
    }

    @Test
    fun checksOncePerDayAndIsSilentOnFailure() = runTest {
        var calls = 0
        var now = 10 * UpdateChecker.CHECK_INTERVAL_MS
        var status = HttpStatusCode.OK
        val checker = UpdateChecker(
            settings = MapSettings(),
            currentVersion = "3.0.0",
            clientFactory = {
                HttpClient(MockEngine {
                    calls++
                    respond("""{"tag_name":"v3.2.0","html_url":"https://github.com/r"}""", status)
                })
            },
            nowMs = { now }
        )
        assertEquals("3.2.0", checker.checkIfDue()?.version)
        assertEquals("3.2.0", checker.checkIfDue()?.version)
        assertEquals(1, calls)
        now += UpdateChecker.CHECK_INTERVAL_MS
        status = HttpStatusCode.Forbidden
        assertEquals("3.2.0", checker.checkIfDue()?.version)
        assertEquals(2, calls)
    }

    @Test
    fun failureWithoutCacheShowsNothing() = runTest {
        val checker = UpdateChecker(
            settings = MapSettings(),
            currentVersion = "3.0.0",
            clientFactory = { HttpClient(MockEngine { throw java.io.IOException("offline") }) }
        )
        assertNull(checker.checkIfDue())
    }
}
