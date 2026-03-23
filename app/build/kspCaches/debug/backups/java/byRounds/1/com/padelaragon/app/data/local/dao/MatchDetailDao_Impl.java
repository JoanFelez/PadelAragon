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
import com.padelaragon.app.data.local.entity.MatchDetailPairEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class MatchDetailDao_Impl implements MatchDetailDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MatchDetailPairEntity> __insertionAdapterOfMatchDetailPairEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByDetailUrl;

  public MatchDetailDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMatchDetailPairEntity = new EntityInsertionAdapter<MatchDetailPairEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `match_detail_pairs` (`detailUrl`,`pairNumber`,`localPlayer1`,`localPlayer2`,`visitorPlayer1`,`visitorPlayer2`,`set1Local`,`set1Visitor`,`set2Local`,`set2Visitor`,`set3Local`,`set3Visitor`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MatchDetailPairEntity entity) {
        statement.bindString(1, entity.getDetailUrl());
        statement.bindLong(2, entity.getPairNumber());
        statement.bindString(3, entity.getLocalPlayer1());
        statement.bindString(4, entity.getLocalPlayer2());
        statement.bindString(5, entity.getVisitorPlayer1());
        statement.bindString(6, entity.getVisitorPlayer2());
        if (entity.getSet1Local() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getSet1Local());
        }
        if (entity.getSet1Visitor() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getSet1Visitor());
        }
        if (entity.getSet2Local() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getSet2Local());
        }
        if (entity.getSet2Visitor() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getSet2Visitor());
        }
        if (entity.getSet3Local() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getSet3Local());
        }
        if (entity.getSet3Visitor() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getSet3Visitor());
        }
      }
    };
    this.__preparedStmtOfDeleteByDetailUrl = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM match_detail_pairs WHERE detailUrl = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<MatchDetailPairEntity> pairs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMatchDetailPairEntity.insert(pairs);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByDetailUrl(final String detailUrl,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByDetailUrl.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, detailUrl);
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
          __preparedStmtOfDeleteByDetailUrl.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByDetailUrl(final String detailUrl,
      final Continuation<? super List<MatchDetailPairEntity>> $completion) {
    final String _sql = "SELECT * FROM match_detail_pairs WHERE detailUrl = ? ORDER BY pairNumber";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, detailUrl);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MatchDetailPairEntity>>() {
      @Override
      @NonNull
      public List<MatchDetailPairEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDetailUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "detailUrl");
          final int _cursorIndexOfPairNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "pairNumber");
          final int _cursorIndexOfLocalPlayer1 = CursorUtil.getColumnIndexOrThrow(_cursor, "localPlayer1");
          final int _cursorIndexOfLocalPlayer2 = CursorUtil.getColumnIndexOrThrow(_cursor, "localPlayer2");
          final int _cursorIndexOfVisitorPlayer1 = CursorUtil.getColumnIndexOrThrow(_cursor, "visitorPlayer1");
          final int _cursorIndexOfVisitorPlayer2 = CursorUtil.getColumnIndexOrThrow(_cursor, "visitorPlayer2");
          final int _cursorIndexOfSet1Local = CursorUtil.getColumnIndexOrThrow(_cursor, "set1Local");
          final int _cursorIndexOfSet1Visitor = CursorUtil.getColumnIndexOrThrow(_cursor, "set1Visitor");
          final int _cursorIndexOfSet2Local = CursorUtil.getColumnIndexOrThrow(_cursor, "set2Local");
          final int _cursorIndexOfSet2Visitor = CursorUtil.getColumnIndexOrThrow(_cursor, "set2Visitor");
          final int _cursorIndexOfSet3Local = CursorUtil.getColumnIndexOrThrow(_cursor, "set3Local");
          final int _cursorIndexOfSet3Visitor = CursorUtil.getColumnIndexOrThrow(_cursor, "set3Visitor");
          final List<MatchDetailPairEntity> _result = new ArrayList<MatchDetailPairEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MatchDetailPairEntity _item;
            final String _tmpDetailUrl;
            _tmpDetailUrl = _cursor.getString(_cursorIndexOfDetailUrl);
            final int _tmpPairNumber;
            _tmpPairNumber = _cursor.getInt(_cursorIndexOfPairNumber);
            final String _tmpLocalPlayer1;
            _tmpLocalPlayer1 = _cursor.getString(_cursorIndexOfLocalPlayer1);
            final String _tmpLocalPlayer2;
            _tmpLocalPlayer2 = _cursor.getString(_cursorIndexOfLocalPlayer2);
            final String _tmpVisitorPlayer1;
            _tmpVisitorPlayer1 = _cursor.getString(_cursorIndexOfVisitorPlayer1);
            final String _tmpVisitorPlayer2;
            _tmpVisitorPlayer2 = _cursor.getString(_cursorIndexOfVisitorPlayer2);
            final Integer _tmpSet1Local;
            if (_cursor.isNull(_cursorIndexOfSet1Local)) {
              _tmpSet1Local = null;
            } else {
              _tmpSet1Local = _cursor.getInt(_cursorIndexOfSet1Local);
            }
            final Integer _tmpSet1Visitor;
            if (_cursor.isNull(_cursorIndexOfSet1Visitor)) {
              _tmpSet1Visitor = null;
            } else {
              _tmpSet1Visitor = _cursor.getInt(_cursorIndexOfSet1Visitor);
            }
            final Integer _tmpSet2Local;
            if (_cursor.isNull(_cursorIndexOfSet2Local)) {
              _tmpSet2Local = null;
            } else {
              _tmpSet2Local = _cursor.getInt(_cursorIndexOfSet2Local);
            }
            final Integer _tmpSet2Visitor;
            if (_cursor.isNull(_cursorIndexOfSet2Visitor)) {
              _tmpSet2Visitor = null;
            } else {
              _tmpSet2Visitor = _cursor.getInt(_cursorIndexOfSet2Visitor);
            }
            final Integer _tmpSet3Local;
            if (_cursor.isNull(_cursorIndexOfSet3Local)) {
              _tmpSet3Local = null;
            } else {
              _tmpSet3Local = _cursor.getInt(_cursorIndexOfSet3Local);
            }
            final Integer _tmpSet3Visitor;
            if (_cursor.isNull(_cursorIndexOfSet3Visitor)) {
              _tmpSet3Visitor = null;
            } else {
              _tmpSet3Visitor = _cursor.getInt(_cursorIndexOfSet3Visitor);
            }
            _item = new MatchDetailPairEntity(_tmpDetailUrl,_tmpPairNumber,_tmpLocalPlayer1,_tmpLocalPlayer2,_tmpVisitorPlayer1,_tmpVisitorPlayer2,_tmpSet1Local,_tmpSet1Visitor,_tmpSet2Local,_tmpSet2Visitor,_tmpSet3Local,_tmpSet3Visitor);
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
