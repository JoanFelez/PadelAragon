package com.padelaragon.shared.data.model

/**
 * A top-level competition on padelfederacion.es (the `Liga=` query param), distinct from
 * [LeagueGroup] which represents a single category/group *within* a league.
 *
 * The built-in constants are the offline fallback; the backend `/api/leagues` list can add or
 * rename leagues each season without an app release (see `LeagueCatalog`).
 */
data class League(val id: Int, val displayName: String) {
    companion object {
        val ABSOLUTA = League(27951, "Absoluta")
        val VETERANOS = League(27961, "Veteranos")
        val MENORES = League(28060, "Menores")

        val entries: List<League> = listOf(ABSOLUTA, VETERANOS, MENORES)

        fun fromId(id: Int): League? = entries.find { it.id == id }
    }
}
