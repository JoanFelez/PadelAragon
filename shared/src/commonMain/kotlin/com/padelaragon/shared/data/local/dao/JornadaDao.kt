package com.padelaragon.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import com.padelaragon.shared.data.local.entity.JornadaEntity

@Dao
interface JornadaDao {
    @Query("SELECT jornada FROM jornadas WHERE leagueId = :leagueId AND groupId = :groupId ORDER BY jornada ASC")
    suspend fun getByGroupId(leagueId: Int, groupId: Int): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jornadas: List<JornadaEntity>)

    @Query("DELETE FROM jornadas WHERE leagueId = :leagueId AND groupId = :groupId")
    suspend fun deleteByGroupId(leagueId: Int, groupId: Int)

    @Transaction
    suspend fun replaceForGroup(leagueId: Int, groupId: Int, jornadas: List<JornadaEntity>) {
        deleteByGroupId(leagueId, groupId)
        insertAll(jornadas)
    }
}
