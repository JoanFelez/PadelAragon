package com.padelaragon.app.data.model

enum class League(val id: Int, val displayName: String) {
    ABSOLUTA(27951, "Absoluta"),
    VETERANOS(27961, "Veteranos"),
    MENORES(28060, "Menores");

    companion object {
        fun fromId(id: Int?): League? = entries.firstOrNull { it.id == id }
    }
}
