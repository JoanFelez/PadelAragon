package com.padelaragon.app.ui.viewmodel

import com.padelaragon.app.data.model.MatchDetail
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.model.PairDetail
import com.padelaragon.app.data.model.PlayerStats
import com.padelaragon.app.data.model.SetScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [TeamViewModel.Companion.computePlayerStats] — the pure
 * aggregation logic that computes per-player win/loss/pair statistics
 * from match details.
 */
class TeamViewModelPlayerStatsTest {

    private val ourTeamId = 100

    private fun match(
        jornada: Int, detailUrl: String,
        localTeamId: Int = ourTeamId, visitorTeamId: Int = 200,
        localScore: String = "3", visitorScore: String = "0"
    ) = MatchResult(
        localTeam = "Our Team", localTeamId = localTeamId,
        visitorTeam = "Their Team", visitorTeamId = visitorTeamId,
        localScore = localScore, visitorScore = visitorScore,
        date = null, venue = null, jornada = jornada, detailUrl = detailUrl
    )

    private fun pair(
        pairNumber: Int,
        localP1: String, localP2: String,
        visitorP1: String, visitorP2: String,
        sets: List<SetScore>
    ) = PairDetail(
        pairNumber = pairNumber,
        localPlayer1 = localP1, localPlayer2 = localP2,
        visitorPlayer1 = visitorP1, visitorPlayer2 = visitorP2,
        sets = sets
    )

    // ── Basic aggregation ───────────────────────────────────────

    @Test
    fun `single match as local team - winning pair gets wins`() {
        val matches = listOf(match(1, "/d/1"))
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "Alice", "Bob", "Eve", "Mallory",
                    sets = listOf(SetScore(6, 3), SetScore(6, 4)))
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)

        assertEquals(2, stats.size)
        val alice = stats.first { it.name == "Alice" }
        assertEquals(1, alice.wins)
        assertEquals(0, alice.losses)
        assertEquals(1, alice.pair1Count)
    }

    @Test
    fun `single match as visitor team - losing pair gets losses`() {
        // Our team is the visitor (visitorTeamId = ourTeamId)
        val matches = listOf(match(1, "/d/1", localTeamId = 200, visitorTeamId = ourTeamId))
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "Eve", "Mallory", "Alice", "Bob",
                    sets = listOf(SetScore(6, 3), SetScore(6, 4)))  // local (opponent) wins
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)

        assertEquals(2, stats.size)
        val alice = stats.first { it.name == "Alice" }
        assertEquals(0, alice.wins)
        assertEquals(1, alice.losses)
    }

    @Test
    fun `visitor team winning pair gets wins`() {
        val matches = listOf(match(1, "/d/1", localTeamId = 200, visitorTeamId = ourTeamId))
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "Eve", "Mallory", "Alice", "Bob",
                    sets = listOf(SetScore(3, 6), SetScore(4, 6)))  // visitor (our team) wins
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)
        val alice = stats.first { it.name == "Alice" }
        assertEquals(1, alice.wins)
        assertEquals(0, alice.losses)
    }

    // ── Multiple pairs ──────────────────────────────────────────

    @Test
    fun `pair numbers are tracked correctly`() {
        val matches = listOf(match(1, "/d/1"))
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "Alice", "Bob", "Eve", "Mallory",
                    sets = listOf(SetScore(6, 3), SetScore(6, 4))),
                pair(2, "Alice", "Carlos", "Frank", "Grace",
                    sets = listOf(SetScore(6, 2), SetScore(6, 1))),
                pair(3, "Diana", "Bob", "Heidi", "Ivan",
                    sets = listOf(SetScore(3, 6), SetScore(4, 6)))
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)

        val alice = stats.first { it.name == "Alice" }
        assertEquals(2, alice.wins)
        assertEquals(0, alice.losses)
        assertEquals(1, alice.pair1Count)
        assertEquals(1, alice.pair2Count)
        assertEquals(0, alice.pair3Count)

        val bob = stats.first { it.name == "Bob" }
        assertEquals(1, bob.wins)
        assertEquals(1, bob.losses)
        assertEquals(1, bob.pair1Count)
        assertEquals(0, bob.pair2Count)
        assertEquals(1, bob.pair3Count)
    }

    // ── Three-set matches ───────────────────────────────────────

    @Test
    fun `three-set match counts correctly for the winner`() {
        val matches = listOf(match(1, "/d/1"))
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "Alice", "Bob", "Eve", "Mallory",
                    sets = listOf(SetScore(6, 3), SetScore(3, 6), SetScore(7, 5)))
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)
        val alice = stats.first { it.name == "Alice" }
        assertEquals(1, alice.wins) // local won 2 sets to 1
    }

    // ── Edge cases ──────────────────────────────────────────────

    @Test
    fun `empty sets are skipped`() {
        val matches = listOf(match(1, "/d/1"))
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "Alice", "Bob", "Eve", "Mallory", sets = emptyList())
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)
        assertTrue(stats.isEmpty())
    }

    @Test
    fun `tied sets (equal wins) are skipped`() {
        val matches = listOf(match(1, "/d/1"))
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "Alice", "Bob", "Eve", "Mallory",
                    sets = listOf(SetScore(6, 3), SetScore(3, 6)))  // 1-1 in sets
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)
        assertTrue(stats.isEmpty())
    }

    @Test
    fun `empty player name is skipped`() {
        val matches = listOf(match(1, "/d/1"))
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "Alice", "", "Eve", "Mallory",
                    sets = listOf(SetScore(6, 3), SetScore(6, 4)))
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)
        assertEquals(1, stats.size) // only Alice, not the empty-name player
        assertEquals("Alice", stats[0].name)
    }

    @Test
    fun `whitespace-only player name is skipped`() {
        val matches = listOf(match(1, "/d/1"))
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "  ", "Bob", "Eve", "Mallory",
                    sets = listOf(SetScore(6, 3), SetScore(6, 4)))
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)
        assertEquals(1, stats.size)
        assertEquals("Bob", stats[0].name)
    }

    @Test
    fun `case-insensitive player name dedup`() {
        val matches = listOf(
            match(1, "/d/1"),
            match(2, "/d/2")
        )
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "Alice", "Bob", "Eve", "Mallory",
                    sets = listOf(SetScore(6, 3), SetScore(6, 4)))
            )),
            "/d/2" to MatchDetail(pairs = listOf(
                pair(1, "ALICE", "Bob", "Frank", "Grace",
                    sets = listOf(SetScore(6, 2), SetScore(6, 1)))
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)
        // "Alice" and "ALICE" should be merged (first occurrence display name used)
        val aliceStats = stats.filter { it.name.equals("Alice", ignoreCase = true) }
        assertEquals(1, aliceStats.size)
        assertEquals(2, aliceStats[0].wins)
    }

    @Test
    fun `no played matches returns empty stats`() {
        val stats = TeamViewModel.computePlayerStats(emptyMap(), emptyList(), ourTeamId)
        assertTrue(stats.isEmpty())
    }

    @Test
    fun `match without detail entry is skipped`() {
        val matches = listOf(match(1, "/d/missing"))
        val details = emptyMap<String, MatchDetail>()

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)
        assertTrue(stats.isEmpty())
    }

    // ── Sorting ─────────────────────────────────────────────────

    @Test
    fun `results sorted by wins descending then losses ascending`() {
        val matches = listOf(match(1, "/d/1"), match(2, "/d/2"), match(3, "/d/3"))
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "Alice", "Bob", "E", "M",
                    sets = listOf(SetScore(6, 3), SetScore(6, 4)))
            )),
            "/d/2" to MatchDetail(pairs = listOf(
                pair(1, "Alice", "Carlos", "F", "G",
                    sets = listOf(SetScore(6, 2), SetScore(6, 1)))
            )),
            "/d/3" to MatchDetail(pairs = listOf(
                pair(1, "Bob", "Carlos", "H", "I",
                    sets = listOf(SetScore(3, 6), SetScore(4, 6)))
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)

        // Alice: 2 wins, 0 losses → first
        // Bob: 1 win, 1 loss → second (1 win beats Carlos's 0)
        // Carlos: 1 win, 1 loss → same wins as Bob, same losses → tied by order
        assertEquals("Alice", stats[0].name)
        assertEquals(2, stats[0].wins)
        assertTrue(stats[1].wins >= stats[2].wins)
    }

    // ── Multiple matches accumulation ───────────────────────────

    @Test
    fun `stats accumulate across multiple matches`() {
        val matches = listOf(
            match(1, "/d/1"),
            match(2, "/d/2"),
            match(3, "/d/3")
        )
        val details = mapOf(
            "/d/1" to MatchDetail(pairs = listOf(
                pair(1, "Alice", "Bob", "E", "M", sets = listOf(SetScore(6, 3), SetScore(6, 4)))
            )),
            "/d/2" to MatchDetail(pairs = listOf(
                pair(2, "Alice", "Bob", "F", "G", sets = listOf(SetScore(3, 6), SetScore(4, 6)))
            )),
            "/d/3" to MatchDetail(pairs = listOf(
                pair(1, "Alice", "Bob", "H", "I", sets = listOf(SetScore(6, 2), SetScore(6, 1)))
            ))
        )

        val stats = TeamViewModel.computePlayerStats(details, matches, ourTeamId)
        val alice = stats.first { it.name == "Alice" }

        assertEquals(2, alice.wins)
        assertEquals(1, alice.losses)
        assertEquals(2, alice.pair1Count)
        assertEquals(1, alice.pair2Count)
    }
}
