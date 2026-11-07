package com.padelaragon.shared.data.local.entity

import androidx.room.Entity

@Entity(tableName = "team_details", primaryKeys = ["leagueId", "teamId"])
data class TeamDetailEntity(
    val leagueId: Int,
    val teamId: Int,
    val category: String?,
    val captainName: String?
)
