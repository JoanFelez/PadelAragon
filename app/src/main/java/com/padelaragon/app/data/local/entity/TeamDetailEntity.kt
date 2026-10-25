package com.padelaragon.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_details", primaryKeys = ["leagueId", "teamId"])
data class TeamDetailEntity(
    val leagueId: Int = 27951,
    val teamId: Int,
    val category: String?,
    val captainName: String?
)
