package com.padelaragon.scraper

import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.remote.dto.BundleJson
import com.padelaragon.shared.data.remote.dto.ManifestDto
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PublisherTest {
    private val leagues = League.entries.toList()
    private val selected = listOf(League.ABSOLUTA)
    private val bundle = Fixtures.sampleBundle()
    private val t0 = "2026-11-07T12:00:00Z"
    private val t1 = "2026-11-07T14:00:00Z"

    private fun run(
        scraped: Map<Int, com.padelaragon.shared.data.remote.dto.LeagueBundleDto>,
        previous: PreviousState,
        now: String
    ) = assemble(selected, scraped, leagues, previous, now)

    private fun stateFrom(artifacts: List<Artifact>) = PreviousState(
        manifestBytes = artifacts.first { it.fileName == "manifest.json" }.bytes,
        leaguesBytes = artifacts.first { it.fileName == "leagues.json" }.bytes,
        bundleBytes = artifacts.filter { it.fileName.first().isDigit() }.associate { it.fileName.removeSuffix(".json").toInt() to it.bytes }
    )

    @Test
    fun firstRunUploadsEverythingInPublishOrder() {
        val artifacts = run(mapOf(27951 to bundle), PreviousState(), t0)
        assertEquals(listOf("27951.json", "leagues.json", "manifest.json"), artifacts.map { it.fileName })
        assertTrue(artifacts.all { it.changed })
        assertEquals(artifacts.size, uploadsFor(artifacts).size)
        artifacts.forEach { assertEquals(sha256Hex(it.bytes), it.hash) }
    }

    @Test
    fun identicalContentKeepsExactBytesAndUploadsNothing() {
        val first = run(mapOf(27951 to bundle), PreviousState(), t0)
        val second = run(mapOf(27951 to bundle.copy(generatedAt = t1)), stateFrom(first), t1)
        assertTrue(uploadsFor(second).isEmpty())
        first.zip(second).forEach { (a, b) -> assertContentEquals(a.bytes, b.bytes) }
    }

    @Test
    fun changedLeagueUploadsBundleAndManifestOnly() {
        val first = run(mapOf(27951 to bundle), PreviousState(), t0)
        val changedBundle = bundle.copy(groups = bundle.groups.map { it.copy(jornadas = it.jornadas + 99) })
        val second = run(mapOf(27951 to changedBundle), stateFrom(first), t1)
        assertEquals(listOf("27951.json", "manifest.json"), uploadsFor(second).map { it.fileName })
        val manifest = BundleJson.decodeFromString(ManifestDto.serializer(), second.last().bytes.decodeToString())
        assertEquals(t1, manifest.generatedAt)
        assertEquals(t1, manifest.leagues.single().updatedAt)
    }

    @Test
    fun serializationIsDeterministic() {
        val a = run(mapOf(27951 to bundle), PreviousState(), t0)
        val b = run(mapOf(27951 to bundle), PreviousState(), t0)
        a.zip(b).forEach { (x, y) -> assertEquals(x.hash, y.hash) }
    }

    @Test
    fun failedLeagueKeepsPreviousBundleAndManifestEntry() {
        val first = run(mapOf(27951 to bundle), PreviousState(), t0)
        val second = run(emptyMap(), stateFrom(first), t1)
        assertTrue(uploadsFor(second).isEmpty())
        assertContentEquals(first.first().bytes, second.first().bytes)
        assertFalse(second.any { it.fileName == "28060.json" })
    }
}
