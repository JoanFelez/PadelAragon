package com.padelaragon.app.data.model

data class MatchResult(
    val localTeam: String,
    val localTeamId: Int,
    val visitorTeam: String,
    val visitorTeamId: Int,
    val localScore: String, // String because can be "--" when not played
    val visitorScore: String,
    val date: String?, // Date string as scraped
    val venue: String?, // Venue/location
    val jornada: Int // Round/matchday number
)
