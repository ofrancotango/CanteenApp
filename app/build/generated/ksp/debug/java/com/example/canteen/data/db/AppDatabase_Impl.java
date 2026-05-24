package com.example.canteen.data.db;

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
  private volatile DailyStatsDao _dailyStatsDao;

  private volatile ScanEventDao _scanEventDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_stats` (`date` TEXT NOT NULL, `totalScans` INTEGER NOT NULL, `uniqueUsers` INTEGER NOT NULL, PRIMARY KEY(`date`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `scan_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `scannedCode` TEXT NOT NULL, `matchedName` TEXT, `company` TEXT, `result` TEXT NOT NULL, `reason` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd7ef5e74d4bdbb000f1479953b89aeac')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `daily_stats`");
        db.execSQL("DROP TABLE IF EXISTS `scan_events`");
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
        final HashMap<String, TableInfo.Column> _columnsDailyStats = new HashMap<String, TableInfo.Column>(3);
        _columnsDailyStats.put("date", new TableInfo.Column("date", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("totalScans", new TableInfo.Column("totalScans", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyStats.put("uniqueUsers", new TableInfo.Column("uniqueUsers", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDailyStats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDailyStats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDailyStats = new TableInfo("daily_stats", _columnsDailyStats, _foreignKeysDailyStats, _indicesDailyStats);
        final TableInfo _existingDailyStats = TableInfo.read(db, "daily_stats");
        if (!_infoDailyStats.equals(_existingDailyStats)) {
          return new RoomOpenHelper.ValidationResult(false, "daily_stats(com.example.canteen.data.db.DailyStats).\n"
                  + " Expected:\n" + _infoDailyStats + "\n"
                  + " Found:\n" + _existingDailyStats);
        }
        final HashMap<String, TableInfo.Column> _columnsScanEvents = new HashMap<String, TableInfo.Column>(7);
        _columnsScanEvents.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanEvents.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanEvents.put("scannedCode", new TableInfo.Column("scannedCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanEvents.put("matchedName", new TableInfo.Column("matchedName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanEvents.put("company", new TableInfo.Column("company", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanEvents.put("result", new TableInfo.Column("result", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScanEvents.put("reason", new TableInfo.Column("reason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysScanEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesScanEvents = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoScanEvents = new TableInfo("scan_events", _columnsScanEvents, _foreignKeysScanEvents, _indicesScanEvents);
        final TableInfo _existingScanEvents = TableInfo.read(db, "scan_events");
        if (!_infoScanEvents.equals(_existingScanEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "scan_events(com.example.canteen.data.db.ScanEvent).\n"
                  + " Expected:\n" + _infoScanEvents + "\n"
                  + " Found:\n" + _existingScanEvents);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "d7ef5e74d4bdbb000f1479953b89aeac", "649fe9adc2ac15ce694adf2409e06390");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "daily_stats","scan_events");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `daily_stats`");
      _db.execSQL("DELETE FROM `scan_events`");
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
    _typeConvertersMap.put(DailyStatsDao.class, DailyStatsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ScanEventDao.class, ScanEventDao_Impl.getRequiredConverters());
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
  public DailyStatsDao dailyStatsDao() {
    if (_dailyStatsDao != null) {
      return _dailyStatsDao;
    } else {
      synchronized(this) {
        if(_dailyStatsDao == null) {
          _dailyStatsDao = new DailyStatsDao_Impl(this);
        }
        return _dailyStatsDao;
      }
    }
  }

  @Override
  public ScanEventDao scanEventDao() {
    if (_scanEventDao != null) {
      return _scanEventDao;
    } else {
      synchronized(this) {
        if(_scanEventDao == null) {
          _scanEventDao = new ScanEventDao_Impl(this);
        }
        return _scanEventDao;
      }
    }
  }
}
