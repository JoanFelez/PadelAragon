package com.padelaragon.app.data.model

data class StandingRow(
    val position: Int,
    val teamName: String,
    val teamId: Int,
    val teamHref: String = "",
    val points: Int,
    val matchesPlayed: Int,
    val encountersWon: Int,
    val encountersLost: Int,
    val matchesWon: Int, // Partidos ganados
    val matchesLost: Int, // Partidos perdidos
    val setsWon: Int,
    val setsLost: Int,
    val gamesWon: Int,
    val gamesLost: Int
)
