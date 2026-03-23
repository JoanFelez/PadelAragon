package com.padelaragon.app.data.parser

import com.padelaragon.app.FixtureLoader
import com.padelaragon.app.data.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Local JVM tests for [GroupParser] using saved HTML fixtures from
 * `test/resources/fixtures/parser/group/`. No network access or
 * Android emulator required.
 */
class GroupParserTest {

    private val parser = GroupParser()

    // ── Happy-path: group extraction ────────────────────────────

    @Test
    fun `parse extracts all groups from fixture HTML`() {
        val html = loadFixture("valid_groups.html")
        val groups = parser.parse(html)

        assertEquals("Expected 5 groups from fixture", 5, groups.size)
    }

    @Test
    fun `parse identifies masculine and feminine groups correctly`() {
        val html = loadFixture("valid_groups.html")
        val groups = parser.parse(html)

        val masculine = groups.filter { it.gender == Gender.MASCULINA }
        val feminine = groups.filter { it.gender == Gender.FEMENINA }

        assertEquals("Expected 3 masculine groups", 3, masculine.size)
        assertEquals("Expected 2 feminine groups", 2, feminine.size)
    }

    @Test
    fun `parse extracts group IDs as integers`() {
        val html = loadFixture("valid_groups.html")
        val groups = parser.parse(html)

        val ids = groups.map { it.id }
        assertTrue("All IDs should be positive", ids.all { it > 0 })
        assertTrue("Expected ID 30941 present", 30941 in ids)
        assertTrue("Expected ID 30960 present", 30960 in ids)
    }

    @Test
    fun `parse extracts group letter when present`() {
        val html = loadFixture("valid_groups.html")
        val groups = parser.parse(html)

        val groupA = groups.firstOrNull { it.id == 30941 }
        assertNotNull("Group 30941 should exist", groupA)
        assertEquals("A", groupA!!.groupLetter)
    }

    @Test
    fun `parse extracts category from group name`() {
        val html = loadFixture("valid_groups.html")
        val groups = parser.parse(html)

        val first = groups.first { it.id == 30941 }
        assertEquals("1ª CATEGORÍA", first.category)
    }

    // ── Happy-path: jornada extraction ──────────────────────────

    @Test
    fun `parseJornadas extracts jornada numbers from fixture`() {
        val html = loadFixture("valid_groups.html")
        val jornadas = parser.parseJornadas(html)

        assertEquals("Expected 3 jornadas", 3, jornadas.size)
        assertEquals(listOf(1, 2, 3), jornadas)
    }

    // ── Fallback: without optgroups gender inferred from text ───

    @Test
    fun `parse infers gender from option text when no optgroups present`() {
        val html = loadFixture("no_optgroups.html")
        val groups = parser.parse(html)

        assertEquals(2, groups.size)
        assertEquals(Gender.MASCULINA, groups.first { it.id == 100 }.gender)
        assertEquals(Gender.FEMENINA, groups.first { it.id == 101 }.gender)
    }

    // ── Malformed / edge-case HTML ──────────────────────────────

    @Test(expected = IOException::class)
    fun `parse throws IOException when group selector is missing`() {
        val html = loadFixture("no_group_selector.html")
        parser.parse(html)
    }

    @Test(expected = IOException::class)
    fun `parse throws IOException on blank input`() {
        parser.parse("   ")
    }

    @Test
    fun `parse returns empty list when only placeholder options exist`() {
        val html = loadFixture("empty_options.html")
        val groups = parser.parse(html)

        assertTrue("Placeholder options should be skipped", groups.isEmpty())
    }

    @Test
    fun `parseJornadas returns empty list when jornada selector is missing`() {
        val html = loadFixture("no_group_selector.html")
        val jornadas = parser.parseJornadas(html)

        assertTrue("Should return empty for missing jornada selector", jornadas.isEmpty())
    }

    @Test
    fun `parseJornadas returns empty list when jornada selector has no options`() {
        val html = loadFixture("empty_options.html")
        val jornadas = parser.parseJornadas(html)

        assertTrue("Empty jornada select should yield no jornadas", jornadas.isEmpty())
    }

    // ── Fixture helper ──────────────────────────────────────────

    private fun loadFixture(name: String): String =
        FixtureLoader.load("parser/group/$name")
}
