package com.padelaragon.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.padelaragon.app.data.local.dao.CacheTimestampDao;
import com.padelaragon.app.data.local.dao.CacheTimestampDao_Impl;
import com.padelaragon.app.data.local.dao.JornadaDao;
import com.padelaragon.app.data.local.dao.JornadaDao_Impl;
import com.padelaragon.app.data.local.dao.LeagueGroupDao;
import com.padelaragon.app.data.local.dao.LeagueGroupDao_Impl;
import com.padelaragon.app.data.local.dao.MatchDetailDao;
import com.padelaragon.app.data.local.dao.MatchDetailDao_Impl;
import com.padelaragon.app.data.local.dao.MatchResultDao;
import com.padelaragon.app.data.local.dao.MatchResultDao_Impl;
import com.padelaragon.app.data.local.dao.StandingRowDao;
import com.padelaragon.app.data.local.dao.StandingRowDao_Impl;
import com.padelaragon.app.data.local.dao.TeamDetailDao;
import com.padelaragon.app.data.local.dao.TeamDetailDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile LeagueGroupDao _leagueGroupDao;

  private volatile StandingRowDao _standingRowDao;

  private volatile MatchResultDao _matchResultDao;

  private volatile MatchDetailDao _matchDetailDao;

  private volatile TeamDetailDao _teamDetailDao;

  private volatile JornadaDao _jornadaDao;

  private volatile CacheTimestampDao _cacheTimestampDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `league_groups` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `gender` TEXT NOT NULL, `category` TEXT NOT NULL, `groupLetter` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `standings` (`groupId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `teamName` TEXT NOT NULL, `teamId` INTEGER NOT NULL, `teamHref` TEXT NOT NULL, `points` INTEGER NOT NULL, `matchesPlayed` INTEGER NOT NULL, `encountersWon` INTEGER NOT NULL, `encountersLost` INTEGER NOT NULL, `matchesWon` INTEGER NOT NULL, `matchesLost` INTEGER NOT NULL, `setsWon` INTEGER NOT NULL, `setsLost` INTEGER NOT NULL, `gamesWon` INTEGER NOT NULL, `gamesLost` INTEGER NOT NULL, PRIMARY KEY(`groupId`, `teamId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `match_results` (`groupId` INTEGER NOT NULL, `localTeam` TEXT NOT NULL, `localTeamId` INTEGER NOT NULL, `visitorTeam` TEXT NOT NULL, `visitorTeamId` INTEGER NOT NULL, `localScore` TEXT NOT NULL, `visitorScore` TEXT NOT NULL, `date` TEXT, `venue` TEXT, `jornada` INTEGER NOT NULL, `detailUrl` TEXT, PRIMARY KEY(`groupId`, `jornada`, `localTeamId`, `visitorTeamId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `match_detail_pairs` (`detailUrl` TEXT NOT NULL, `pairNumber` INTEGER NOT NULL, `localPlayer1` TEXT NOT NULL, `localPlayer2` TEXT NOT NULL, `visitorPlayer1` TEXT NOT NULL, `visitorPlayer2` TEXT NOT NULL, `set1Local` INTEGER, `set1Visitor` INTEGER, `set2Local` INTEGER, `set2Visitor` INTEGER, `set3Local` INTEGER, `set3Visitor` INTEGER, PRIMARY KEY(`detailUrl`, `pairNumber`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `team_details` (`teamId` INTEGER NOT NULL, `category` TEXT, `captainName` TEXT, PRIMARY KEY(`teamId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `players` (`teamId` INTEGER NOT NULL, `name` TEXT NOT NULL, `isCaptain` INTEGER NOT NULL, `points` TEXT, `birthYear` TEXT, PRIMARY KEY(`teamId`, `name`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `jornadas` (`groupId` INTEGER NOT NULL, `jornada` INTEGER NOT NULL, PRIMARY KEY(`groupId`, `jornada`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cache_timestamps` (`cacheKey` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`cacheKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '8fa1147b885252b2ff743b6fbcba7084')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `league_groups`");
        db.execSQL("DROP TABLE IF EXISTS `standings`");
        db.execSQL("DROP TABLE IF EXISTS `match_results`");
        db.execSQL("DROP TABLE IF EXISTS `match_detail_pairs`");
        db.execSQL("DROP TABLE IF EXISTS `team_details`");
        db.execSQL("DROP TABLE IF EXISTS `players`");
        db.execSQL("DROP TABLE IF EXISTS `jornadas`");
        db.execSQL("DROP TABLE IF EXISTS `cache_timestamps`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsLeagueGroups = new HashMap<String, TableInfo.Column>(5);
        _columnsLeagueGroups.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLeagueGroups.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLeagueGroups.put("gender", new TableInfo.Column("gender", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLeagueGroups.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLeagueGroups.put("groupLetter", new TableInfo.Column("groupLetter", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLeagueGroups = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLeagueGroups = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLeagueGroups = new TableInfo("league_groups", _columnsLeagueGroups, _foreignKeysLeagueGroups, _indicesLeagueGroups);
        final TableInfo _existingLeagueGroups = TableInfo.read(db, "league_groups");
        if (!_infoLeagueGroups.equals(_existingLeagueGroups)) {
          return new RoomOpenHelper.ValidationResult(false, "league_groups(com.padelaragon.app.data.local.entity.LeagueGroupEntity).\n"
                  + " Expected:\n" + _infoLeagueGroups + "\n"
                  + " Found:\n" + _existingLeagueGroups);
        }
        final HashMap<String, TableInfo.Column> _columnsStandings = new HashMap<String, TableInfo.Column>(15);
        _columnsStandings.put("groupId", new TableInfo.Column("groupId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("position", new TableInfo.Column("position", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("teamName", new TableInfo.Column("teamName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("teamId", new TableInfo.Column("teamId", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("teamHref", new TableInfo.Column("teamHref", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("points", new TableInfo.Column("points", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("matchesPlayed", new TableInfo.Column("matchesPlayed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("encountersWon", new TableInfo.Column("encountersWon", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("encountersLost", new TableInfo.Column("encountersLost", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("matchesWon", new TableInfo.Column("matchesWon", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("matchesLost", new TableInfo.Column("matchesLost", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("setsWon", new TableInfo.Column("setsWon", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("setsLost", new TableInfo.Column("setsLost", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("gamesWon", new TableInfo.Column("gamesWon", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStandings.put("gamesLost", new TableInfo.Column("gamesLost", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStandings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStandings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStandings = new TableInfo("standings", _columnsStandings, _foreignKeysStandings, _indicesStandings);
        final TableInfo _existingStandings = TableInfo.read(db, "standings");
        if (!_infoStandings.equals(_existingStandings)) {
          return new RoomOpenHelper.ValidationResult(false, "standings(com.padelaragon.app.data.local.entity.StandingRowEntity).\n"
                  + " Expected:\n" + _infoStandings + "\n"
                  + " Found:\n" + _existingStandings);
        }
        final HashMap<String, TableInfo.Column> _columnsMatchResults = new HashMap<String, TableInfo.Column>(11);
        _columnsMatchResults.put("groupId", new TableInfo.Column("groupId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchResults.put("localTeam", new TableInfo.Column("localTeam", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchResults.put("localTeamId", new TableInfo.Column("localTeamId", "INTEGER", true, 3, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchResults.put("visitorTeam", new TableInfo.Column("visitorTeam", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchResults.put("visitorTeamId", new TableInfo.Column("visitorTeamId", "INTEGER", true, 4, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchResults.put("localScore", new TableInfo.Column("localScore", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchResults.put("visitorScore", new TableInfo.Column("visitorScore", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchResults.put("date", new TableInfo.Column("date", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchResults.put("venue", new TableInfo.Column("venue", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchResults.put("jornada", new TableInfo.Column("jornada", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchResults.put("detailUrl", new TableInfo.Column("detailUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMatchResults = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMatchResults = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMatchResults = new TableInfo("match_results", _columnsMatchResults, _foreignKeysMatchResults, _indicesMatchResults);
        final TableInfo _existingMatchResults = TableInfo.read(db, "match_results");
        if (!_infoMatchResults.equals(_existingMatchResults)) {
          return new RoomOpenHelper.ValidationResult(false, "match_results(com.padelaragon.app.data.local.entity.MatchResultEntity).\n"
                  + " Expected:\n" + _infoMatchResults + "\n"
                  + " Found:\n" + _existingMatchResults);
        }
        final HashMap<String, TableInfo.Column> _columnsMatchDetailPairs = new HashMap<String, TableInfo.Column>(12);
        _columnsMatchDetailPairs.put("detailUrl", new TableInfo.Column("detailUrl", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchDetailPairs.put("pairNumber", new TableInfo.Column("pairNumber", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchDetailPairs.put("localPlayer1", new TableInfo.Column("localPlayer1", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchDetailPairs.put("localPlayer2", new TableInfo.Column("localPlayer2", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchDetailPairs.put("visitorPlayer1", new TableInfo.Column("visitorPlayer1", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchDetailPairs.put("visitorPlayer2", new TableInfo.Column("visitorPlayer2", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchDetailPairs.put("set1Local", new TableInfo.Column("set1Local", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchDetailPairs.put("set1Visitor", new TableInfo.Column("set1Visitor", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchDetailPairs.put("set2Local", new TableInfo.Column("set2Local", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchDetailPairs.put("set2Visitor", new TableInfo.Column("set2Visitor", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchDetailPairs.put("set3Local", new TableInfo.Column("set3Local", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatchDetailPairs.put("set3Visitor", new TableInfo.Column("set3Visitor", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMatchDetailPairs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMatchDetailPairs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMatchDetailPairs = new TableInfo("match_detail_pairs", _columnsMatchDetailPairs, _foreignKeysMatchDetailPairs, _indicesMatchDetailPairs);
        final TableInfo _existingMatchDetailPairs = TableInfo.read(db, "match_detail_pairs");
        if (!_infoMatchDetailPairs.equals(_existingMatchDetailPairs)) {
          return new RoomOpenHelper.ValidationResult(false, "match_detail_pairs(com.padelaragon.app.data.local.entity.MatchDetailPairEntity).\n"
                  + " Expected:\n" + _infoMatchDetailPairs + "\n"
                  + " Found:\n" + _existingMatchDetailPairs);
        }
        final HashMap<String, TableInfo.Column> _columnsTeamDetails = new HashMap<String, TableInfo.Column>(3);
        _columnsTeamDetails.put("teamId", new TableInfo.Column("teamId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTeamDetails.put("category", new TableInfo.Column("category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTeamDetails.put("captainName", new TableInfo.Column("captainName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTeamDetails = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTeamDetails = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTeamDetails = new TableInfo("team_details", _columnsTeamDetails, _foreignKeysTeamDetails, _indicesTeamDetails);
        final TableInfo _existingTeamDetails = TableInfo.read(db, "team_details");
        if (!_infoTeamDetails.equals(_existingTeamDetails)) {
          return new RoomOpenHelper.ValidationResult(false, "team_details(com.padelaragon.app.data.local.entity.TeamDetailEntity).\n"
                  + " Expected:\n" + _infoTeamDetails + "\n"
                  + " Found:\n" + _existingTeamDetails);
        }
        final HashMap<String, TableInfo.Column> _columnsPlayers = new HashMap<String, TableInfo.Column>(5);
        _columnsPlayers.put("teamId", new TableInfo.Column("teamId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("name", new TableInfo.Column("name", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("isCaptain", new TableInfo.Column("isCaptain", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("points", new TableInfo.Column("points", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("birthYear", new TableInfo.Column("birthYear", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlayers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlayers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlayers = new TableInfo("players", _columnsPlayers, _foreignKeysPlayers, _indicesPlayers);
        final TableInfo _existingPlayers = TableInfo.read(db, "players");
        if (!_infoPlayers.equals(_existingPlayers)) {
          return new RoomOpenHelper.ValidationResult(false, "players(com.padelaragon.app.data.local.entity.PlayerEntity).\n"
                  + " Expected:\n" + _infoPlayers + "\n"
                  + " Found:\n" + _existingPlayers);
        }
        final HashMap<String, TableInfo.Column> _columnsJornadas = new HashMap<String, TableInfo.Column>(2);
        _columnsJornadas.put("groupId", new TableInfo.Column("groupId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJornadas.put("jornada", new TableInfo.Column("jornada", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysJornadas = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesJornadas = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoJornadas = new TableInfo("jornadas", _columnsJornadas, _foreignKeysJornadas, _indicesJornadas);
        final TableInfo _existingJornadas = TableInfo.read(db, "jornadas");
        if (!_infoJornadas.equals(_existingJornadas)) {
          return new RoomOpenHelper.ValidationResult(false, "jornadas(com.padelaragon.app.data.local.entity.JornadaEntity).\n"
                  + " Expected:\n" + _infoJornadas + "\n"
                  + " Found:\n" + _existingJornadas);
        }
        final HashMap<String, TableInfo.Column> _columnsCacheTimestamps = new HashMap<String, TableInfo.Column>(2);
        _columnsCacheTimestamps.put("cacheKey", new TableInfo.Column("cacheKey", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCacheTimestamps.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCacheTimestamps = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCacheTimestamps = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCacheTimestamps = new TableInfo("cache_timestamps", _columnsCacheTimestamps, _foreignKeysCacheTimestamps, _indicesCacheTimestamps);
        final TableInfo _existingCacheTimestamps = TableInfo.read(db, "cache_timestamps");
        if (!_infoCacheTimestamps.equals(_existingCacheTimestamps)) {
          return new RoomOpenHelper.ValidationResult(false, "cache_timestamps(com.padelaragon.app.data.local.entity.CacheTimestamp).\n"
                  + " Expected:\n" + _infoCacheTimestamps + "\n"
                  + " Found:\n" + _existingCacheTimestamps);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "8fa1147b885252b2ff743b6fbcba7084", "3eb5ce2e8f603510d0c7c07d59e96896");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "league_groups","standings","match_results","match_detail_pairs","team_details","players","jornadas","cache_timestamps");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `league_groups`");
      _db.execSQL("DELETE FROM `standings`");
      _db.execSQL("DELETE FROM `match_results`");
      _db.execSQL("DELETE FROM `match_detail_pairs`");
      _db.execSQL("DELETE FROM `team_details`");
      _db.execSQL("DELETE FROM `players`");
      _db.execSQL("DELETE FROM `jornadas`");
      _db.execSQL("DELETE FROM `cache_timestamps`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LeagueGroupDao.class, LeagueGroupDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(StandingRowDao.class, StandingRowDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MatchResultDao.class, MatchResultDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MatchDetailDao.class, MatchDetailDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TeamDetailDao.class, TeamDetailDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(JornadaDao.class, JornadaDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CacheTimestampDao.class, CacheTimestampDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LeagueGroupDao leagueGroupDao() {
    if (_leagueGroupDao != null) {
      return _leagueGroupDao;
    } else {
      synchronized(this) {
        if(_leagueGroupDao == null) {
          _leagueGroupDao = new LeagueGroupDao_Impl(this);
        }
        return _leagueGroupDao;
      }
    }
  }

  @Override
  public StandingRowDao standingRowDao() {
    if (_standingRowDao != null) {
      return _standingRowDao;
    } else {
      synchronized(this) {
        if(_standingRowDao == null) {
          _standingRowDao = new StandingRowDao_Impl(this);
        }
        return _standingRowDao;
      }
    }
  }

  @Override
  public MatchResultDao matchResultDao() {
    if (_matchResultDao != null) {
      return _matchResultDao;
    } else {
      synchronized(this) {
        if(_matchResultDao == null) {
          _matchResultDao = new MatchResultDao_Impl(this);
        }
        return _matchResultDao;
      }
    }
  }

  @Override
  public MatchDetailDao matchDetailDao() {
    if (_matchDetailDao != null) {
      return _matchDetailDao;
    } else {
      synchronized(this) {
        if(_matchDetailDao == null) {
          _matchDetailDao = new MatchDetailDao_Impl(this);
        }
        return _matchDetailDao;
      }
    }
  }

  @Override
  public TeamDetailDao teamDetailDao() {
    if (_teamDetailDao != null) {
      return _teamDetailDao;
    } else {
      synchronized(this) {
        if(_teamDetailDao == null) {
          _teamDetailDao = new TeamDetailDao_Impl(this);
        }
        return _teamDetailDao;
      }
    }
  }

  @Override
  public JornadaDao jornadaDao() {
    if (_jornadaDao != null) {
      return _jornadaDao;
    } else {
      synchronized(this) {
        if(_jornadaDao == null) {
          _jornadaDao = new JornadaDao_Impl(this);
        }
        return _jornadaDao;
      }
    }
  }

  @Override
  public CacheTimestampDao cacheTimestampDao() {
    if (_cacheTimestampDao != null) {
      return _cacheTimestampDao;
    } else {
      synchronized(this) {
        if(_cacheTimestampDao == null) {
          _cacheTimestampDao = new CacheTimestampDao_Impl(this);
        }
        return _cacheTimestampDao;
      }
    }
  }
}
