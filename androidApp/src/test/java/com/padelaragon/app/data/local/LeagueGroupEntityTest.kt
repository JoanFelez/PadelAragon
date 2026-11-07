package com.padelaragon.app.data.local

import com.padelaragon.app.data.local.entity.LeagueGroupEntity
import com.padelaragon.app.data.model.Gender
import com.padelaragon.app.data.model.LeagueGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-JVM tests for [LeagueGroupEntity] ↔ [LeagueGroup] mapping.
 * Catches field-mapping and enum-serialization mistakes without a device.
 *
 * NOTE: DAO-level insert/query round-trips require Robolectric's Android SDK
 * JAR, which must be downloaded at test time. That download is unavailable in
 * this offline workspace, so SQL-level coverage is deferred to an environment
 * with network access or an emulator.
 */
class LeagueGroupEntityTest {

    @Test
    fun `toModel converts entity to model with MASCULINA gender`() {
        val entity = LeagueGroupEntity(
            id = 30941, name = "1ª CATEGORÍA MASCULINA - GRUPO A",
            gender = "MASCULINA", category = "1ª CATEGORÍA", groupLetter = "A"
        )
        val model = entity.toModel()

        assertEquals(30941, model.id)
        assertEquals("1ª CATEGORÍA MASCULINA - GRUPO A", model.name)
        assertEquals(Gender.MASCULINA, model.gender)
        assertEquals("1ª CATEGORÍA", model.category)
        assertEquals("A", model.groupLetter)
    }

    @Test
    fun `toModel converts entity to model with FEMENINA gender`() {
        val entity = LeagueGroupEntity(
            id = 42, name = "2ª FEMENINA", gender = "FEMENINA",
            category = "2ª CATEGORÍA", groupLetter = null
        )
        val model = entity.toModel()

        assertEquals(Gender.FEMENINA, model.gender)
        assertNull(model.groupLetter)
    }

    @Test
    fun `fromModel converts model to entity`() {
        val model = LeagueGroup(
            id = 99, name = "Round Trip",
            gender = Gender.MASCULINA, category = "1ª", groupLetter = "B"
        )
        val entity = LeagueGroupEntity.fromModel(model)

        assertEquals(99, entity.id)
        assertEquals("Round Trip", entity.name)
        assertEquals("MASCULINA", entity.gender)
        assertEquals("1ª", entity.category)
        assertEquals("B", entity.groupLetter)
    }

    @Test
    fun `fromModel with null groupLetter`() {
        val model = LeagueGroup(
            id = 5, name = "No Letter", gender = Gender.FEMENINA,
            category = "3ª", groupLetter = null
        )
        val entity = LeagueGroupEntity.fromModel(model)

        assertNull(entity.groupLetter)
        assertEquals("FEMENINA", entity.gender)
    }

    @Test
    fun `round-trip model to entity to model is identity`() {
        val original = LeagueGroup(
            id = 100, name = "Full Trip", gender = Gender.MASCULINA,
            category = "1ª CATEGORÍA", groupLetter = "C"
        )
        val roundTripped = LeagueGroupEntity.fromModel(original).toModel()

        assertEquals(original, roundTripped)
    }

    @Test
    fun `round-trip with null groupLetter preserves null`() {
        val original = LeagueGroup(
            id = 200, name = "No Group", gender = Gender.FEMENINA,
            category = "2ª", groupLetter = null
        )
        val roundTripped = LeagueGroupEntity.fromModel(original).toModel()

        assertEquals(original, roundTripped)
    }
}
