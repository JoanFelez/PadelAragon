package com.padelaragon.app.data.parser

import com.padelaragon.app.FixtureLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local JVM tests for [TeamDetailParser] using saved HTML fixtures from
 * `test/resources/fixtures/parser/team-detail/`. No network access or
 * Android emulator required.
 */
class TeamDetailParserTest {

    private val parser = TeamDetailParser()

    // ── Happy-path: full team extraction ────────────────────────

    @Test
    fun `parse extracts category from ranking div`() {
        val html = loadFixture("valid_team.html")
        val team = parser.parse(html)

        assertEquals("1ª CATEGORÍA MASCULINA - GRUPO A", team.category)
    }

    @Test
    fun `parse extracts captain name from metadata`() {
        val html = loadFixture("valid_team.html")
        val team = parser.parse(html)

        assertEquals("GARCÍA LÓPEZ, Juan", team.captainName)
    }

    @Test
    fun `parse extracts all players from LineasRnk rows`() {
        val html = loadFixture("valid_team.html")
        val team = parser.parse(html)

        assertEquals("Expected 4 players", 4, team.players.size)
    }

    @Test
    fun `parse builds player names from nombre and apellido columns`() {
        val html = loadFixture("valid_team.html")
        val players = parser.parse(html).players

        assertEquals("Juan García López", players[0].name)
        assertEquals("Pedro Martínez Ruiz", players[1].name)
    }

    @Test
    fun `parse extracts points from player rows`() {
        val html = loadFixture("valid_team.html")
        val players = parser.parse(html).players

        assertEquals("1200", players[0].points)
        assertEquals("1050", players[3].points)
    }

    @Test
    fun `parse detects captain via Si text marker`() {
        val html = loadFixture("valid_team.html")
        val players = parser.parse(html).players

        assertTrue("First player should be captain", players[0].isCaptain)
        assertTrue("Other players should not be captain", players.drop(1).none { it.isCaptain })
    }

    @Test
    fun `captain property returns captain player`() {
        val html = loadFixture("valid_team.html")
        val team = parser.parse(html)

        assertNotNull("Captain should be resolved", team.captain)
        assertEquals("Juan García López", team.captain!!.name)
    }

    // ── Captain from metadata (no C column) ─────────────────────

    @Test
    fun `parse applies captain from metadata when no captain column exists`() {
        val html = loadFixture("captain_from_metadata.html")
        val team = parser.parse(html)

        assertEquals("Marta Romero", team.captainName)
        assertEquals("2ª CATEGORÍA FEMENINA - GRUPO B", team.category)

        // applyCaptainFromMetadata should match "Marta Romero Vidal" to captainName
        val captain = team.captain
        assertNotNull("Captain should be resolved from metadata", captain)
        assertTrue(
            "Captain name should match metadata",
            captain!!.name.contains("Romero") || captain.name.contains("Marta")
        )
    }

    // ── Captain detected by image ───────────────────────────────

    @Test
    fun `parse detects captain via image element in captain column`() {
        val html = loadFixture("captain_by_image.html")
        val team = parser.parse(html)

        val captain = team.players.firstOrNull { it.isCaptain }
        assertNotNull("Captain should be detected from img tag", captain)
        assertTrue(
            "Captain should be Pablo Ruiz Ortega",
            captain!!.name.contains("Pablo")
        )
    }

    // ── Generic table fallback (full-name column) ───────────────

    @Test
    fun `parse falls back to generic table with Jugador column`() {
        val html = loadFixture("generic_table_fullname.html")
        val team = parser.parse(html)

        assertEquals("Expected 3 players from generic table", 3, team.players.size)
        assertEquals("Ana Pérez García", team.players[0].name)
    }

    @Test
    fun `parse extracts birth year from generic table`() {
        val html = loadFixture("generic_table_fullname.html")
        val team = parser.parse(html)

        assertEquals("1990", team.players[0].birthYear)
        assertEquals("1988", team.players[2].birthYear)
    }

    @Test
    fun `parse extracts points from generic table`() {
        val html = loadFixture("generic_table_fullname.html")
        val team = parser.parse(html)

        assertEquals("950", team.players[0].points)
    }

    // ── Empty rows are skipped ──────────────────────────────────

    @Test
    fun `parse skips empty rows in player table`() {
        val html = loadFixture("empty_rows_mixed.html")
        val team = parser.parse(html)

        assertEquals("Only non-empty rows should yield players", 1, team.players.size)
        assertEquals("Solo Jugador Único", team.players[0].name)
    }

    // ── Edge cases: blank and missing data ──────────────────────

    @Test
    fun `parse returns empty TeamDetail for blank HTML`() {
        val team = parser.parse("   ")

        assertTrue("Should return empty players for blank input", team.players.isEmpty())
        assertNull("Category should be null for blank input", team.category)
        assertNull("Captain should be null for blank input", team.captainName)
    }

    @Test
    fun `parse returns empty players when no table exists`() {
        val html = loadFixture("no_team_data.html")
        val team = parser.parse(html)

        assertTrue("Should return empty players for missing table", team.players.isEmpty())
    }

    @Test
    fun `parse returns no category when ranking div is absent`() {
        val html = loadFixture("generic_table_fullname.html")
        val team = parser.parse(html)

        // No div.ranking → no metadata category (fallback may fire but no matching text)
        // The generic table fixture has no ranking div
        assertNull("Category should be null without ranking div", team.category)
    }

    // ── Fixture helper ──────────────────────────────────────────

    private fun loadFixture(name: String): String =
        FixtureLoader.load("parser/team-detail/$name")
}
