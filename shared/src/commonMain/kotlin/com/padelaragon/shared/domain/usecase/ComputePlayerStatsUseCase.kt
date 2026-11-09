package com.padelaragon.shared.domain.usecase

import com.padelaragon.shared.data.model.MatchDetail
import com.padelaragon.shared.data.model.MatchResult
import com.padelaragon.shared.data.model.PlayerStats
import com.padelaragon.shared.data.model.PlayerStatsSplit

class ComputePlayerStatsUseCase {
    operator fun invoke(
        allDetails: Map<String, MatchDetail>,
        playedMatches: List<MatchResult>,
        teamId: Int
    ): List<PlayerStats> {
        data class SplitAccumulator(
            var wins: Int = 0,
            var losses: Int = 0,
            val pairCounts: MutableMap<Int, Int> = mutableMapOf()
        )

        data class Accumulator(
            var displayName: String = "",
            val home: SplitAccumulator = SplitAccumulator(),
            val away: SplitAccumulator = SplitAccumulator()
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
                    val split = if (isLocal) acc.home else acc.away
                    if (ourSideWon) split.wins++ else split.losses++
                    split.pairCounts[pair.pairNumber] = (split.pairCounts[pair.pairNumber] ?: 0) + 1
                }
            }
        }

        fun SplitAccumulator.toSplit() = PlayerStatsSplit(
            wins = wins,
            losses = losses,
            pair1Count = pairCounts[1] ?: 0,
            pair2Count = pairCounts[2] ?: 0,
            pair3Count = pairCounts[3] ?: 0
        )

        return statsMap.values
            .map {
                PlayerStats(
                    name = it.displayName,
                    home = it.home.toSplit(),
                    away = it.away.toSplit()
                )
            }
            .sortedWith(
                compareByDescending<PlayerStats> { it.home.wins + it.away.wins }
                    .thenBy { it.home.losses + it.away.losses }
            )
    }
}
