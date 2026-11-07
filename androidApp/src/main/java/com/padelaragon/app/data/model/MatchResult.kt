package com.padelaragon.app.data.model

data class MatchResult(
    val localTeam: String,
    val localTeamId: Int,
    val visitorTeam: String,
    val visitorTeamId: Int,
    val localScore: String,
    val visitorScore: String,
    val date: String?,
    val venue: String?,
    val jornada: Int,
    val detailUrl: String? = null
)
