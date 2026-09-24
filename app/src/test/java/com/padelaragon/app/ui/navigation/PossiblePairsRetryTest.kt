package com.padelaragon.app.ui.navigation

import com.padelaragon.app.data.model.Player
import com.padelaragon.app.data.model.TeamDetail
import com.padelaragon.app.data.model.TeamInfo
import com.padelaragon.app.data.repository.datasource.TeamDataSource
import com.padelaragon.app.ui.screen.PossiblePairsInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PossiblePairsRetryTest {
    @Test
    fun `reloads possible pairs in the original team context`() = runTest {
        val input = PossiblePairsInput(
            teamDetail = TeamDetail(players = listOf(Player("Stale"))),
            groupName = "Grupo anterior",
            teamId = 42,
            teamName = "Equipo",
            groupId = 7
        )
        val dataSource = RecordingTeamDataSource(
            TeamInfo(
                teamId = 42,
                teamName = "Equipo",
                groupName = "Grupo actualizado",
                groupId = 7,
                standing = null,
                matches = emptyList(),
                teamDetail = TeamDetail(players = listOf(Player("Actualizado")))
            )
        )

        val result = reloadPossiblePairsInput(input, dataSource)

        assertEquals(Triple(42, "Equipo", 7), dataSource.request)
        assertEquals("Grupo actualizado", result?.groupName)
        assertEquals(listOf("Actualizado"), result?.teamDetail?.players?.map(Player::name))
        assertEquals(42, result?.teamId)
        assertEquals("Equipo", result?.teamName)
        assertEquals(7, result?.groupId)
    }

    @Test
    fun `keeps the current possible pairs state when a retry has no roster`() = runTest {
        val input = PossiblePairsInput(
            teamDetail = TeamDetail(players = listOf(Player("Disponible"))),
            groupName = "Grupo",
            teamId = 42,
            teamName = "Equipo",
            groupId = 7
        )

        assertNull(reloadPossiblePairsInput(input, RecordingTeamDataSource(null)))
    }

    private class RecordingTeamDataSource(
        private val teamInfo: TeamInfo?
    ) : TeamDataSource {
        var request: Triple<Int, String, Int>? = null

        override suspend fun getTeamDetail(teamId: Int, teamHref: String): TeamDetail? = null

        override suspend fun getTeamInfo(teamId: Int, teamName: String): TeamInfo? = null

        override suspend fun getTeamInfoForGroup(
            teamId: Int,
            teamName: String,
            groupId: Int
        ): TeamInfo? {
            request = Triple(teamId, teamName, groupId)
            return teamInfo
        }
    }
}
