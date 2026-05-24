package com.example.canteen.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey
    val date: String, // Format: yyyy-MM-dd
    val totalScans: Int,
    val uniqueUsers: Int
)
