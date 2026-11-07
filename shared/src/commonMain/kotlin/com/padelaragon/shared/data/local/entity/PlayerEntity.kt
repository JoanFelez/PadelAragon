package com.padelaragon.shared.data.local.entity

import androidx.room.Entity
import com.padelaragon.shared.data.model.Player

@Entity(tableName = "players", primaryKeys = ["leagueId", "teamId", "name"])
data class PlayerEntity(
    val leagueId: Int,
    val teamId: Int,
    val name: String,
    val isCaptain: Boolean,
    val points: String?,
    val birthYear: String?
) {
    fun toModel(): Player = Player(
        name = name,
        isCaptain = isCaptain,
        points = points,
        birthYear = birthYear
    )

    companion object {
        fun fromModel(leagueId: Int, teamId: Int, model: Player): PlayerEntity = PlayerEntity(
            leagueId = leagueId,
            teamId = teamId,
            name = model.name,
            isCaptain = model.isCaptain,
            points = model.points,
            birthYear = model.birthYear
        )
    }
}
