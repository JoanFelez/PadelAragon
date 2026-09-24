package com.padelaragon.app.data.local.entity

import androidx.room.Entity
import com.padelaragon.app.data.model.Player

@Entity(tableName = "players", primaryKeys = ["leagueId", "teamId", "name"])
data class PlayerEntity(
    val leagueId: Int = 27951,
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
        fun fromModel(teamId: Int, model: Player): PlayerEntity = fromModel(27951, teamId, model)
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
