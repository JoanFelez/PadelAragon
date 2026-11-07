package com.padelaragon.app.data.parser

import com.padelaragon.app.FixtureLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local JVM tests for [MatchResultParser] using saved HTML fixtures from
 * `test/resources/fixtures/parser/results/`. No network access or
 * Android emulator required.
 */
class MatchResultParserTest {

    private val parser = MatchResultParser()
    private val testJornada = 1

    // ── Happy-path: result extraction ───────────────────────────

    @Test
    fun `parse extracts all match results from valid fixture`() {
        val html = loadFixture("valid_results.html")
        val results = parser.parse(html, testJornada)

        assertEquals("Expected 2 match results", 2, results.size)
    }

    @Test
    fun `parse extracts team names and IDs from links`() {
        val html = loadFixture("valid_results.html")
        val first = parser.parse(html, testJornada).first()

        assertEquals("Club Pádel Zaragoza A", first.localTeam)
        assertEquals(5001, first.localTeamId)
        assertEquals("CT Monzón", first.visitorTeam)
        assertEquals(5002, first.visitorTeamId)
    }

    @Test
    fun `parse extracts scores correctly`() {
        val html = loadFixture("valid_results.html")
        val first = parser.parse(html, testJornada).first()

        assertEquals("3", first.localScore)
        assertEquals("0", first.visitorScore)
    }

    @Test
    fun `parse extracts date and venue from combined cell`() {
        val html = loadFixture("valid_results.html")
        val first = parser.parse(html, testJornada).first()

        assertEquals("15/03/2025", first.date)
        assertEquals("Pabellón Municipal", first.venue)
    }

    @Test
    fun `parse extracts detail URL when VerDetalle image exists`() {
        val html = loadFixture("valid_results.html")
        val first = parser.parse(html, testJornada).first()

        assertNotNull("Detail URL should be extracted", first.detailUrl)
        assertTrue(
            "Detail URL should reference the result detail page",
            first.detailUrl!!.contains("detalle_resultado")
        )
    }

    @Test
    fun `parse attaches the supplied jornada value to each result`() {
        val html = loadFixture("valid_results.html")
        val results = parser.parse(html, 7)

        assertTrue("All results should carry the supplied jornada", results.all { it.jornada == 7 })
    }

    // ── Bye / rest team ─────────────────────────────────────────

    @Test
    fun `parse identifies bye team with special ID`() {
        val html = loadFixture("bye_team.html")
        val results = parser.parse(html, testJornada)

        assertEquals(1, results.size)
        val match = results.first()
        assertEquals(-2, match.localTeamId)  // BYE_TEAM_ID
        assertTrue(match.localTeam.contains("descansa", ignoreCase = true))
    }

    // ── Missing / blank scores ──────────────────────────────────

    @Test
    fun `parse normalizes blank scores to double-dash`() {
        val html = loadFixture("missing_scores.html")
        val results = parser.parse(html, testJornada)

        assertEquals(1, results.size)
        assertEquals("--", results.first().localScore)
        assertEquals("--", results.first().visitorScore)
    }

    @Test
    fun `parse returns null date and venue when cell is empty`() {
        val html = loadFixture("missing_scores.html")
        val results = parser.parse(html, testJornada)

        assertNull("Date should be null for empty cell", results.first().date)
        assertNull("Venue should be null for empty cell", results.first().venue)
    }

    // ── Malformed / edge-case HTML ──────────────────────────────

    @Test
    fun `parse returns empty list when no results table exists`() {
        val html = loadFixture("no_results_table.html")
        val results = parser.parse(html, testJornada)

        assertTrue("Should return empty for missing table", results.isEmpty())
    }

    @Test
    fun `parse returns empty list for blank HTML`() {
        val results = parser.parse("", testJornada)

        assertTrue("Blank input should yield empty list", results.isEmpty())
    }

    // ── Fixture helper ──────────────────────────────────────────

    private fun loadFixture(name: String): String =
        FixtureLoader.load("parser/results/$name")
}
