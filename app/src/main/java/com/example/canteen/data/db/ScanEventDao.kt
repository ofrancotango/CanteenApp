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

    @Query("SELECT COUNT(*) FROM scan_events WHERE result = 'BONUS' AND timestamp >= :startOfDay")
    suspend fun getBonusCountForToday(startOfDay: Long): Int

    // All events today (SUCCESS, BONUS, DENIED) — for the live Today's Users screen
    @Query("SELECT * FROM scan_events WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodayAllScans(startOfDay: Long): Flow<List<ScanEvent>>
}
