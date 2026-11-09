package com.padelaragon.shared.data.local.entity

import androidx.room.Entity

@Entity(tableName = "jornadas", primaryKeys = ["leagueId", "groupId", "jornada"])
data class JornadaEntity(
    val leagueId: Int,
    val groupId: Int,
    val jornada: Int
)
