package com.padelaragon.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "jornadas", primaryKeys = ["leagueId", "groupId", "jornada"])
data class JornadaEntity(
    val leagueId: Int = 27951,
    val groupId: Int,
    val jornada: Int
)
