package com.example.canteen.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.canteen.data.EmailConfig
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val request = PeriodicWorkRequestBuilder<DailyReportWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_canteen_report",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
