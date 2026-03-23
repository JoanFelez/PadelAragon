package com.padelaragon.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.padelaragon.app.data.local.dao.CacheTimestampDao
import com.padelaragon.app.data.local.dao.JornadaDao
import com.padelaragon.app.data.local.dao.LeagueGroupDao
import com.padelaragon.app.data.local.dao.MatchResultDao
import com.padelaragon.app.data.local.dao.StandingRowDao
import com.padelaragon.app.data.local.dao.TeamDetailDao
import com.padelaragon.app.data.local.entity.CacheTimestamp
import com.padelaragon.app.data.local.entity.JornadaEntity
import com.padelaragon.app.data.local.entity.LeagueGroupEntity
import com.padelaragon.app.data.local.entity.MatchResultEntity
import com.padelaragon.app.data.local.entity.PlayerEntity
import com.padelaragon.app.data.local.entity.StandingRowEntity
import com.padelaragon.app.data.local.entity.TeamDetailEntity

@Database(
    entities = [
        LeagueGroupEntity::class,
        StandingRowEntity::class,
        MatchResultEntity::class,
        TeamDetailEntity::class,
        PlayerEntity::class,
        JornadaEntity::class,
        CacheTimestamp::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leagueGroupDao(): LeagueGroupDao
    abstract fun standingRowDao(): StandingRowDao
    abstract fun matchResultDao(): MatchResultDao
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
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
