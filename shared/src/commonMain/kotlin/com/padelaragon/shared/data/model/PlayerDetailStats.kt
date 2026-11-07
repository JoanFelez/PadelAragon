package com.padelaragon.shared.data.model

/**
 * Detailed desktop-only statistics for a single player: total matches
 * won/lost, total games won/lost across all sets, and the teammates
 * they've partnered with most often.
 */
data class PlayerDetailStats(
    val name: String,
    val matchesWon: Int,
    val matchesLost: Int,
    val gamesWon: Int,
    val gamesLost: Int,
    val topPartners: List<PartnerCount>,
    val matches: List<PlayerMatchRecord> = emptyList()
) {
    /** Percentage of matches won (0-100), or null if the player has no matches. */
    val winRate: Double?
        get() = if (matches.isEmpty()) null else (matches.count { it.won } * 100.0) / matches.size

    /** Percentage of games won (0-100), or null if the player has no games played. */
    val gameWinRate: Double?
        get() {
            val totalGames = gamesWon + gamesLost
            return if (totalGames == 0) null else (gamesWon * 100.0) / totalGames
        }
}

data class PartnerCount(
    val name: String,
    val matchesTogether: Int
)

/**
 * A single set score from the player's own side of the court.
 */
data class PlayerSetScore(
    val gamesWon: Int,
    val gamesLost: Int
)

/**
 * A single match the player took part in, with the opposing team/players and
 * the set-by-set result from the player's own side of the court.
 */
data class PlayerMatchRecord(
    val jornada: Int,
    val date: String?,
    val partnerName: String,
    val opponentTeam: String,
    val opponentPlayer1: String,
    val opponentPlayer2: String,
    val sets: List<PlayerSetScore>,
    val won: Boolean,
    val isHome: Boolean
)
