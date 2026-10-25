package com.padelaragon.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.padelaragon.app.data.local.entity.StandingRowEntity

@Dao
interface StandingRowDao {
    @Query("SELECT * FROM standings WHERE leagueId = :leagueId AND groupId = :groupId ORDER BY position ASC")
    suspend fun getByGroupId(leagueId: Int, groupId: Int): List<StandingRowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(standings: List<StandingRowEntity>)

    @Query("DELETE FROM standings WHERE leagueId = :leagueId AND groupId = :groupId")
    suspend fun deleteByGroupId(leagueId: Int, groupId: Int)
}
