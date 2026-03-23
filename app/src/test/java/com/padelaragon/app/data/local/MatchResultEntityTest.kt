package com.padelaragon.app.data.local

import com.padelaragon.app.data.local.entity.MatchResultEntity
import com.padelaragon.app.data.model.MatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-JVM tests for [MatchResultEntity] ↔ [MatchResult] mapping.
 *
 * DAO-level (SQL) coverage deferred — see [LeagueGroupEntityTest] header.
 */
class MatchResultEntityTest {

    private fun makeModel(
        jornada: Int = 1, localTeamId: Int = 10, visitorTeamId: Int = 20,
        detailUrl: String? = "/detail/url"
    ) = MatchResult(
        localTeam = "Local $localTeamId", localTeamId = localTeamId,
        visitorTeam = "Visitor $visitorTeamId", visitorTeamId = visitorTeamId,
        localScore = "3", visitorScore = "0",
        date = "2024-03-15", venue = "Polideportivo", jornada = jornada,
        detailUrl = detailUrl
    )

    @Test
    fun `toModel preserves all fields`() {
        val entity = MatchResultEntity(
            groupId = 1, localTeam = "Club A", localTeamId = 10,
            visitorTeam = "Club B", visitorTeamId = 20,
            localScore = "2", visitorScore = "1",
            date = "2024-01-10", venue = "Pabellón Norte",
            jornada = 5, detailUrl = "/detail/5"
        )
        val model = entity.toModel()

        assertEquals("Club A", model.localTeam)
        assertEquals(10, model.localTeamId)
        assertEquals("Club B", model.visitorTeam)
        assertEquals(20, model.visitorTeamId)
        assertEquals("2", model.localScore)
        assertEquals("1", model.visitorScore)
        assertEquals("2024-01-10", model.date)
        assertEquals("Pabellón Norte", model.venue)
        assertEquals(5, model.jornada)
        assertEquals("/detail/5", model.detailUrl)
    }

    @Test
    fun `toModel handles null date, venue, and detailUrl`() {
        val entity = MatchResultEntity(
            groupId = 1, localTeam = "A", localTeamId = 1,
            visitorTeam = "B", visitorTeamId = 2,
            localScore = "--", visitorScore = "--",
            date = null, venue = null, jornada = 1, detailUrl = null
        )
        val model = entity.toModel()

        assertNull(model.date)
        assertNull(model.venue)
        assertNull(model.detailUrl)
    }

    @Test
    fun `fromModel preserves groupId and all model fields`() {
        val model = makeModel(jornada = 3, localTeamId = 100, visitorTeamId = 200)
        val entity = MatchResultEntity.fromModel(groupId = 42, model = model)

        assertEquals(42, entity.groupId)
        assertEquals("Local 100", entity.localTeam)
        assertEquals(100, entity.localTeamId)
        assertEquals("Visitor 200", entity.visitorTeam)
        assertEquals(200, entity.visitorTeamId)
        assertEquals(3, entity.jornada)
        assertEquals("/detail/url", entity.detailUrl)
    }

    @Test
    fun `round-trip model to entity to model is identity (minus groupId)`() {
        val original = makeModel(jornada = 2, localTeamId = 50, visitorTeamId = 60)
        val roundTripped = MatchResultEntity.fromModel(groupId = 1, model = original).toModel()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `round-trip with null detailUrl`() {
        val original = makeModel(detailUrl = null)
        val roundTripped = MatchResultEntity.fromModel(groupId = 1, model = original).toModel()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `unplayed match with dashes round-trips`() {
        val model = MatchResult(
            localTeam = "A", localTeamId = 1, visitorTeam = "B", visitorTeamId = 2,
            localScore = "--", visitorScore = "--",
            date = null, venue = null, jornada = 10, detailUrl = null
        )
        val roundTripped = MatchResultEntity.fromModel(groupId = 1, model = model).toModel()

        assertEquals("--", roundTripped.localScore)
        assertEquals("--", roundTripped.visitorScore)
        assertEquals(model, roundTripped)
    }
}
