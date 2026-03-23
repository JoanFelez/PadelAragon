package com.padelaragon.app.data.local

import com.padelaragon.app.data.local.entity.PlayerEntity
import com.padelaragon.app.data.local.entity.TeamDetailEntity
import com.padelaragon.app.data.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [TeamDetailEntity] and [PlayerEntity] ↔ [Player] mapping.
 *
 * DAO-level (SQL) coverage deferred — see [LeagueGroupEntityTest] header.
 */
class TeamDetailEntityTest {

    // ── TeamDetailEntity ────────────────────────────────────────

    @Test
    fun `TeamDetailEntity holds all fields`() {
        val entity = TeamDetailEntity(teamId = 1, category = "1ª CATEGORÍA", captainName = "García López")

        assertEquals(1, entity.teamId)
        assertEquals("1ª CATEGORÍA", entity.category)
        assertEquals("García López", entity.captainName)
    }

    @Test
    fun `TeamDetailEntity allows null category and captainName`() {
        val entity = TeamDetailEntity(teamId = 2, category = null, captainName = null)

        assertNull(entity.category)
        assertNull(entity.captainName)
    }

    // ── PlayerEntity ↔ Player ───────────────────────────────────

    @Test
    fun `PlayerEntity toModel preserves all fields`() {
        val entity = PlayerEntity(
            teamId = 1, name = "María García", isCaptain = true,
            points = "750", birthYear = "1988"
        )
        val model = entity.toModel()

        assertEquals("María García", model.name)
        assertTrue(model.isCaptain)
        assertEquals("750", model.points)
        assertEquals("1988", model.birthYear)
    }

    @Test
    fun `PlayerEntity toModel handles nulls`() {
        val entity = PlayerEntity(
            teamId = 1, name = "Player", isCaptain = false,
            points = null, birthYear = null
        )
        val model = entity.toModel()

        assertFalse(model.isCaptain)
        assertNull(model.points)
        assertNull(model.birthYear)
    }

    @Test
    fun `PlayerEntity fromModel preserves teamId and model fields`() {
        val model = Player(name = "Juan Pérez", isCaptain = false, points = "300", birthYear = "1995")
        val entity = PlayerEntity.fromModel(teamId = 42, model = model)

        assertEquals(42, entity.teamId)
        assertEquals("Juan Pérez", entity.name)
        assertFalse(entity.isCaptain)
        assertEquals("300", entity.points)
        assertEquals("1995", entity.birthYear)
    }

    @Test
    fun `PlayerEntity round-trip model to entity to model is identity`() {
        val original = Player(name = "Ana Ruiz", isCaptain = true, points = "500", birthYear = "1990")
        val roundTripped = PlayerEntity.fromModel(teamId = 10, model = original).toModel()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `PlayerEntity round-trip with null optional fields`() {
        val original = Player(name = "Test", isCaptain = false, points = null, birthYear = null)
        val roundTripped = PlayerEntity.fromModel(teamId = 1, model = original).toModel()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `PlayerEntity composite key uses teamId and name`() {
        val p1 = PlayerEntity(teamId = 1, name = "A", isCaptain = false, points = null, birthYear = null)
        val p2 = PlayerEntity(teamId = 1, name = "B", isCaptain = false, points = null, birthYear = null)
        val p3 = PlayerEntity(teamId = 2, name = "A", isCaptain = false, points = null, birthYear = null)

        // All three have distinct composite keys
        val keys = listOf(p1, p2, p3).map { it.teamId to it.name }.toSet()
        assertEquals(3, keys.size)
    }
}
