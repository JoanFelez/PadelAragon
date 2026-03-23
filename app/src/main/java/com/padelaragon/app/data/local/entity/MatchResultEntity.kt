package com.padelaragon.app.data.local.entity

import androidx.room.Entity
import com.padelaragon.app.data.model.MatchResult

@Entity(tableName = "match_results", primaryKeys = ["groupId", "jornada", "localTeamId", "visitorTeamId"])
data class MatchResultEntity(
    val groupId: Int,
    val localTeam: String,
    val localTeamId: Int,
    val visitorTeam: String,
    val visitorTeamId: Int,
    val localScore: String,
    val visitorScore: String,
    val date: String?,
    val venue: String?,
    val jornada: Int,
    val detailUrl: String? = null
) {
    fun toModel(): MatchResult = MatchResult(
        localTeam = localTeam,
        localTeamId = localTeamId,
        visitorTeam = visitorTeam,
        visitorTeamId = visitorTeamId,
        localScore = localScore,
        visitorScore = visitorScore,
        date = date,
        venue = venue,
        jornada = jornada,
        detailUrl = detailUrl
    )

    companion object {
        fun fromModel(groupId: Int, model: MatchResult): MatchResultEntity = MatchResultEntity(
            groupId = groupId,
            localTeam = model.localTeam,
            localTeamId = model.localTeamId,
            visitorTeam = model.visitorTeam,
            visitorTeamId = model.visitorTeamId,
            localScore = model.localScore,
            visitorScore = model.visitorScore,
            date = model.date,
            venue = model.venue,
            jornada = model.jornada,
            detailUrl = model.detailUrl
        )
    }
}
