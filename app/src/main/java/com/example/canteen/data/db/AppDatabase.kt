package com.example.canteen.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DailyStats::class, ScanEvent::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun scanEventDao(): ScanEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "canteen_database"
                )
                .fallbackToDestructiveMigration() // Allow destructive migration for dev
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
