package com.padelaragon.app.data.local

import com.padelaragon.app.data.local.entity.JornadaEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests for [JornadaEntity].
 * JornadaEntity is a simple two-field entity with no model mapping methods,
 * so tests verify construction and composite-key semantics.
 *
 * DAO-level (SQL) coverage deferred — see [LeagueGroupEntityTest] header.
 */
class JornadaEntityTest {

    @Test
    fun `entity holds groupId and jornada`() {
        val entity = JornadaEntity(groupId = 5, jornada = 3)

        assertEquals(5, entity.groupId)
        assertEquals(3, entity.jornada)
    }

    @Test
    fun `data class equality uses both fields`() {
        val a = JornadaEntity(groupId = 1, jornada = 2)
        val b = JornadaEntity(groupId = 1, jornada = 2)
        val c = JornadaEntity(groupId = 1, jornada = 3)
        val d = JornadaEntity(groupId = 2, jornada = 2)

        assertEquals(a, b)
        assert(a != c) { "Different jornada should not be equal" }
        assert(a != d) { "Different groupId should not be equal" }
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = JornadaEntity(groupId = 10, jornada = 5)
        val copied = original.copy(jornada = 6)

        assertEquals(10, copied.groupId)
        assertEquals(6, copied.jornada)
    }
}
