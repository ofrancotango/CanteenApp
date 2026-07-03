package com.example.canteen.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_events")
data class ScanEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val scannedCode: String,
    val matchedName: String?,
    val company: String?,
    val result: String, // "SUCCESS", "DENIED", "BONUS"
    val reason: String? = null, // "LIMIT_REACHED", "UNKNOWN_USER", "BLACKLISTED", etc.
    val shift: String = "DAY", // "DAY" (06:00-22:00) or "NIGHT" (22:00-06:00)
    val note: String? = null // Optional note (e.g. "Jim Catering passing: Mario Rossi")
)
