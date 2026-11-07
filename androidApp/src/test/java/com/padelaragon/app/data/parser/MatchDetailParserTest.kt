package com.padelaragon.app.data.parser

import com.padelaragon.app.FixtureLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local JVM tests for [MatchDetailParser] using saved HTML fixtures from
 * `test/resources/fixtures/parser/match-detail/`. No network access or
 * Android emulator required.
 */
class MatchDetailParserTest {

    private val parser = MatchDetailParser()

    // ── Happy-path: pair and set extraction ─────────────────────

    @Test
    fun `parse extracts two pairs from valid fixture`() {
        val html = loadFixture("valid_two_pairs.html")
        val detail = parser.parse(html)

        assertEquals("Expected 2 pairs", 2, detail.pairs.size)
    }

    @Test
    fun `parse assigns sequential pair numbers`() {
        val html = loadFixture("valid_two_pairs.html")
        val detail = parser.parse(html)

        assertEquals(1, detail.pairs[0].pairNumber)
        assertEquals(2, detail.pairs[1].pairNumber)
    }

    @Test
    fun `parse extracts local player names from first pair`() {
        val html = loadFixture("valid_two_pairs.html")
        val pair1 = parser.parse(html).pairs[0]

        assertEquals("GARCÍA LÓPEZ, Juan", pair1.localPlayer1)
        assertEquals("MARTÍNEZ RUIZ, Pedro", pair1.localPlayer2)
    }

    @Test
    fun `parse extracts visitor player names from first pair`() {
        val html = loadFixture("valid_two_pairs.html")
        val pair1 = parser.parse(html).pairs[0]

        assertEquals("FERNÁNDEZ GIL, Carlos", pair1.visitorPlayer1)
        assertEquals("SÁNCHEZ DÍAZ, Luis", pair1.visitorPlayer2)
    }

    @Test
    fun `parse extracts set scores for three-set pair`() {
        val html = loadFixture("valid_two_pairs.html")
        val pair1 = parser.parse(html).pairs[0]

        assertEquals("Expected 3 sets", 3, pair1.sets.size)
        assertEquals(6, pair1.sets[0].localScore)
        assertEquals(3, pair1.sets[0].visitorScore)
        assertEquals(4, pair1.sets[1].localScore)
        assertEquals(6, pair1.sets[1].visitorScore)
        assertEquals(7, pair1.sets[2].localScore)
        assertEquals(5, pair1.sets[2].visitorScore)
    }

    @Test
    fun `parse extracts set scores for two-set pair`() {
        val html = loadFixture("valid_two_pairs.html")
        val pair2 = parser.parse(html).pairs[1]

        assertEquals("Expected 2 sets for pair 2", 2, pair2.sets.size)
        assertEquals(6, pair2.sets[0].localScore)
        assertEquals(2, pair2.sets[0].visitorScore)
        assertEquals(6, pair2.sets[1].localScore)
        assertEquals(4, pair2.sets[1].visitorScore)
    }

    // ── Three-pair match ────────────────────────────────────────

    @Test
    fun `parse extracts three pairs from fixture`() {
        val html = loadFixture("three_pairs.html")
        val detail = parser.parse(html)

        assertEquals("Expected 3 pairs", 3, detail.pairs.size)
        assertEquals(3, detail.pairs[2].pairNumber)
        assertEquals("LOCAL E", detail.pairs[2].localPlayer1)
        assertEquals("VISITOR E", detail.pairs[2].visitorPlayer1)
    }

    // ── Fallback: single player name (no separator) ─────────────

    @Test
    fun `parse handles single player name with no dash separator`() {
        val html = loadFixture("single_player_per_side.html")
        val detail = parser.parse(html)

        assertEquals(1, detail.pairs.size)
        val pair = detail.pairs[0]
        // When only one name (no " - " separator), first part is the full text, second is empty
        assertEquals("SOLO JUGADOR", pair.localPlayer1)
        assertEquals("", pair.localPlayer2)
    }

    // ── Malformed: non-numeric scores ───────────────────────────

    @Test
    fun `parse skips non-numeric set scores`() {
        val html = loadFixture("non_numeric_scores.html")
        val detail = parser.parse(html)

        assertEquals(1, detail.pairs.size)
        assertTrue("Non-numeric scores should be skipped", detail.pairs[0].sets.isEmpty())
    }

    // ── Empty / missing ─────────────────────────────────────────

    @Test
    fun `parse returns empty pairs when no detail table exists`() {
        val html = loadFixture("no_match_detail.html")
        val detail = parser.parse(html)

        assertTrue("Should return empty pairs for missing table", detail.pairs.isEmpty())
    }

    @Test
    fun `parse returns empty pairs for blank HTML`() {
        val detail = parser.parse("")

        assertTrue("Blank input should yield empty pairs", detail.pairs.isEmpty())
    }

    // ── Fixture helper ──────────────────────────────────────────

    private fun loadFixture(name: String): String =
        FixtureLoader.load("parser/match-detail/$name")
}
