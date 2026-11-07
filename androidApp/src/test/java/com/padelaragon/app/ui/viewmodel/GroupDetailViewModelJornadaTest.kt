package com.padelaragon.app.ui.viewmodel

import com.padelaragon.app.data.model.MatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for [GroupDetailViewModel.Companion.findDefaultJornada] — the pure
 * logic that picks which jornada tab to show by default.
 *
 * Rule: pick the latest jornada that has at least one played match (scores
 * are not "--"). If no jornada has results yet, fall back to the first
 * available jornada. Empty list returns null.
 */
class GroupDetailViewModelJornadaTest {

    private fun match(
        jornada: Int, localScore: String = "3", visitorScore: String = "0"
    ) = MatchResult(
        localTeam = "A", localTeamId = 1, visitorTeam = "B", visitorTeamId = 2,
        localScore = localScore, visitorScore = visitorScore,
        date = null, venue = null, jornada = jornada
    )

    // ── Basic selection ─────────────────────────────────────────

    @Test
    fun `selects latest jornada with played results`() {
        val jornadas = listOf(1, 2, 3)
        val results = mapOf(
            1 to listOf(match(1, "3", "0")),
            2 to listOf(match(2, "2", "1")),
            3 to listOf(match(3, "--", "--"))
        )

        val selected = GroupDetailViewModel.findDefaultJornada(jornadas, results)

        assertEquals(2, selected)
    }

    @Test
    fun `selects last jornada when all have results`() {
        val jornadas = listOf(1, 2, 3)
        val results = mapOf(
            1 to listOf(match(1, "3", "0")),
            2 to listOf(match(2, "2", "1")),
            3 to listOf(match(3, "1", "2"))
        )

        val selected = GroupDetailViewModel.findDefaultJornada(jornadas, results)

        assertEquals(3, selected)
    }

    // ── Fallback to first ───────────────────────────────────────

    @Test
    fun `falls back to first jornada when none have results`() {
        val jornadas = listOf(1, 2, 3)
        val results = mapOf(
            1 to listOf(match(1, "--", "--")),
            2 to listOf(match(2, "--", "--")),
            3 to listOf(match(3, "--", "--"))
        )

        val selected = GroupDetailViewModel.findDefaultJornada(jornadas, results)

        assertEquals(1, selected)
    }

    @Test
    fun `falls back to first jornada when results map is empty`() {
        val jornadas = listOf(5, 6, 7)
        val results = emptyMap<Int, List<MatchResult>>()

        val selected = GroupDetailViewModel.findDefaultJornada(jornadas, results)

        assertEquals(5, selected)
    }

    // ── Edge cases ──────────────────────────────────────────────

    @Test
    fun `returns null for empty jornada list`() {
        assertNull(GroupDetailViewModel.findDefaultJornada(emptyList(), emptyMap()))
    }

    @Test
    fun `single jornada with results selects it`() {
        val jornadas = listOf(1)
        val results = mapOf(1 to listOf(match(1, "3", "0")))

        assertEquals(1, GroupDetailViewModel.findDefaultJornada(jornadas, results))
    }

    @Test
    fun `single jornada without results still selects it (fallback)`() {
        val jornadas = listOf(1)
        val results = mapOf(1 to listOf(match(1, "--", "--")))

        assertEquals(1, GroupDetailViewModel.findDefaultJornada(jornadas, results))
    }

    @Test
    fun `mixed played and unplayed in same jornada - counts as played`() {
        val jornadas = listOf(1, 2)
        val results = mapOf(
            1 to listOf(match(1, "3", "0")),
            2 to listOf(
                match(2, "2", "1"),   // played
                match(2, "--", "--")  // not yet played
            )
        )

        // Jornada 2 has at least one played match, so it should be selected
        assertEquals(2, GroupDetailViewModel.findDefaultJornada(jornadas, results))
    }

    @Test
    fun `only considers jornadas in the provided sorted list`() {
        val jornadas = listOf(1, 3)  // jornada 2 is not in the list
        val results = mapOf(
            1 to listOf(match(1, "3", "0")),
            2 to listOf(match(2, "2", "1")),  // has results but not in jornadas list
            3 to listOf(match(3, "--", "--"))
        )

        // Jornada 3 has no results, jornada 2 has results but is not in the list
        // So latest with results is jornada 1
        assertEquals(1, GroupDetailViewModel.findDefaultJornada(jornadas, results))
    }

    @Test
    fun `partial score with one dash does not count as unplayed`() {
        // A score of "3" / "--" is technically partial, but the logic checks
        // both must be "--" to be unplayed. So "3"/"--" counts as played.
        val jornadas = listOf(1)
        val results = mapOf(
            1 to listOf(match(1, "3", "--"))
        )

        // localScore != "--" so this jornada counts as having results
        assertEquals(1, GroupDetailViewModel.findDefaultJornada(jornadas, results))
    }

    @Test
    fun `jornada with empty match list is treated as no results`() {
        val jornadas = listOf(1, 2)
        val results = mapOf(
            1 to listOf(match(1, "2", "1")),
            2 to emptyList<MatchResult>()
        )

        // Jornada 2 has no matches at all, so latest with results is 1
        assertEquals(1, GroupDetailViewModel.findDefaultJornada(jornadas, results))
    }
}
