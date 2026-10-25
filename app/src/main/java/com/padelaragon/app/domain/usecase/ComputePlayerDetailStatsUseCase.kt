package com.padelaragon.app.domain.usecase

import com.padelaragon.app.data.model.MatchDetail
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.model.PlayerDetailStats
import com.padelaragon.app.data.model.PlayerMatchRecord
import com.padelaragon.app.data.model.PlayerSetScore

class ComputePlayerDetailStatsUseCase {
    operator fun invoke(
        details: Map<String, MatchDetail>,
        playedMatches: List<MatchResult>,
        teamId: Int,
        playerName: String
    ): PlayerDetailStats {
        val target = playerName.trim().lowercase()
        var won = 0
        var lost = 0
        var gamesWon = 0
        var gamesLost = 0
        val records = mutableListOf<PlayerMatchRecord>()

        playedMatches.forEach { match ->
            val detail = match.detailUrl?.let(details::get) ?: return@forEach
            val isHome = match.localTeamId == teamId
            detail.pairs.forEach { pair ->
                if (pair.sets.isEmpty()) return@forEach
                val players = if (isHome) {
                    listOf(pair.localPlayer1, pair.localPlayer2)
                } else {
                    listOf(pair.visitorPlayer1, pair.visitorPlayer2)
                }
                if (players.none { it.trim().lowercase() == target }) return@forEach

                val scores = pair.sets.map { set ->
                    if (isHome) PlayerSetScore(set.localScore, set.visitorScore)
                    else PlayerSetScore(set.visitorScore, set.localScore)
                }
                val pairWon = scores.count { it.gamesWon > it.gamesLost } >
                    scores.count { it.gamesLost > it.gamesWon }
                if (pairWon) won++ else lost++
                gamesWon += scores.sumOf { it.gamesWon }
                gamesLost += scores.sumOf { it.gamesLost }
                records += PlayerMatchRecord(
                    jornada = match.jornada,
                    date = match.date,
                    opponentTeam = if (isHome) match.visitorTeam else match.localTeam,
                    sets = scores,
                    won = pairWon,
                    isHome = isHome
                )
            }
        }
        return PlayerDetailStats(
            playerName, won, lost, gamesWon, gamesLost,
            records.sortedByDescending { it.jornada }
        )
    }
}
