package com.example.canteen.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanEventDao {
    @Insert
    suspend fun insert(event: ScanEvent)

    @Query("SELECT * FROM scan_events ORDER BY timestamp DESC")
    suspend fun getAll(): List<ScanEvent>

    @Query("SELECT * FROM scan_events WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getEventsByDate(start: Long, end: Long): List<ScanEvent>

    @Query("SELECT * FROM scan_events WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getEventsByDateFlow(start: Long, end: Long): Flow<List<ScanEvent>>

    @Query("SELECT COUNT(*) FROM scan_events WHERE result = 'BONUS' AND timestamp >= :startOfDay")
    suspend fun getBonusCountForToday(startOfDay: Long): Int

    // All events today (SUCCESS, BONUS, DENIED) — for the live Today's Users screen
    // Dynamic start-of-day: SQLite calculates "today at midnight" in LOCAL TIME every time the query runs
    @Query("SELECT * FROM scan_events WHERE timestamp >= (strftime('%s', 'now', 'localtime', 'start of day') * 1000) ORDER BY timestamp DESC")
    fun getTodayAllScans(): Flow<List<ScanEvent>>

    @Query("SELECT * FROM scan_events WHERE area = :area AND timestamp >= (strftime('%s', 'now', 'localtime', 'start of day') * 1000) ORDER BY timestamp DESC")
    fun getTodayAllScansByArea(area: String): Flow<List<ScanEvent>>

    @Query("SELECT COUNT(*) FROM scan_events WHERE result IN ('SUCCESS', 'BONUS') AND timestamp >= (strftime('%s', 'now', 'localtime', 'start of day') * 1000)")
    fun getTodayAdmittedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_events WHERE area = :area AND result IN ('SUCCESS', 'BONUS') AND timestamp >= (strftime('%s', 'now', 'localtime', 'start of day') * 1000)")
    fun getTodayAdmittedCountByArea(area: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_events WHERE result = 'DENIED' AND timestamp >= (strftime('%s', 'now', 'localtime', 'start of day') * 1000)")
    fun getTodayDeniedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_events WHERE area = :area AND result = 'DENIED' AND timestamp >= (strftime('%s', 'now', 'localtime', 'start of day') * 1000)")
    fun getTodayDeniedCountByArea(area: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_events WHERE timestamp >= (strftime('%s', 'now', 'localtime', 'start of day') * 1000)")
    fun getTodayTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_events WHERE area = :area AND timestamp >= (strftime('%s', 'now', 'localtime', 'start of day') * 1000)")
    fun getTodayTotalCountByArea(area: String): Flow<Int>

    @Query("UPDATE scan_events SET note = :note WHERE timestamp = :timestamp AND (company = 'JimCatering' OR company = 'Jim Catering')")
    suspend fun updateNoteByTimestamp(note: String, timestamp: Long)
}
