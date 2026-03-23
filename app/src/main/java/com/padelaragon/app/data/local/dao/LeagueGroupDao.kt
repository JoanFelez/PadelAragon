package com.padelaragon.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.padelaragon.app.data.local.entity.LeagueGroupEntity

@Dao
interface LeagueGroupDao {
    @Query("SELECT * FROM league_groups")
    suspend fun getAll(): List<LeagueGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<LeagueGroupEntity>)

    @Query("DELETE FROM league_groups")
    suspend fun deleteAll()
}
