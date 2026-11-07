package com.padelaragon.shared.domain.usecase

import com.padelaragon.shared.data.model.MatchDetail
import com.padelaragon.shared.data.model.MatchResult
import com.padelaragon.shared.data.model.PartnerCount
import com.padelaragon.shared.data.model.PlayerDetailStats
import com.padelaragon.shared.data.model.PlayerMatchRecord
import com.padelaragon.shared.data.model.PlayerSetScore

/**
 * Desktop-only use case: computes total matches won/lost, total games
 * won/lost, and the top 3 most frequent partners for a single player,
 * across all of the team's played matches. Reuses the same match/detail
 * data already loaded for the team's player-stats tab, so no extra network
 * calls are required.
 */
class ComputePlayerDetailStatsUseCase {
    operator fun invoke(
        allDetails: Map<String, MatchDetail>,
        playedMatches: List<MatchResult>,
        teamId: Int,
        playerName: String
    ): PlayerDetailStats {
        val targetKey = playerName.trim().lowercase()
        var matchesWon = 0
        var matchesLost = 0
        var gamesWon = 0
        var gamesLost = 0
        val partnerCounts = mutableMapOf<String, Int>()
        val partnerDisplayNames = mutableMapOf<String, String>()
        val matches = mutableListOf<PlayerMatchRecord>()

        for (match in playedMatches) {
            val detail = allDetails[match.detailUrl] ?: continue
            val isLocal = match.localTeamId == teamId
            val opponentTeam = if (isLocal) match.visitorTeam else match.localTeam

            for (pair in detail.pairs) {
                if (pair.sets.isEmpty()) continue

                val player1 = if (isLocal) pair.localPlayer1 else pair.visitorPlayer1
                val player2 = if (isLocal) pair.localPlayer2 else pair.visitorPlayer2

                val isPlayer1 = player1.trim().lowercase() == targetKey
                val isPlayer2 = player2.trim().lowercase() == targetKey
                if (!isPlayer1 && !isPlayer2) continue

                var pairSetsWon = 0
                var pairSetsLost = 0
                val playerSets = mutableListOf<PlayerSetScore>()
                for (set in pair.sets) {
                    val setGamesWon = if (isLocal) set.localScore else set.visitorScore
                    val setGamesLost = if (isLocal) set.visitorScore else set.localScore
                    playerSets += PlayerSetScore(gamesWon = setGamesWon, gamesLost = setGamesLost)
                    if (setGamesWon > setGamesLost) pairSetsWon++ else if (setGamesLost > setGamesWon) pairSetsLost++
                    gamesWon += setGamesWon
                    gamesLost += setGamesLost
                }
                val pairWon = pairSetsWon > pairSetsLost
                if (pairWon) matchesWon++ else matchesLost++

                val partnerName = (if (isPlayer1) player2 else player1).trim()
                if (partnerName.isNotEmpty()) {
                    val key = partnerName.lowercase()
                    partnerCounts[key] = (partnerCounts[key] ?: 0) + 1
                    partnerDisplayNames.putIfAbsent(key, partnerName)
                }

                val opponentPlayer1 = if (isLocal) pair.visitorPlayer1 else pair.localPlayer1
                val opponentPlayer2 = if (isLocal) pair.visitorPlayer2 else pair.localPlayer2

                matches += PlayerMatchRecord(
                    jornada = match.jornada,
                    date = match.date,
                    partnerName = partnerName,
                    opponentTeam = opponentTeam,
                    opponentPlayer1 = opponentPlayer1,
                    opponentPlayer2 = opponentPlayer2,
                    sets = playerSets,
                    won = pairWon,
                    isHome = isLocal
                )
            }
        }

        val topPartners = partnerCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { (key, count) -> PartnerCount(name = partnerDisplayNames.getValue(key), matchesTogether = count) }

        return PlayerDetailStats(
            name = playerName,
            matchesWon = matchesWon,
            matchesLost = matchesLost,
            gamesWon = gamesWon,
            gamesLost = gamesLost,
            topPartners = topPartners,
            matches = matches.sortedByDescending { it.jornada }
        )
    }
}
