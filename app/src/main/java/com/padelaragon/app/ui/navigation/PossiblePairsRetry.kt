package com.padelaragon.app.ui.navigation

import com.padelaragon.app.data.repository.datasource.TeamDataSource
import com.padelaragon.app.ui.screen.PossiblePairsInput

internal suspend fun reloadPossiblePairsInput(
    input: PossiblePairsInput,
    teamDataSource: TeamDataSource
): PossiblePairsInput? {
    val teamInfo = teamDataSource.getTeamInfoForGroup(
        input.teamId,
        input.teamName,
        input.groupId
    ) ?: return null
    val teamDetail = teamInfo.teamDetail ?: return null

    return input.copy(teamDetail = teamDetail, groupName = teamInfo.groupName)
}
