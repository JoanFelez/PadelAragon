package com.padelaragon.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "jornadas", primaryKeys = ["groupId", "jornada"])
data class JornadaEntity(
    val groupId: Int,
    val jornada: Int
)
