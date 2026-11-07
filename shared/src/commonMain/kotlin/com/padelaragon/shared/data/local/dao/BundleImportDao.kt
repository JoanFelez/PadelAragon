package com.padelaragon.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.padelaragon.shared.data.local.entity.CacheTimestamp
import com.padelaragon.shared.data.local.entity.JornadaEntity
import com.padelaragon.shared.data.local.entity.LeagueGroupEntity
import com.padelaragon.shared.data.local.entity.MatchDetailPairEntity
import com.padelaragon.shared.data.local.entity.MatchResultEntity
import com.padelaragon.shared.data.local.entity.PlayerEntity
import com.padelaragon.shared.data.local.entity.StandingRowEntity
import com.padelaragon.shared.data.local.entity.TeamDetailEntity

/** Rows of one league bundle, already mapped to Room entities. */
class LeagueImport(
    val leagueId: Int,
    val groups: List<LeagueGroupEntity>,
    val standings: List<StandingRowEntity>,
    val jornadas: List<JornadaEntity>,
    val results: List<MatchResultEntity>,
    val teams: List<TeamDetailEntity>,
    val players: List<PlayerEntity>,
    val matchPairs: List<MatchDetailPairEntity>,
    val timestamps: List<CacheTimestamp>
)

@Dao
interface BundleImportDao {
    @Query(
        "DELETE FROM match_detail_pairs WHERE detailUrl IN " +
            "(SELECT detailUrl FROM match_results WHERE leagueId = :leagueId AND detailUrl IS NOT NULL)"
    )
    suspend fun deleteMatchPairsOfLeague(leagueId: Int)

    @Query("DELETE FROM league_groups WHERE leagueId = :leagueId")
    suspend fun deleteGroups(leagueId: Int)

    @Query("DELETE FROM standings WHERE leagueId = :leagueId")
    suspend fun deleteStandings(leagueId: Int)

    @Query("DELETE FROM jornadas WHERE leagueId = :leagueId")
    suspend fun deleteJornadas(leagueId: Int)

    @Query("DELETE FROM match_results WHERE leagueId = :leagueId")
    suspend fun deleteResults(leagueId: Int)

    @Query("DELETE FROM team_details WHERE leagueId = :leagueId")
    suspend fun deleteTeams(leagueId: Int)

    @Query("DELETE FROM players WHERE leagueId = :leagueId")
    suspend fun deletePlayers(leagueId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(rows: List<LeagueGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandings(rows: List<StandingRowEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJornadas(rows: List<JornadaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(rows: List<MatchResultEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeams(rows: List<TeamDetailEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(rows: List<PlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchPairs(rows: List<MatchDetailPairEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimestamps(rows: List<CacheTimestamp>)

    /** Replaces everything stored for the league atomically: readers never see a half-imported bundle. */
    @Transaction
    suspend fun replaceLeague(data: LeagueImport) {
        deleteMatchPairsOfLeague(data.leagueId)
        deleteGroups(data.leagueId)
        deleteStandings(data.leagueId)
        deleteJornadas(data.leagueId)
        deleteResults(data.leagueId)
        deleteTeams(data.leagueId)
        deletePlayers(data.leagueId)
        insertGroups(data.groups)
        insertStandings(data.standings)
        insertJornadas(data.jornadas)
        insertResults(data.results)
        insertTeams(data.teams)
        insertPlayers(data.players)
        data.matchPairs.chunked(500).forEach { insertMatchPairs(it) }
        insertTimestamps(data.timestamps)
    }
}
