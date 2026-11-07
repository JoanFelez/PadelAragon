package com.padelaragon.app.data.parser

import com.padelaragon.app.FixtureLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local JVM tests for [StandingsParser] using saved HTML fixtures from
 * `test/resources/fixtures/parser/standings/`. No network access or
 * Android emulator required.
 */
class StandingsParserTest {

    private val parser = StandingsParser()

    // ── Happy-path: full standings extraction ───────────────────

    @Test
    fun `parse extracts all standing rows from valid fixture`() {
        val html = loadFixture("valid_standings.html")
        val rows = parser.parse(html)

        assertEquals("Expected 3 standing rows", 3, rows.size)
    }

    @Test
    fun `parse extracts positions correctly`() {
        val html = loadFixture("valid_standings.html")
        val rows = parser.parse(html)

        assertEquals(1, rows[0].position)
        assertEquals(2, rows[1].position)
        assertEquals(3, rows[2].position)
    }

    @Test
    fun `parse extracts team names and IDs from links`() {
        val html = loadFixture("valid_standings.html")
        val rows = parser.parse(html)

        assertEquals("Club Pádel Zaragoza A", rows[0].teamName)
        assertEquals(5001, rows[0].teamId)

        assertEquals("CT Monzón", rows[1].teamName)
        assertEquals(5002, rows[1].teamId)
    }

    @Test
    fun `parse extracts team href`() {
        val html = loadFixture("valid_standings.html")
        val rows = parser.parse(html)

        assertEquals("equipo.php?IdEquipo=5001", rows[0].teamHref)
    }

    @Test
    fun `parse extracts all statistical columns`() {
        val html = loadFixture("valid_standings.html")
        val first = parser.parse(html).first()

        assertEquals(5, first.matchesPlayed)
        assertEquals(15, first.points)
        assertEquals(5, first.encountersWon)
        assertEquals(0, first.encountersLost)
        assertEquals(14, first.matchesWon)
        assertEquals(1, first.matchesLost)
        assertEquals(28, first.setsWon)
        assertEquals(5, first.setsLost)
        assertEquals(190, first.gamesWon)
        assertEquals(120, first.gamesLost)
    }

    // ── Malformed / edge-case HTML ──────────────────────────────

    @Test
    fun `parse returns empty list when no standings table exists`() {
        val html = loadFixture("no_standings_table.html")
        val rows = parser.parse(html)

        assertTrue("Should return empty for missing table", rows.isEmpty())
    }

    @Test
    fun `parse skips rows with fewer than 12 cells`() {
        val html = loadFixture("partial_row.html")
        val rows = parser.parse(html)

        assertTrue("Rows with too few cells should be skipped", rows.isEmpty())
    }

    @Test
    fun `parse skips rows where team link is missing`() {
        val html = loadFixture("missing_team_link.html")
        val rows = parser.parse(html)

        // No <a> tag means no extractable teamId → row is dropped
        assertTrue("Row without team link should be skipped", rows.isEmpty())
    }

    @Test
    fun `parse returns empty list for blank HTML`() {
        val rows = parser.parse("")

        assertTrue("Blank input should yield empty list", rows.isEmpty())
    }

    // ── Fixture helper ──────────────────────────────────────────

    private fun loadFixture(name: String): String =
        FixtureLoader.load("parser/standings/$name")
}
