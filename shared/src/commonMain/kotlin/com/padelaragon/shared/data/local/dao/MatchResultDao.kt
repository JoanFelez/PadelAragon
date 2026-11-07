package com.padelaragon.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import com.padelaragon.shared.data.local.entity.MatchResultEntity

@Dao
interface MatchResultDao {
    @Query("SELECT * FROM match_results WHERE leagueId = :leagueId AND groupId = :groupId AND jornada = :jornada")
    suspend fun getByGroupAndJornada(leagueId: Int, groupId: Int, jornada: Int): List<MatchResultEntity>

    @Query("SELECT * FROM match_results WHERE leagueId = :leagueId AND groupId = :groupId")
    suspend fun getByGroupId(leagueId: Int, groupId: Int): List<MatchResultEntity>

    @Query("SELECT * FROM match_results WHERE leagueId = :leagueId AND groupId = :groupId")
    fun observeByGroupId(leagueId: Int, groupId: Int): Flow<List<MatchResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<MatchResultEntity>)

    @Query("DELETE FROM match_results WHERE leagueId = :leagueId AND groupId = :groupId AND jornada = :jornada")
    suspend fun deleteByGroupAndJornada(leagueId: Int, groupId: Int, jornada: Int)

    @Query("DELETE FROM match_results WHERE leagueId = :leagueId AND groupId = :groupId")
    suspend fun deleteByGroupId(leagueId: Int, groupId: Int)

    @Transaction
    suspend fun replaceForJornada(leagueId: Int, groupId: Int, jornada: Int, results: List<MatchResultEntity>) {
        deleteByGroupAndJornada(leagueId, groupId, jornada)
        insertAll(results)
    }
}
