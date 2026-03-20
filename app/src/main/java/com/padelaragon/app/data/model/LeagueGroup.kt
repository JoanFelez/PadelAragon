package com.padelaragon.app.data.model

data class LeagueGroup(
    val id: Int, // Group ID (e.g., 30941) used in URL params
    val name: String, // Full name like "1ª CATEGORÍA MASCULINA - GRUPO A"
    val gender: Gender, // MASCULINA or FEMENINA
    val category: String, // e.g., "1ª CATEGORÍA"
    val groupLetter: String? // e.g., "A", "B", null if no group subdivision
)

enum class Gender { MASCULINA, FEMENINA }
