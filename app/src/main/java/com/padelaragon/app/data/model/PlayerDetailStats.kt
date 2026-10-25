package com.padelaragon.app.data.model

data class PlayerDetailStats(
    val name: String,
    val matchesWon: Int,
    val matchesLost: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    val matches: List<PlayerMatchRecord>
) {
    val homeMatches: List<PlayerMatchRecord> get() = matches.filter { it.isHome }
    val awayMatches: List<PlayerMatchRecord> get() = matches.filterNot { it.isHome }
}

data class PlayerMatchRecord(
    val jornada: Int,
    val date: String?,
    val opponentTeam: String,
    val sets: List<PlayerSetScore>,
    val won: Boolean,
    val isHome: Boolean
)

data class PlayerSetScore(val gamesWon: Int, val gamesLost: Int)
