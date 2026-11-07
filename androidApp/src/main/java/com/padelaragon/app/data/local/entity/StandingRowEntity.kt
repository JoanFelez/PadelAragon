package com.padelaragon.app.data.local.entity

import androidx.room.Entity
import com.padelaragon.app.data.model.StandingRow

@Entity(tableName = "standings", primaryKeys = ["groupId", "teamId"])
data class StandingRowEntity(
    val groupId: Int,
    val position: Int,
    val teamName: String,
    val teamId: Int,
    val teamHref: String,
    val points: Int,
    val matchesPlayed: Int,
    val encountersWon: Int,
    val encountersLost: Int,
    val matchesWon: Int,
    val matchesLost: Int,
    val setsWon: Int,
    val setsLost: Int,
    val gamesWon: Int,
    val gamesLost: Int
) {
    fun toModel(): StandingRow = StandingRow(
        position = position,
        teamName = teamName,
        teamId = teamId,
        teamHref = teamHref,
        points = points,
        matchesPlayed = matchesPlayed,
        encountersWon = encountersWon,
        encountersLost = encountersLost,
        matchesWon = matchesWon,
        matchesLost = matchesLost,
        setsWon = setsWon,
        setsLost = setsLost,
        gamesWon = gamesWon,
        gamesLost = gamesLost
    )

    companion object {
        fun fromModel(groupId: Int, model: StandingRow): StandingRowEntity = StandingRowEntity(
            groupId = groupId,
            position = model.position,
            teamName = model.teamName,
            teamId = model.teamId,
            teamHref = model.teamHref,
            points = model.points,
            matchesPlayed = model.matchesPlayed,
            encountersWon = model.encountersWon,
            encountersLost = model.encountersLost,
            matchesWon = model.matchesWon,
            matchesLost = model.matchesLost,
            setsWon = model.setsWon,
            setsLost = model.setsLost,
            gamesWon = model.gamesWon,
            gamesLost = model.gamesLost
        )
    }
}
