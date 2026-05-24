package com.example.canteen.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
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
public final class ScanEventDao_Impl implements ScanEventDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ScanEvent> __insertionAdapterOfScanEvent;

  public ScanEventDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfScanEvent = new EntityInsertionAdapter<ScanEvent>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `scan_events` (`id`,`timestamp`,`scannedCode`,`matchedName`,`company`,`result`,`reason`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ScanEvent entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, entity.getScannedCode());
        if (entity.getMatchedName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMatchedName());
        }
        if (entity.getCompany() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getCompany());
        }
        statement.bindString(6, entity.getResult());
        if (entity.getReason() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getReason());
        }
      }
    };
  }

  @Override
  public Object insert(final ScanEvent event, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfScanEvent.insert(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAll(final Continuation<? super List<ScanEvent>> $completion) {
    final String _sql = "SELECT * FROM scan_events ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ScanEvent>>() {
      @Override
      @NonNull
      public List<ScanEvent> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfScannedCode = CursorUtil.getColumnIndexOrThrow(_cursor, "scannedCode");
          final int _cursorIndexOfMatchedName = CursorUtil.getColumnIndexOrThrow(_cursor, "matchedName");
          final int _cursorIndexOfCompany = CursorUtil.getColumnIndexOrThrow(_cursor, "company");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final List<ScanEvent> _result = new ArrayList<ScanEvent>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScanEvent _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpScannedCode;
            _tmpScannedCode = _cursor.getString(_cursorIndexOfScannedCode);
            final String _tmpMatchedName;
            if (_cursor.isNull(_cursorIndexOfMatchedName)) {
              _tmpMatchedName = null;
            } else {
              _tmpMatchedName = _cursor.getString(_cursorIndexOfMatchedName);
            }
            final String _tmpCompany;
            if (_cursor.isNull(_cursorIndexOfCompany)) {
              _tmpCompany = null;
            } else {
              _tmpCompany = _cursor.getString(_cursorIndexOfCompany);
            }
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            _item = new ScanEvent(_tmpId,_tmpTimestamp,_tmpScannedCode,_tmpMatchedName,_tmpCompany,_tmpResult,_tmpReason);
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
  public Object getEventsByDate(final long start, final long end,
      final Continuation<? super List<ScanEvent>> $completion) {
    final String _sql = "SELECT * FROM scan_events WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, start);
    _argIndex = 2;
    _statement.bindLong(_argIndex, end);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ScanEvent>>() {
      @Override
      @NonNull
      public List<ScanEvent> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfScannedCode = CursorUtil.getColumnIndexOrThrow(_cursor, "scannedCode");
          final int _cursorIndexOfMatchedName = CursorUtil.getColumnIndexOrThrow(_cursor, "matchedName");
          final int _cursorIndexOfCompany = CursorUtil.getColumnIndexOrThrow(_cursor, "company");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfReason = CursorUtil.getColumnIndexOrThrow(_cursor, "reason");
          final List<ScanEvent> _result = new ArrayList<ScanEvent>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScanEvent _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpScannedCode;
            _tmpScannedCode = _cursor.getString(_cursorIndexOfScannedCode);
            final String _tmpMatchedName;
            if (_cursor.isNull(_cursorIndexOfMatchedName)) {
              _tmpMatchedName = null;
            } else {
              _tmpMatchedName = _cursor.getString(_cursorIndexOfMatchedName);
            }
            final String _tmpCompany;
            if (_cursor.isNull(_cursorIndexOfCompany)) {
              _tmpCompany = null;
            } else {
              _tmpCompany = _cursor.getString(_cursorIndexOfCompany);
            }
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpReason;
            if (_cursor.isNull(_cursorIndexOfReason)) {
              _tmpReason = null;
            } else {
              _tmpReason = _cursor.getString(_cursorIndexOfReason);
            }
            _item = new ScanEvent(_tmpId,_tmpTimestamp,_tmpScannedCode,_tmpMatchedName,_tmpCompany,_tmpResult,_tmpReason);
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
  public Object getBonusCountForToday(final long startOfDay,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM scan_events WHERE result = 'BONUS' AND timestamp >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startOfDay);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
