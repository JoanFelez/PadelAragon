package com.padelaragon.shared.data.local

import androidx.room.Database
import androidx.room.ConstructedBy
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import com.padelaragon.shared.data.local.dao.BundleImportDao
import com.padelaragon.shared.data.local.dao.CacheTimestampDao
import com.padelaragon.shared.data.local.dao.JornadaDao
import com.padelaragon.shared.data.local.dao.LeagueGroupDao
import com.padelaragon.shared.data.local.dao.MatchDetailDao
import com.padelaragon.shared.data.local.dao.MatchResultDao
import com.padelaragon.shared.data.local.dao.StandingRowDao
import com.padelaragon.shared.data.local.dao.TeamDetailDao
import com.padelaragon.shared.data.local.entity.CacheTimestamp
import com.padelaragon.shared.data.local.entity.JornadaEntity
import com.padelaragon.shared.data.local.entity.LeagueGroupEntity
import com.padelaragon.shared.data.local.entity.MatchDetailPairEntity
import com.padelaragon.shared.data.local.entity.MatchResultEntity
import com.padelaragon.shared.data.local.entity.PlayerEntity
import com.padelaragon.shared.data.local.entity.StandingRowEntity
import com.padelaragon.shared.data.local.entity.TeamDetailEntity

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
    version = 5,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leagueGroupDao(): LeagueGroupDao
    abstract fun standingRowDao(): StandingRowDao
    abstract fun matchResultDao(): MatchResultDao
    abstract fun matchDetailDao(): MatchDetailDao
    abstract fun teamDetailDao(): TeamDetailDao
    abstract fun jornadaDao(): JornadaDao
    abstract fun cacheTimestampDao(): CacheTimestampDao
    abstract fun bundleImportDao(): BundleImportDao
}

// Room's KSP processor generates the actual implementation of this constructor.
@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/** Platform-specific handle needed to locate the database file (Android: Context; desktop: data dir). */
expect class PlatformContext

expect fun appDatabaseBuilder(context: PlatformContext): RoomDatabase.Builder<AppDatabase>

/** The cache is disposable: any schema change simply rebuilds it from the network. */
fun buildAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
