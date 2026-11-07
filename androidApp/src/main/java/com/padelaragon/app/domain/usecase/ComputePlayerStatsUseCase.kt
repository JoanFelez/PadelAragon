package com.padelaragon.app.domain.usecase

import com.padelaragon.app.data.model.MatchDetail
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.model.PlayerStats

class ComputePlayerStatsUseCase {
    operator fun invoke(
        allDetails: Map<String, MatchDetail>,
        playedMatches: List<MatchResult>,
        teamId: Int
    ): List<PlayerStats> {
        data class Accumulator(
            var wins: Int = 0,
            var losses: Int = 0,
            val pairCounts: MutableMap<Int, Int> = mutableMapOf(),
            var displayName: String = ""
        )

        val statsMap = mutableMapOf<String, Accumulator>()

        for (match in playedMatches) {
            val detail = allDetails[match.detailUrl] ?: continue
            val isLocal = match.localTeamId == teamId

            for (pair in detail.pairs) {
                if (pair.sets.isEmpty()) continue

                val localSetsWon = pair.sets.count { it.localScore > it.visitorScore }
                val visitorSetsWon = pair.sets.count { it.visitorScore > it.localScore }

                if (localSetsWon == visitorSetsWon) continue

                val localWins = localSetsWon > visitorSetsWon
                val ourSideWon = if (isLocal) localWins else !localWins

                val player1 = if (isLocal) pair.localPlayer1 else pair.visitorPlayer1
                val player2 = if (isLocal) pair.localPlayer2 else pair.visitorPlayer2

                for (playerName in listOf(player1, player2)) {
                    val trimmed = playerName.trim()
                    if (trimmed.isEmpty()) continue
                    val key = trimmed.lowercase()

                    val acc = statsMap.getOrPut(key) { Accumulator(displayName = trimmed) }
                    if (ourSideWon) acc.wins++ else acc.losses++
                    acc.pairCounts[pair.pairNumber] = (acc.pairCounts[pair.pairNumber] ?: 0) + 1
                }
            }
        }

        return statsMap.values
            .map {
                PlayerStats(
                    name = it.displayName,
                    wins = it.wins,
                    losses = it.losses,
                    pair1Count = it.pairCounts[1] ?: 0,
                    pair2Count = it.pairCounts[2] ?: 0,
                    pair3Count = it.pairCounts[3] ?: 0
                )
            }
            .sortedWith(compareByDescending<PlayerStats> { it.wins }.thenBy { it.losses })
    }
}
