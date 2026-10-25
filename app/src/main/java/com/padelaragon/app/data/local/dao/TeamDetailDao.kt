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
    @Query("SELECT * FROM team_details WHERE leagueId = :leagueId AND teamId = :teamId")
    suspend fun getByTeamId(leagueId: Int, teamId: Int): TeamDetailEntity?

    @Query("SELECT * FROM players WHERE leagueId = :leagueId AND teamId = :teamId")
    suspend fun getPlayersByTeamId(leagueId: Int, teamId: Int): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamDetail(detail: TeamDetailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Query("DELETE FROM players WHERE leagueId = :leagueId AND teamId = :teamId")
    suspend fun deletePlayersByTeamId(leagueId: Int, teamId: Int)

    @Transaction
    suspend fun insertTeamWithPlayers(detail: TeamDetailEntity, players: List<PlayerEntity>) {
        insertTeamDetail(detail)
        deletePlayersByTeamId(detail.leagueId, detail.teamId)
        insertPlayers(players)
    }
}
