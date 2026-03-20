package com.padelaragon.app.data.model

/**
 * Aggregated info about a team across the league.
 */
data class TeamInfo(
    val teamId: Int,
    val teamName: String,
    val groupName: String,     // The group this team belongs to
    val groupId: Int,
    val standing: StandingRow?, // Their position in standings (null if not yet available)
    val matches: List<MatchResult>, // All their matches across all jornadas, sorted by jornada
    val teamDetail: TeamDetail? = null
)
