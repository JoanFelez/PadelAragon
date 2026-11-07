package com.padelaragon.app.ui.viewmodel

import com.padelaragon.app.data.model.Gender
import com.padelaragon.app.data.model.LeagueGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [GroupListViewModel.Companion.sortGroups] — the pure sorting
 * logic that orders league groups for display.
 *
 * Order: MASCULINA before FEMENINA → then by category → then by name.
 */
class GroupListViewModelSortTest {

    private fun group(
        id: Int = 1, name: String = "Group", gender: Gender = Gender.MASCULINA,
        category: String = "1ª", groupLetter: String? = null
    ) = LeagueGroup(id = id, name = name, gender = gender, category = category, groupLetter = groupLetter)

    // ── Gender ordering ─────────────────────────────────────────

    @Test
    fun `MASCULINA groups come before FEMENINA`() {
        val groups = listOf(
            group(id = 1, name = "Fem Group", gender = Gender.FEMENINA),
            group(id = 2, name = "Masc Group", gender = Gender.MASCULINA)
        )
        val sorted = GroupListViewModel.sortGroups(groups)

        assertEquals(Gender.MASCULINA, sorted[0].gender)
        assertEquals(Gender.FEMENINA, sorted[1].gender)
    }

    @Test
    fun `all MASCULINA groups appear before all FEMENINA groups`() {
        val groups = listOf(
            group(id = 1, name = "F1", gender = Gender.FEMENINA, category = "1ª"),
            group(id = 2, name = "M2", gender = Gender.MASCULINA, category = "2ª"),
            group(id = 3, name = "F2", gender = Gender.FEMENINA, category = "2ª"),
            group(id = 4, name = "M1", gender = Gender.MASCULINA, category = "1ª")
        )
        val sorted = GroupListViewModel.sortGroups(groups)

        // First two should be MASCULINA, last two FEMENINA
        assertEquals(Gender.MASCULINA, sorted[0].gender)
        assertEquals(Gender.MASCULINA, sorted[1].gender)
        assertEquals(Gender.FEMENINA, sorted[2].gender)
        assertEquals(Gender.FEMENINA, sorted[3].gender)
    }

    // ── Category ordering within same gender ────────────────────

    @Test
    fun `groups are sorted by category within same gender`() {
        val groups = listOf(
            group(id = 1, name = "A", gender = Gender.MASCULINA, category = "3ª"),
            group(id = 2, name = "B", gender = Gender.MASCULINA, category = "1ª"),
            group(id = 3, name = "C", gender = Gender.MASCULINA, category = "2ª")
        )
        val sorted = GroupListViewModel.sortGroups(groups)

        assertEquals(listOf("1ª", "2ª", "3ª"), sorted.map { it.category })
    }

    // ── Name ordering within same gender and category ───────────

    @Test
    fun `groups with same gender and category are sorted by name`() {
        val groups = listOf(
            group(id = 1, name = "Grupo C", gender = Gender.MASCULINA, category = "1ª"),
            group(id = 2, name = "Grupo A", gender = Gender.MASCULINA, category = "1ª"),
            group(id = 3, name = "Grupo B", gender = Gender.MASCULINA, category = "1ª")
        )
        val sorted = GroupListViewModel.sortGroups(groups)

        assertEquals(listOf("Grupo A", "Grupo B", "Grupo C"), sorted.map { it.name })
    }

    // ── Combined multi-tier sorting ─────────────────────────────

    @Test
    fun `full three-tier sort with mixed data`() {
        val groups = listOf(
            group(id = 1, name = "1ª CAT FEMENINA - B", gender = Gender.FEMENINA, category = "1ª"),
            group(id = 2, name = "2ª CAT MASCULINA - A", gender = Gender.MASCULINA, category = "2ª"),
            group(id = 3, name = "1ª CAT MASCULINA - B", gender = Gender.MASCULINA, category = "1ª"),
            group(id = 4, name = "1ª CAT MASCULINA - A", gender = Gender.MASCULINA, category = "1ª"),
            group(id = 5, name = "1ª CAT FEMENINA - A", gender = Gender.FEMENINA, category = "1ª")
        )
        val sorted = GroupListViewModel.sortGroups(groups)

        assertEquals(listOf(
            "1ª CAT MASCULINA - A",  // MASC, 1ª, A
            "1ª CAT MASCULINA - B",  // MASC, 1ª, B
            "2ª CAT MASCULINA - A",  // MASC, 2ª, A
            "1ª CAT FEMENINA - A",   // FEM, 1ª, A
            "1ª CAT FEMENINA - B"    // FEM, 1ª, B
        ), sorted.map { it.name })
    }

    // ── Edge cases ──────────────────────────────────────────────

    @Test
    fun `empty list returns empty`() {
        assertTrue(GroupListViewModel.sortGroups(emptyList()).isEmpty())
    }

    @Test
    fun `single element returns as-is`() {
        val single = group(id = 1, name = "Only One")
        val sorted = GroupListViewModel.sortGroups(listOf(single))

        assertEquals(1, sorted.size)
        assertEquals(single, sorted[0])
    }

    @Test
    fun `already sorted list stays sorted`() {
        val groups = listOf(
            group(id = 1, name = "A", gender = Gender.MASCULINA, category = "1ª"),
            group(id = 2, name = "B", gender = Gender.MASCULINA, category = "1ª"),
            group(id = 3, name = "A", gender = Gender.FEMENINA, category = "1ª")
        )
        val sorted = GroupListViewModel.sortGroups(groups)

        assertEquals(groups, sorted)
    }
}
