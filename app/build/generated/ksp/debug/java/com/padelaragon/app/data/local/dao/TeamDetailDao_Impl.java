package com.padelaragon.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.padelaragon.app.data.local.entity.PlayerEntity;
import com.padelaragon.app.data.local.entity.TeamDetailEntity;
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
public final class TeamDetailDao_Impl implements TeamDetailDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TeamDetailEntity> __insertionAdapterOfTeamDetailEntity;

  private final EntityInsertionAdapter<PlayerEntity> __insertionAdapterOfPlayerEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeletePlayersByTeamId;

  public TeamDetailDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTeamDetailEntity = new EntityInsertionAdapter<TeamDetailEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `team_details` (`teamId`,`category`,`captainName`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TeamDetailEntity entity) {
        statement.bindLong(1, entity.getTeamId());
        if (entity.getCategory() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getCategory());
        }
        if (entity.getCaptainName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getCaptainName());
        }
      }
    };
    this.__insertionAdapterOfPlayerEntity = new EntityInsertionAdapter<PlayerEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `players` (`teamId`,`name`,`isCaptain`,`points`,`birthYear`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlayerEntity entity) {
        statement.bindLong(1, entity.getTeamId());
        statement.bindString(2, entity.getName());
        final int _tmp = entity.isCaptain() ? 1 : 0;
        statement.bindLong(3, _tmp);
        if (entity.getPoints() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getPoints());
        }
        if (entity.getBirthYear() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getBirthYear());
        }
      }
    };
    this.__preparedStmtOfDeletePlayersByTeamId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM players WHERE teamId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertTeamDetail(final TeamDetailEntity detail,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTeamDetailEntity.insert(detail);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertPlayers(final List<PlayerEntity> players,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlayerEntity.insert(players);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTeamWithPlayers(final TeamDetailEntity detail,
      final List<PlayerEntity> players, final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> TeamDetailDao.DefaultImpls.insertTeamWithPlayers(TeamDetailDao_Impl.this, detail, players, __cont), $completion);
  }

  @Override
  public Object deletePlayersByTeamId(final int teamId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePlayersByTeamId.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, teamId);
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
          __preparedStmtOfDeletePlayersByTeamId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByTeamId(final int teamId,
      final Continuation<? super TeamDetailEntity> $completion) {
    final String _sql = "SELECT * FROM team_details WHERE teamId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, teamId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TeamDetailEntity>() {
      @Override
      @Nullable
      public TeamDetailEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTeamId = CursorUtil.getColumnIndexOrThrow(_cursor, "teamId");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfCaptainName = CursorUtil.getColumnIndexOrThrow(_cursor, "captainName");
          final TeamDetailEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpTeamId;
            _tmpTeamId = _cursor.getInt(_cursorIndexOfTeamId);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpCaptainName;
            if (_cursor.isNull(_cursorIndexOfCaptainName)) {
              _tmpCaptainName = null;
            } else {
              _tmpCaptainName = _cursor.getString(_cursorIndexOfCaptainName);
            }
            _result = new TeamDetailEntity(_tmpTeamId,_tmpCategory,_tmpCaptainName);
          } else {
            _result = null;
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
  public Object getPlayersByTeamId(final int teamId,
      final Continuation<? super List<PlayerEntity>> $completion) {
    final String _sql = "SELECT * FROM players WHERE teamId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, teamId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PlayerEntity>>() {
      @Override
      @NonNull
      public List<PlayerEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfTeamId = CursorUtil.getColumnIndexOrThrow(_cursor, "teamId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIsCaptain = CursorUtil.getColumnIndexOrThrow(_cursor, "isCaptain");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final int _cursorIndexOfBirthYear = CursorUtil.getColumnIndexOrThrow(_cursor, "birthYear");
          final List<PlayerEntity> _result = new ArrayList<PlayerEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlayerEntity _item;
            final int _tmpTeamId;
            _tmpTeamId = _cursor.getInt(_cursorIndexOfTeamId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpIsCaptain;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCaptain);
            _tmpIsCaptain = _tmp != 0;
            final String _tmpPoints;
            if (_cursor.isNull(_cursorIndexOfPoints)) {
              _tmpPoints = null;
            } else {
              _tmpPoints = _cursor.getString(_cursorIndexOfPoints);
            }
            final String _tmpBirthYear;
            if (_cursor.isNull(_cursorIndexOfBirthYear)) {
              _tmpBirthYear = null;
            } else {
              _tmpBirthYear = _cursor.getString(_cursorIndexOfBirthYear);
            }
            _item = new PlayerEntity(_tmpTeamId,_tmpName,_tmpIsCaptain,_tmpPoints,_tmpBirthYear);
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
