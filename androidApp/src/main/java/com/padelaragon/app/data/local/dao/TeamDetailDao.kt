package com.padelaragon.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.padelaragon.app.data.local.entity.PlayerEntity
import com.padelaragon.app.data.local.entity.TeamDetailEntity

@Dao
interface TeamDetailDao {
    @Query("SELECT * FROM team_details WHERE teamId = :teamId")
    suspend fun getByTeamId(teamId: Int): TeamDetailEntity?

    @Query("SELECT * FROM players WHERE teamId = :teamId")
    suspend fun getPlayersByTeamId(teamId: Int): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamDetail(detail: TeamDetailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Query("DELETE FROM players WHERE teamId = :teamId")
    suspend fun deletePlayersByTeamId(teamId: Int)

    @Transaction
    suspend fun insertTeamWithPlayers(detail: TeamDetailEntity, players: List<PlayerEntity>) {
        insertTeamDetail(detail)
        deletePlayersByTeamId(detail.teamId)
        insertPlayers(players)
    }
}
