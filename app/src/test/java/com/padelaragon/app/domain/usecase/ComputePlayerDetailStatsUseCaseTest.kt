package com.padelaragon.app.domain.usecase

import com.padelaragon.app.data.model.MatchDetail
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.model.PairDetail
import com.padelaragon.app.data.model.SetScore
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputePlayerDetailStatsUseCaseTest {
    @Test
    fun `classifies home and away records using the selected team score orientation`() {
        val home = MatchResult("Us", 1, "Them", 2, "3", "0", null, null, 1, "home")
        val away = MatchResult("Them", 2, "Us", 1, "0", "3", null, null, 2, "away")
        val detail = MatchDetail(listOf(
            PairDetail(1, "Ana", "B", "C", "D", listOf(SetScore(6, 3), SetScore(6, 4)))
        ))
        val awayDetail = MatchDetail(listOf(
            PairDetail(1, "C", "D", "Ana", "B", listOf(SetScore(3, 6), SetScore(4, 6)))
        ))

        val stats = ComputePlayerDetailStatsUseCase()(
            mapOf("home" to detail, "away" to awayDetail), listOf(home, away), 1, "Ana"
        )

        assertEquals(2, stats.matchesWon)
        assertEquals(0, stats.matchesLost)
        assertEquals(1, stats.homeMatches.size)
        assertEquals(1, stats.awayMatches.size)
        assertEquals(12, stats.awayMatches.single().sets.sumOf { it.gamesWon })
    }
}
