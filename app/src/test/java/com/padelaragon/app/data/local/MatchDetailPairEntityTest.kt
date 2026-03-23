package com.padelaragon.app.data.local

import com.padelaragon.app.data.local.entity.MatchDetailPairEntity
import com.padelaragon.app.data.model.PairDetail
import com.padelaragon.app.data.model.SetScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-JVM tests for [MatchDetailPairEntity] ↔ [PairDetail] mapping.
 * The set-flattening/unflattening logic (3 sets → 6 nullable columns) is
 * the most error-prone mapping in the cache layer, so edge cases are
 * covered thoroughly here.
 *
 * DAO-level (SQL) coverage deferred — see [LeagueGroupEntityTest] header.
 */
class MatchDetailPairEntityTest {

    // ── toModel ─────────────────────────────────────────────────

    @Test
    fun `toModel builds two sets when third is null`() {
        val entity = MatchDetailPairEntity(
            detailUrl = "/d/1", pairNumber = 1,
            localPlayer1 = "LP1", localPlayer2 = "LP2",
            visitorPlayer1 = "VP1", visitorPlayer2 = "VP2",
            set1Local = 6, set1Visitor = 3,
            set2Local = 4, set2Visitor = 6,
            set3Local = null, set3Visitor = null
        )
        val model = entity.toModel()

        assertEquals(2, model.sets.size)
        assertEquals(SetScore(6, 3), model.sets[0])
        assertEquals(SetScore(4, 6), model.sets[1])
    }

    @Test
    fun `toModel builds three sets when all present`() {
        val entity = MatchDetailPairEntity(
            detailUrl = "/d/1", pairNumber = 2,
            localPlayer1 = "A", localPlayer2 = "B",
            visitorPlayer1 = "C", visitorPlayer2 = "D",
            set1Local = 6, set1Visitor = 4,
            set2Local = 3, set2Visitor = 6,
            set3Local = 7, set3Visitor = 5
        )
        val model = entity.toModel()

        assertEquals(3, model.sets.size)
        assertEquals(SetScore(7, 5), model.sets[2])
    }

    @Test
    fun `toModel builds empty sets list when all scores are null`() {
        val entity = MatchDetailPairEntity(
            detailUrl = "/d/1", pairNumber = 1,
            localPlayer1 = "A", localPlayer2 = "B",
            visitorPlayer1 = "C", visitorPlayer2 = "D",
            set1Local = null, set1Visitor = null,
            set2Local = null, set2Visitor = null,
            set3Local = null, set3Visitor = null
        )
        val model = entity.toModel()

        assertEquals(0, model.sets.size)
    }

    @Test
    fun `toModel preserves player names and pair number`() {
        val entity = MatchDetailPairEntity(
            detailUrl = "/d/1", pairNumber = 3,
            localPlayer1 = "García, Juan", localPlayer2 = "López, Pedro",
            visitorPlayer1 = "Martín, Ana", visitorPlayer2 = "Ruiz, Elena",
            set1Local = 6, set1Visitor = 2,
            set2Local = 6, set2Visitor = 1,
            set3Local = null, set3Visitor = null
        )
        val model = entity.toModel()

        assertEquals(3, model.pairNumber)
        assertEquals("García, Juan", model.localPlayer1)
        assertEquals("López, Pedro", model.localPlayer2)
        assertEquals("Martín, Ana", model.visitorPlayer1)
        assertEquals("Ruiz, Elena", model.visitorPlayer2)
    }

    // ── fromModel ───────────────────────────────────────────────

    @Test
    fun `fromModel flattens three sets into six columns`() {
        val model = PairDetail(
            pairNumber = 1,
            localPlayer1 = "A", localPlayer2 = "B",
            visitorPlayer1 = "C", visitorPlayer2 = "D",
            sets = listOf(SetScore(6, 4), SetScore(3, 6), SetScore(7, 5))
        )
        val entity = MatchDetailPairEntity.fromModel("/url", model)

        assertEquals(6, entity.set1Local)
        assertEquals(4, entity.set1Visitor)
        assertEquals(3, entity.set2Local)
        assertEquals(6, entity.set2Visitor)
        assertEquals(7, entity.set3Local)
        assertEquals(5, entity.set3Visitor)
    }

    @Test
    fun `fromModel with two sets leaves third pair null`() {
        val model = PairDetail(
            pairNumber = 1,
            localPlayer1 = "A", localPlayer2 = "B",
            visitorPlayer1 = "C", visitorPlayer2 = "D",
            sets = listOf(SetScore(6, 2), SetScore(6, 3))
        )
        val entity = MatchDetailPairEntity.fromModel("/url", model)

        assertEquals(6, entity.set1Local)
        assertEquals(2, entity.set1Visitor)
        assertEquals(6, entity.set2Local)
        assertEquals(3, entity.set2Visitor)
        assertNull(entity.set3Local)
        assertNull(entity.set3Visitor)
    }

    @Test
    fun `fromModel with one set leaves second and third null`() {
        val model = PairDetail(
            pairNumber = 1,
            localPlayer1 = "A", localPlayer2 = "B",
            visitorPlayer1 = "C", visitorPlayer2 = "D",
            sets = listOf(SetScore(6, 0))
        )
        val entity = MatchDetailPairEntity.fromModel("/url", model)

        assertEquals(6, entity.set1Local)
        assertEquals(0, entity.set1Visitor)
        assertNull(entity.set2Local)
        assertNull(entity.set2Visitor)
        assertNull(entity.set3Local)
        assertNull(entity.set3Visitor)
    }

    @Test
    fun `fromModel with empty sets leaves all score columns null`() {
        val model = PairDetail(
            pairNumber = 1,
            localPlayer1 = "A", localPlayer2 = "B",
            visitorPlayer1 = "C", visitorPlayer2 = "D",
            sets = emptyList()
        )
        val entity = MatchDetailPairEntity.fromModel("/url", model)

        assertNull(entity.set1Local)
        assertNull(entity.set1Visitor)
        assertNull(entity.set2Local)
        assertNull(entity.set2Visitor)
        assertNull(entity.set3Local)
        assertNull(entity.set3Visitor)
    }

    // ── Round-trip ──────────────────────────────────────────────

    @Test
    fun `round-trip three sets preserves all data`() {
        val original = PairDetail(
            pairNumber = 2,
            localPlayer1 = "A", localPlayer2 = "B",
            visitorPlayer1 = "C", visitorPlayer2 = "D",
            sets = listOf(SetScore(6, 4), SetScore(3, 6), SetScore(7, 5))
        )
        val roundTripped = MatchDetailPairEntity.fromModel("/url", original).toModel()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `round-trip two sets preserves all data`() {
        val original = PairDetail(
            pairNumber = 1,
            localPlayer1 = "X", localPlayer2 = "Y",
            visitorPlayer1 = "W", visitorPlayer2 = "Z",
            sets = listOf(SetScore(6, 1), SetScore(6, 2))
        )
        val roundTripped = MatchDetailPairEntity.fromModel("/url", original).toModel()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `round-trip empty sets preserves empty list`() {
        val original = PairDetail(
            pairNumber = 3,
            localPlayer1 = "A", localPlayer2 = "B",
            visitorPlayer1 = "C", visitorPlayer2 = "D",
            sets = emptyList()
        )
        val roundTripped = MatchDetailPairEntity.fromModel("/url", original).toModel()

        assertEquals(original, roundTripped)
    }
}
