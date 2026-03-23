package com.padelaragon.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_details")
data class TeamDetailEntity(
    @PrimaryKey val teamId: Int,
    val category: String?,
    val captainName: String?
)
