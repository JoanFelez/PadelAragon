package com.padelaragon.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.padelaragon.app.data.local.entity.StandingRowEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class StandingRowDao_Impl implements StandingRowDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<StandingRowEntity> __insertionAdapterOfStandingRowEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByGroupId;

  public StandingRowDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStandingRowEntity = new EntityInsertionAdapter<StandingRowEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `standings` (`groupId`,`position`,`teamName`,`teamId`,`teamHref`,`points`,`matchesPlayed`,`encountersWon`,`encountersLost`,`matchesWon`,`matchesLost`,`setsWon`,`setsLost`,`gamesWon`,`gamesLost`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StandingRowEntity entity) {
        statement.bindLong(1, entity.getGroupId());
        statement.bindLong(2, entity.getPosition());
        statement.bindString(3, entity.getTeamName());
        statement.bindLong(4, entity.getTeamId());
        statement.bindString(5, entity.getTeamHref());
        statement.bindLong(6, entity.getPoints());
        statement.bindLong(7, entity.getMatchesPlayed());
        statement.bindLong(8, entity.getEncountersWon());
        statement.bindLong(9, entity.getEncountersLost());
        statement.bindLong(10, entity.getMatchesWon());
        statement.bindLong(11, entity.getMatchesLost());
        statement.bindLong(12, entity.getSetsWon());
        statement.bindLong(13, entity.getSetsLost());
        statement.bindLong(14, entity.getGamesWon());
        statement.bindLong(15, entity.getGamesLost());
      }
    };
    this.__preparedStmtOfDeleteByGroupId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM standings WHERE groupId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<StandingRowEntity> standings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfStandingRowEntity.insert(standings);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByGroupId(final int groupId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByGroupId.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, groupId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByGroupId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByGroupId(final int groupId,
      final Continuation<? super List<StandingRowEntity>> $completion) {
    final String _sql = "SELECT * FROM standings WHERE groupId = ? ORDER BY position ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, groupId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<StandingRowEntity>>() {
      @Override
      @NonNull
      public List<StandingRowEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "position");
          final int _cursorIndexOfTeamName = CursorUtil.getColumnIndexOrThrow(_cursor, "teamName");
          final int _cursorIndexOfTeamId = CursorUtil.getColumnIndexOrThrow(_cursor, "teamId");
          final int _cursorIndexOfTeamHref = CursorUtil.getColumnIndexOrThrow(_cursor, "teamHref");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final int _cursorIndexOfMatchesPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "matchesPlayed");
          final int _cursorIndexOfEncountersWon = CursorUtil.getColumnIndexOrThrow(_cursor, "encountersWon");
          final int _cursorIndexOfEncountersLost = CursorUtil.getColumnIndexOrThrow(_cursor, "encountersLost");
          final int _cursorIndexOfMatchesWon = CursorUtil.getColumnIndexOrThrow(_cursor, "matchesWon");
          final int _cursorIndexOfMatchesLost = CursorUtil.getColumnIndexOrThrow(_cursor, "matchesLost");
          final int _cursorIndexOfSetsWon = CursorUtil.getColumnIndexOrThrow(_cursor, "setsWon");
          final int _cursorIndexOfSetsLost = CursorUtil.getColumnIndexOrThrow(_cursor, "setsLost");
          final int _cursorIndexOfGamesWon = CursorUtil.getColumnIndexOrThrow(_cursor, "gamesWon");
          final int _cursorIndexOfGamesLost = CursorUtil.getColumnIndexOrThrow(_cursor, "gamesLost");
          final List<StandingRowEntity> _result = new ArrayList<StandingRowEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StandingRowEntity _item;
            final int _tmpGroupId;
            _tmpGroupId = _cursor.getInt(_cursorIndexOfGroupId);
            final int _tmpPosition;
            _tmpPosition = _cursor.getInt(_cursorIndexOfPosition);
            final String _tmpTeamName;
            _tmpTeamName = _cursor.getString(_cursorIndexOfTeamName);
            final int _tmpTeamId;
            _tmpTeamId = _cursor.getInt(_cursorIndexOfTeamId);
            final String _tmpTeamHref;
            _tmpTeamHref = _cursor.getString(_cursorIndexOfTeamHref);
            final int _tmpPoints;
            _tmpPoints = _cursor.getInt(_cursorIndexOfPoints);
            final int _tmpMatchesPlayed;
            _tmpMatchesPlayed = _cursor.getInt(_cursorIndexOfMatchesPlayed);
            final int _tmpEncountersWon;
            _tmpEncountersWon = _cursor.getInt(_cursorIndexOfEncountersWon);
            final int _tmpEncountersLost;
            _tmpEncountersLost = _cursor.getInt(_cursorIndexOfEncountersLost);
            final int _tmpMatchesWon;
            _tmpMatchesWon = _cursor.getInt(_cursorIndexOfMatchesWon);
            final int _tmpMatchesLost;
            _tmpMatchesLost = _cursor.getInt(_cursorIndexOfMatchesLost);
            final int _tmpSetsWon;
            _tmpSetsWon = _cursor.getInt(_cursorIndexOfSetsWon);
            final int _tmpSetsLost;
            _tmpSetsLost = _cursor.getInt(_cursorIndexOfSetsLost);
            final int _tmpGamesWon;
            _tmpGamesWon = _cursor.getInt(_cursorIndexOfGamesWon);
            final int _tmpGamesLost;
            _tmpGamesLost = _cursor.getInt(_cursorIndexOfGamesLost);
            _item = new StandingRowEntity(_tmpGroupId,_tmpPosition,_tmpTeamName,_tmpTeamId,_tmpTeamHref,_tmpPoints,_tmpMatchesPlayed,_tmpEncountersWon,_tmpEncountersLost,_tmpMatchesWon,_tmpMatchesLost,_tmpSetsWon,_tmpSetsLost,_tmpGamesWon,_tmpGamesLost);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
