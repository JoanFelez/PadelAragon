package com.padelaragon.shared.data.local.entity

import androidx.room.Entity
import com.padelaragon.shared.data.model.Gender
import com.padelaragon.shared.data.model.LeagueGroup

@Entity(tableName = "league_groups", primaryKeys = ["leagueId", "id"])
data class LeagueGroupEntity(
    val leagueId: Int,
    val id: Int,
    val name: String,
    val gender: String,
    val category: String,
    val groupLetter: String?
) {
    fun toModel(): LeagueGroup = LeagueGroup(
        id = id,
        name = name,
        gender = Gender.valueOf(gender),
        category = category,
        groupLetter = groupLetter
    )

    companion object {
        fun fromModel(leagueId: Int, model: LeagueGroup): LeagueGroupEntity = LeagueGroupEntity(
            leagueId = leagueId,
            id = model.id,
            name = model.name,
            gender = model.gender.name,
            category = model.category,
            groupLetter = model.groupLetter
        )
    }
}
