package com.padelaragon.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.padelaragon.app.data.local.entity.LeagueGroupEntity

@Dao
interface LeagueGroupDao {
    @Query("SELECT * FROM league_groups WHERE leagueId = :leagueId")
    suspend fun getAll(leagueId: Int): List<LeagueGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<LeagueGroupEntity>)

    @Query("DELETE FROM league_groups WHERE leagueId = :leagueId")
    suspend fun deleteAll(leagueId: Int)
}
