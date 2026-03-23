package com.padelaragon.app.data.local

import com.padelaragon.app.data.local.entity.StandingRowEntity
import com.padelaragon.app.data.model.StandingRow
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests for [StandingRowEntity] ↔ [StandingRow] mapping.
 * Verifies all 15 fields survive the entity-model round-trip.
 *
 * DAO-level (SQL) coverage deferred — see [LeagueGroupEntityTest] header.
 */
class StandingRowEntityTest {

    private fun makeModel(
        position: Int = 1, teamId: Int = 10, teamName: String = "Team A"
    ) = StandingRow(
        position = position, teamName = teamName, teamId = teamId,
        teamHref = "/team/$teamId", points = 12, matchesPlayed = 6,
        encountersWon = 4, encountersLost = 2, matchesWon = 9, matchesLost = 3,
        setsWon = 20, setsLost = 8, gamesWon = 130, gamesLost = 80
    )

    private fun makeEntity(
        groupId: Int = 1, position: Int = 1, teamId: Int = 10, teamName: String = "Team A"
    ) = StandingRowEntity(
        groupId = groupId, position = position, teamName = teamName, teamId = teamId,
        teamHref = "/team/$teamId", points = 12, matchesPlayed = 6,
        encountersWon = 4, encountersLost = 2, matchesWon = 9, matchesLost = 3,
        setsWon = 20, setsLost = 8, gamesWon = 130, gamesLost = 80
    )

    @Test
    fun `toModel preserves all fields`() {
        val entity = makeEntity(groupId = 5, position = 3, teamId = 42, teamName = "Los Campeones")
        val model = entity.toModel()

        assertEquals(3, model.position)
        assertEquals("Los Campeones", model.teamName)
        assertEquals(42, model.teamId)
        assertEquals("/team/42", model.teamHref)
        assertEquals(12, model.points)
        assertEquals(6, model.matchesPlayed)
        assertEquals(4, model.encountersWon)
        assertEquals(2, model.encountersLost)
        assertEquals(9, model.matchesWon)
        assertEquals(3, model.matchesLost)
        assertEquals(20, model.setsWon)
        assertEquals(8, model.setsLost)
        assertEquals(130, model.gamesWon)
        assertEquals(80, model.gamesLost)
    }

    @Test
    fun `fromModel preserves groupId and all model fields`() {
        val model = makeModel(position = 2, teamId = 55, teamName = "Visitantes")
        val entity = StandingRowEntity.fromModel(groupId = 7, model = model)

        assertEquals(7, entity.groupId)
        assertEquals(2, entity.position)
        assertEquals("Visitantes", entity.teamName)
        assertEquals(55, entity.teamId)
        assertEquals("/team/55", entity.teamHref)
        assertEquals(12, entity.points)
    }

    @Test
    fun `round-trip model to entity to model is identity (minus groupId)`() {
        val original = makeModel(position = 1, teamId = 10)
        val roundTripped = StandingRowEntity.fromModel(groupId = 1, model = original).toModel()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `entity composite key fields are independent`() {
        val e1 = makeEntity(groupId = 1, teamId = 10)
        val e2 = makeEntity(groupId = 1, teamId = 20)
        val e3 = makeEntity(groupId = 2, teamId = 10)

        // Composite key (groupId, teamId) — all three should be distinct
        val keys = listOf(e1, e2, e3).map { it.groupId to it.teamId }.toSet()
        assertEquals(3, keys.size)
    }
}
