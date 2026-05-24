package com.example.canteen.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
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
public final class DailyStatsDao_Impl implements DailyStatsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DailyStats> __insertionAdapterOfDailyStats;

  public DailyStatsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDailyStats = new EntityInsertionAdapter<DailyStats>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_stats` (`date`,`totalScans`,`uniqueUsers`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyStats entity) {
        statement.bindString(1, entity.getDate());
        statement.bindLong(2, entity.getTotalScans());
        statement.bindLong(3, entity.getUniqueUsers());
      }
    };
  }

  @Override
  public Object insertOrUpdate(final DailyStats stats,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailyStats.insert(stats);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllStats(final Continuation<? super List<DailyStats>> $completion) {
    final String _sql = "SELECT * FROM daily_stats ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailyStats>>() {
      @Override
      @NonNull
      public List<DailyStats> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalScans = CursorUtil.getColumnIndexOrThrow(_cursor, "totalScans");
          final int _cursorIndexOfUniqueUsers = CursorUtil.getColumnIndexOrThrow(_cursor, "uniqueUsers");
          final List<DailyStats> _result = new ArrayList<DailyStats>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyStats _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpTotalScans;
            _tmpTotalScans = _cursor.getInt(_cursorIndexOfTotalScans);
            final int _tmpUniqueUsers;
            _tmpUniqueUsers = _cursor.getInt(_cursorIndexOfUniqueUsers);
            _item = new DailyStats(_tmpDate,_tmpTotalScans,_tmpUniqueUsers);
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
  public Object getStatsForDate(final String date,
      final Continuation<? super DailyStats> $completion) {
    final String _sql = "SELECT * FROM daily_stats WHERE date = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DailyStats>() {
      @Override
      @Nullable
      public DailyStats call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalScans = CursorUtil.getColumnIndexOrThrow(_cursor, "totalScans");
          final int _cursorIndexOfUniqueUsers = CursorUtil.getColumnIndexOrThrow(_cursor, "uniqueUsers");
          final DailyStats _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final int _tmpTotalScans;
            _tmpTotalScans = _cursor.getInt(_cursorIndexOfTotalScans);
            final int _tmpUniqueUsers;
            _tmpUniqueUsers = _cursor.getInt(_cursorIndexOfUniqueUsers);
            _result = new DailyStats(_tmpDate,_tmpTotalScans,_tmpUniqueUsers);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
