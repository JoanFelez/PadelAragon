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
import com.padelaragon.app.data.local.entity.MatchResultEntity;
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
public final class MatchResultDao_Impl implements MatchResultDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MatchResultEntity> __insertionAdapterOfMatchResultEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByGroupAndJornada;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByGroupId;

  public MatchResultDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMatchResultEntity = new EntityInsertionAdapter<MatchResultEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `match_results` (`groupId`,`localTeam`,`localTeamId`,`visitorTeam`,`visitorTeamId`,`localScore`,`visitorScore`,`date`,`venue`,`jornada`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MatchResultEntity entity) {
        statement.bindLong(1, entity.getGroupId());
        statement.bindString(2, entity.getLocalTeam());
        statement.bindLong(3, entity.getLocalTeamId());
        statement.bindString(4, entity.getVisitorTeam());
        statement.bindLong(5, entity.getVisitorTeamId());
        statement.bindString(6, entity.getLocalScore());
        statement.bindString(7, entity.getVisitorScore());
        if (entity.getDate() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getDate());
        }
        if (entity.getVenue() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getVenue());
        }
        statement.bindLong(10, entity.getJornada());
      }
    };
    this.__preparedStmtOfDeleteByGroupAndJornada = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM match_results WHERE groupId = ? AND jornada = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByGroupId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM match_results WHERE groupId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<MatchResultEntity> results,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMatchResultEntity.insert(results);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByGroupAndJornada(final int groupId, final int jornada,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByGroupAndJornada.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, groupId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, jornada);
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
          __preparedStmtOfDeleteByGroupAndJornada.release(_stmt);
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
  public Object getByGroupAndJornada(final int groupId, final int jornada,
      final Continuation<? super List<MatchResultEntity>> $completion) {
    final String _sql = "SELECT * FROM match_results WHERE groupId = ? AND jornada = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, groupId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, jornada);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MatchResultEntity>>() {
      @Override
      @NonNull
      public List<MatchResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfLocalTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "localTeam");
          final int _cursorIndexOfLocalTeamId = CursorUtil.getColumnIndexOrThrow(_cursor, "localTeamId");
          final int _cursorIndexOfVisitorTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "visitorTeam");
          final int _cursorIndexOfVisitorTeamId = CursorUtil.getColumnIndexOrThrow(_cursor, "visitorTeamId");
          final int _cursorIndexOfLocalScore = CursorUtil.getColumnIndexOrThrow(_cursor, "localScore");
          final int _cursorIndexOfVisitorScore = CursorUtil.getColumnIndexOrThrow(_cursor, "visitorScore");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfVenue = CursorUtil.getColumnIndexOrThrow(_cursor, "venue");
          final int _cursorIndexOfJornada = CursorUtil.getColumnIndexOrThrow(_cursor, "jornada");
          final List<MatchResultEntity> _result = new ArrayList<MatchResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MatchResultEntity _item;
            final int _tmpGroupId;
            _tmpGroupId = _cursor.getInt(_cursorIndexOfGroupId);
            final String _tmpLocalTeam;
            _tmpLocalTeam = _cursor.getString(_cursorIndexOfLocalTeam);
            final int _tmpLocalTeamId;
            _tmpLocalTeamId = _cursor.getInt(_cursorIndexOfLocalTeamId);
            final String _tmpVisitorTeam;
            _tmpVisitorTeam = _cursor.getString(_cursorIndexOfVisitorTeam);
            final int _tmpVisitorTeamId;
            _tmpVisitorTeamId = _cursor.getInt(_cursorIndexOfVisitorTeamId);
            final String _tmpLocalScore;
            _tmpLocalScore = _cursor.getString(_cursorIndexOfLocalScore);
            final String _tmpVisitorScore;
            _tmpVisitorScore = _cursor.getString(_cursorIndexOfVisitorScore);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final String _tmpVenue;
            if (_cursor.isNull(_cursorIndexOfVenue)) {
              _tmpVenue = null;
            } else {
              _tmpVenue = _cursor.getString(_cursorIndexOfVenue);
            }
            final int _tmpJornada;
            _tmpJornada = _cursor.getInt(_cursorIndexOfJornada);
            _item = new MatchResultEntity(_tmpGroupId,_tmpLocalTeam,_tmpLocalTeamId,_tmpVisitorTeam,_tmpVisitorTeamId,_tmpLocalScore,_tmpVisitorScore,_tmpDate,_tmpVenue,_tmpJornada);
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

  @Override
  public Object getByGroupId(final int groupId,
      final Continuation<? super List<MatchResultEntity>> $completion) {
    final String _sql = "SELECT * FROM match_results WHERE groupId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, groupId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MatchResultEntity>>() {
      @Override
      @NonNull
      public List<MatchResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfLocalTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "localTeam");
          final int _cursorIndexOfLocalTeamId = CursorUtil.getColumnIndexOrThrow(_cursor, "localTeamId");
          final int _cursorIndexOfVisitorTeam = CursorUtil.getColumnIndexOrThrow(_cursor, "visitorTeam");
          final int _cursorIndexOfVisitorTeamId = CursorUtil.getColumnIndexOrThrow(_cursor, "visitorTeamId");
          final int _cursorIndexOfLocalScore = CursorUtil.getColumnIndexOrThrow(_cursor, "localScore");
          final int _cursorIndexOfVisitorScore = CursorUtil.getColumnIndexOrThrow(_cursor, "visitorScore");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfVenue = CursorUtil.getColumnIndexOrThrow(_cursor, "venue");
          final int _cursorIndexOfJornada = CursorUtil.getColumnIndexOrThrow(_cursor, "jornada");
          final List<MatchResultEntity> _result = new ArrayList<MatchResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MatchResultEntity _item;
            final int _tmpGroupId;
            _tmpGroupId = _cursor.getInt(_cursorIndexOfGroupId);
            final String _tmpLocalTeam;
            _tmpLocalTeam = _cursor.getString(_cursorIndexOfLocalTeam);
            final int _tmpLocalTeamId;
            _tmpLocalTeamId = _cursor.getInt(_cursorIndexOfLocalTeamId);
            final String _tmpVisitorTeam;
            _tmpVisitorTeam = _cursor.getString(_cursorIndexOfVisitorTeam);
            final int _tmpVisitorTeamId;
            _tmpVisitorTeamId = _cursor.getInt(_cursorIndexOfVisitorTeamId);
            final String _tmpLocalScore;
            _tmpLocalScore = _cursor.getString(_cursorIndexOfLocalScore);
            final String _tmpVisitorScore;
            _tmpVisitorScore = _cursor.getString(_cursorIndexOfVisitorScore);
            final String _tmpDate;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmpDate = null;
            } else {
              _tmpDate = _cursor.getString(_cursorIndexOfDate);
            }
            final String _tmpVenue;
            if (_cursor.isNull(_cursorIndexOfVenue)) {
              _tmpVenue = null;
            } else {
              _tmpVenue = _cursor.getString(_cursorIndexOfVenue);
            }
            final int _tmpJornada;
            _tmpJornada = _cursor.getInt(_cursorIndexOfJornada);
            _item = new MatchResultEntity(_tmpGroupId,_tmpLocalTeam,_tmpLocalTeamId,_tmpVisitorTeam,_tmpVisitorTeamId,_tmpLocalScore,_tmpVisitorScore,_tmpDate,_tmpVenue,_tmpJornada);
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
