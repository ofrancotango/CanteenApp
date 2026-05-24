package com.example.canteen.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    suspend fun getAllStats(): List<DailyStats>

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    suspend fun getStatsForDate(date: String): DailyStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: DailyStats)
}
