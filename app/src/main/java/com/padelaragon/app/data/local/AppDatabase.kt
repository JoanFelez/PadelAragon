package com.padelaragon.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.padelaragon.app.data.local.dao.CacheTimestampDao
import com.padelaragon.app.data.local.dao.JornadaDao
import com.padelaragon.app.data.local.dao.LeagueGroupDao
import com.padelaragon.app.data.local.dao.MatchDetailDao
import com.padelaragon.app.data.local.dao.MatchResultDao
import com.padelaragon.app.data.local.dao.StandingRowDao
import com.padelaragon.app.data.local.dao.TeamDetailDao
import com.padelaragon.app.data.local.entity.CacheTimestamp
import com.padelaragon.app.data.local.entity.JornadaEntity
import com.padelaragon.app.data.local.entity.LeagueGroupEntity
import com.padelaragon.app.data.local.entity.MatchDetailPairEntity
import com.padelaragon.app.data.local.entity.MatchResultEntity
import com.padelaragon.app.data.local.entity.PlayerEntity
import com.padelaragon.app.data.local.entity.StandingRowEntity
import com.padelaragon.app.data.local.entity.TeamDetailEntity

@Database(
    entities = [
        LeagueGroupEntity::class,
        StandingRowEntity::class,
        MatchResultEntity::class,
        MatchDetailPairEntity::class,
        TeamDetailEntity::class,
        PlayerEntity::class,
        JornadaEntity::class,
        CacheTimestamp::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leagueGroupDao(): LeagueGroupDao
    abstract fun standingRowDao(): StandingRowDao
    abstract fun matchResultDao(): MatchResultDao
    abstract fun matchDetailDao(): MatchDetailDao
    abstract fun teamDetailDao(): TeamDetailDao
    abstract fun jornadaDao(): JornadaDao
    abstract fun cacheTimestampDao(): CacheTimestampDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "padel_aragon.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    listOf(
                        "league_groups", "standings", "match_results", "match_detail_pairs",
                        "team_details", "players", "jornadas"
                    ).forEach { db.execSQL("ALTER TABLE $it RENAME TO ${it}_v3") }
                    db.execSQL("CREATE TABLE league_groups (leagueId INTEGER NOT NULL, id INTEGER NOT NULL, name TEXT NOT NULL, gender TEXT NOT NULL, category TEXT NOT NULL, groupLetter TEXT, PRIMARY KEY(leagueId, id))")
                    db.execSQL("CREATE TABLE standings (leagueId INTEGER NOT NULL, groupId INTEGER NOT NULL, position INTEGER NOT NULL, teamName TEXT NOT NULL, teamId INTEGER NOT NULL, teamHref TEXT NOT NULL, points INTEGER NOT NULL, matchesPlayed INTEGER NOT NULL, encountersWon INTEGER NOT NULL, encountersLost INTEGER NOT NULL, matchesWon INTEGER NOT NULL, matchesLost INTEGER NOT NULL, setsWon INTEGER NOT NULL, setsLost INTEGER NOT NULL, gamesWon INTEGER NOT NULL, gamesLost INTEGER NOT NULL, PRIMARY KEY(leagueId, groupId, teamId))")
                    db.execSQL("CREATE TABLE match_results (leagueId INTEGER NOT NULL, groupId INTEGER NOT NULL, localTeam TEXT NOT NULL, localTeamId INTEGER NOT NULL, visitorTeam TEXT NOT NULL, visitorTeamId INTEGER NOT NULL, localScore TEXT NOT NULL, visitorScore TEXT NOT NULL, date TEXT, venue TEXT, jornada INTEGER NOT NULL, detailUrl TEXT, PRIMARY KEY(leagueId, groupId, jornada, localTeamId, visitorTeamId))")
                    db.execSQL("CREATE TABLE match_detail_pairs (leagueId INTEGER NOT NULL, detailUrl TEXT NOT NULL, pairNumber INTEGER NOT NULL, localPlayer1 TEXT NOT NULL, localPlayer2 TEXT NOT NULL, visitorPlayer1 TEXT NOT NULL, visitorPlayer2 TEXT NOT NULL, set1Local INTEGER, set1Visitor INTEGER, set2Local INTEGER, set2Visitor INTEGER, set3Local INTEGER, set3Visitor INTEGER, PRIMARY KEY(leagueId, detailUrl, pairNumber))")
                    db.execSQL("CREATE TABLE team_details (leagueId INTEGER NOT NULL, teamId INTEGER NOT NULL, category TEXT, captainName TEXT, PRIMARY KEY(leagueId, teamId))")
                    db.execSQL("CREATE TABLE players (leagueId INTEGER NOT NULL, teamId INTEGER NOT NULL, name TEXT NOT NULL, isCaptain INTEGER NOT NULL, points TEXT, birthYear TEXT, PRIMARY KEY(leagueId, teamId, name))")
                    db.execSQL("CREATE TABLE jornadas (leagueId INTEGER NOT NULL, groupId INTEGER NOT NULL, jornada INTEGER NOT NULL, PRIMARY KEY(leagueId, groupId, jornada))")
                    listOf(
                        "league_groups", "standings", "match_results", "match_detail_pairs",
                        "team_details", "players", "jornadas"
                    ).forEach { table ->
                        db.execSQL("INSERT INTO $table SELECT 27951, * FROM ${table}_v3")
                        db.execSQL("DROP TABLE ${table}_v3")
                }
            }
        }
    }
}
