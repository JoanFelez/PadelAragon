package com.padelaragon.shared.data.model

/**
 * A top-level competition on padelfederacion.es (the `Liga=` query param), distinct from
 * [LeagueGroup] which represents a single category/group *within* a league.
 */
enum class League(val id: Int, val displayName: String) {
    ABSOLUTA(27951, "Absoluta"),
    VETERANOS(27961, "Veteranos"),
    MENORES(28060, "Menores");

    companion object {
        fun fromId(id: Int): League? = entries.find { it.id == id }
    }
}
