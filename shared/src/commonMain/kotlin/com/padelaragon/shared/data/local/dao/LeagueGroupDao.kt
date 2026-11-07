package com.padelaragon.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import com.padelaragon.shared.data.local.entity.LeagueGroupEntity

@Dao
interface LeagueGroupDao {
    @Query("SELECT * FROM league_groups WHERE leagueId = :leagueId")
    suspend fun getByLeagueId(leagueId: Int): List<LeagueGroupEntity>

    @Query("SELECT * FROM league_groups WHERE leagueId = :leagueId")
    fun observeByLeagueId(leagueId: Int): Flow<List<LeagueGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<LeagueGroupEntity>)

    @Query("DELETE FROM league_groups WHERE leagueId = :leagueId")
    suspend fun deleteByLeagueId(leagueId: Int)

    @Transaction
    suspend fun replaceForLeague(leagueId: Int, groups: List<LeagueGroupEntity>) {
        deleteByLeagueId(leagueId)
        insertAll(groups)
    }
}
