package com.padelaragon.shared.domain.usecase

import com.padelaragon.shared.data.model.MatchDetail
import com.padelaragon.shared.data.model.MatchResult
import com.padelaragon.shared.data.model.PairDetail
import com.padelaragon.shared.data.model.SetScore
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputePlayerDetailStatsUseCaseTest {

    private val useCase = ComputePlayerDetailStatsUseCase()

    private fun match(jornada: Int, localTeamId: Int, visitorTeamId: Int, detailUrl: String) = MatchResult(
        localTeam = "Local",
        localTeamId = localTeamId,
        visitorTeam = "Visitor",
        visitorTeamId = visitorTeamId,
        localScore = "2",
        visitorScore = "1",
        date = null,
        venue = null,
        jornada = jornada,
        detailUrl = detailUrl
    )

    @Test
    fun `sums games won and lost for the target player as local team`() {
        val teamId = 1
        val detailUrl = "match1"
        val detail = MatchDetail(
            pairs = listOf(
                PairDetail(
                    pairNumber = 1,
                    localPlayer1 = "Alice",
                    localPlayer2 = "Bob",
                    visitorPlayer1 = "Carol",
                    visitorPlayer2 = "Dave",
                    sets = listOf(SetScore(6, 4), SetScore(3, 6), SetScore(6, 2))
                )
            )
        )
        val playedMatches = listOf(match(jornada = 1, localTeamId = teamId, visitorTeamId = 2, detailUrl = detailUrl))

        val stats = useCase(mapOf(detailUrl to detail), playedMatches, teamId, "Alice")

        assertEquals(1, stats.matchesWon)
        assertEquals(0, stats.matchesLost)
        assertEquals(15, stats.gamesWon) // 6 + 3 + 6
        assertEquals(12, stats.gamesLost) // 4 + 6 + 2
        assertEquals(1, stats.topPartners.size)
        assertEquals("Bob", stats.topPartners.first().name)
        assertEquals(1, stats.topPartners.first().matchesTogether)

        assertEquals(1, stats.matches.size)
        val record = stats.matches.first()
        assertEquals(1, record.jornada)
        assertEquals("Bob", record.partnerName)
        assertEquals("Visitor", record.opponentTeam)
        assertEquals("Carol", record.opponentPlayer1)
        assertEquals("Dave", record.opponentPlayer2)
        assertEquals(listOf(6 to 4, 3 to 6, 6 to 2), record.sets.map { it.gamesWon to it.gamesLost })
        assertEquals(true, record.won)
        assertEquals(true, record.isHome)
        assertEquals(100.0, stats.winRate)
    }

    @Test
    fun `sums games for the target player as visitor team and swaps score sides`() {
        val teamId = 2
        val detailUrl = "match1"
        val detail = MatchDetail(
            pairs = listOf(
                PairDetail(
                    pairNumber = 1,
                    localPlayer1 = "Carol",
                    localPlayer2 = "Dave",
                    visitorPlayer1 = "Alice",
                    visitorPlayer2 = "Bob",
                    sets = listOf(SetScore(6, 4), SetScore(3, 6), SetScore(2, 6))
                )
            )
        )
        val playedMatches = listOf(match(jornada = 1, localTeamId = 1, visitorTeamId = teamId, detailUrl = detailUrl))

        val stats = useCase(mapOf(detailUrl to detail), playedMatches, teamId, "Alice")

        // Alice is on the visitor side, so games won/lost should use the visitor score column.
        assertEquals(1, stats.matchesWon)
        assertEquals(0, stats.matchesLost)
        assertEquals(16, stats.gamesWon) // 4 + 6 + 6
        assertEquals(11, stats.gamesLost) // 6 + 3 + 2

        assertEquals(1, stats.matches.size)
        val record = stats.matches.first()
        assertEquals("Bob", record.partnerName)
        assertEquals("Local", record.opponentTeam)
        assertEquals("Carol", record.opponentPlayer1)
        assertEquals("Dave", record.opponentPlayer2)
        assertEquals(listOf(4 to 6, 6 to 3, 6 to 2), record.sets.map { it.gamesWon to it.gamesLost })
        assertEquals(true, record.won)
        assertEquals(false, record.isHome)
        assertEquals(100.0, stats.winRate)
    }

    @Test
    fun `computes win rate as percentage of matches won`() {
        val teamId = 1
        val winDetail = MatchDetail(
            pairs = listOf(
                PairDetail(
                    pairNumber = 1,
                    localPlayer1 = "Alice",
                    localPlayer2 = "Bob",
                    visitorPlayer1 = "Carol",
                    visitorPlayer2 = "Dave",
                    sets = listOf(SetScore(6, 0), SetScore(6, 0))
                )
            )
        )
        val lossDetail = MatchDetail(
            pairs = listOf(
                PairDetail(
                    pairNumber = 1,
                    localPlayer1 = "Alice",
                    localPlayer2 = "Bob",
                    visitorPlayer1 = "Carol",
                    visitorPlayer2 = "Dave",
                    sets = listOf(SetScore(0, 6), SetScore(0, 6))
                )
            )
        )
        val playedMatches = listOf(
            match(jornada = 1, localTeamId = teamId, visitorTeamId = 2, detailUrl = "win"),
            match(jornada = 2, localTeamId = teamId, visitorTeamId = 2, detailUrl = "loss"),
            match(jornada = 3, localTeamId = teamId, visitorTeamId = 2, detailUrl = "win2")
        )

        val stats = useCase(
            mapOf("win" to winDetail, "loss" to lossDetail, "win2" to winDetail),
            playedMatches,
            teamId,
            "Alice"
        )

        assertEquals(3, stats.matches.size)
        assertEquals(2.0 / 3.0 * 100.0, stats.winRate!!, 0.001)
    }

    @Test
    fun `win rate is null when there are no matches`() {
        val stats = useCase(emptyMap(), emptyList(), 1, "Alice")

        assertEquals(null, stats.winRate)
        assertEquals(null, stats.gameWinRate)
    }

    @Test
    fun `computes game win rate as percentage of games won`() {
        val teamId = 1
        val detail = MatchDetail(
            pairs = listOf(
                PairDetail(
                    pairNumber = 1,
                    localPlayer1 = "Alice",
                    localPlayer2 = "Bob",
                    visitorPlayer1 = "Carol",
                    visitorPlayer2 = "Dave",
                    sets = listOf(SetScore(6, 4), SetScore(4, 6), SetScore(6, 0))
                )
            )
        )
        val playedMatches = listOf(match(jornada = 1, localTeamId = teamId, visitorTeamId = 2, detailUrl = "m1"))

        val stats = useCase(mapOf("m1" to detail), playedMatches, teamId, "Alice")

        // Games won: 6 + 4 + 6 = 16, games lost: 4 + 6 + 0 = 10, total 26
        assertEquals(16, stats.gamesWon)
        assertEquals(10, stats.gamesLost)
        assertEquals(16.0 / 26.0 * 100.0, stats.gameWinRate!!, 0.001)
    }

    @Test
    fun `orders match history by most recent jornada first`() {
        val teamId = 1
        fun detailForJornada() = MatchDetail(
            pairs = listOf(
                PairDetail(
                    pairNumber = 1,
                    localPlayer1 = "Alice",
                    localPlayer2 = "Bob",
                    visitorPlayer1 = "X",
                    visitorPlayer2 = "Y",
                    sets = listOf(SetScore(6, 0))
                )
            )
        )

        val allDetails = mapOf(
            "m1" to detailForJornada(),
            "m2" to detailForJornada(),
            "m3" to detailForJornada()
        )
        val playedMatches = listOf(
            match(jornada = 1, localTeamId = teamId, visitorTeamId = 99, detailUrl = "m1"),
            match(jornada = 3, localTeamId = teamId, visitorTeamId = 99, detailUrl = "m3"),
            match(jornada = 2, localTeamId = teamId, visitorTeamId = 99, detailUrl = "m2")
        )

        val stats = useCase(allDetails, playedMatches, teamId, "Alice")

        assertEquals(listOf(3, 2, 1), stats.matches.map { it.jornada })
    }

    @Test
    fun `ranks top 3 partners by frequency`() {
        val teamId = 1
        fun detailWithPartner(partner: String) = MatchDetail(
            pairs = listOf(
                PairDetail(
                    pairNumber = 1,
                    localPlayer1 = "Alice",
                    localPlayer2 = partner,
                    visitorPlayer1 = "X",
                    visitorPlayer2 = "Y",
                    sets = listOf(SetScore(6, 0))
                )
            )
        )

        val allDetails = mapOf(
            "m1" to detailWithPartner("Bob"),
            "m2" to detailWithPartner("Bob"),
            "m3" to detailWithPartner("Bob"),
            "m4" to detailWithPartner("Carol"),
            "m5" to detailWithPartner("Carol"),
            "m6" to detailWithPartner("Dave"),
            "m7" to detailWithPartner("Eve")
        )
        val playedMatches = allDetails.keys.mapIndexed { index, url ->
            match(jornada = index + 1, localTeamId = teamId, visitorTeamId = 99, detailUrl = url)
        }

        val stats = useCase(allDetails, playedMatches, teamId, "Alice")

        assertEquals(listOf("Bob", "Carol"), stats.topPartners.take(2).map { it.name })
        assertEquals(3, stats.topPartners.size)
        assertEquals(3, stats.topPartners[0].matchesTogether)
        assertEquals(2, stats.topPartners[1].matchesTogether)
    }
}
