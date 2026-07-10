package com.example.canteen.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import com.example.canteen.data.EmailConfig
import com.example.canteen.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (EmailConfig.SENDER_EMAIL == "your-gmail@gmail.com") {
                    pending.finish()
                    return@launch
                }

                if (!isAlreadySentToday(context)) {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.scanEventDao()

                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val start = calendar.timeInMillis
                    val end = start + 24 * 60 * 60 * 1000L
                    val events = dao.getEventsByDate(start, end)

                    if (events.isNotEmpty()) {
                        EmailSender.sendDailyReport(context, events)
                        markSent(context)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Mail inviata con successo!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                // Always reschedule for tomorrow after check/send
                EmailAlarmScheduler.schedule(context)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore invio mail: ${e.message}", Toast.LENGTH_LONG).show()
                }
                // Reschedule anyway so we don't miss tomorrow
                EmailAlarmScheduler.schedule(context)
            } finally {
                pending.finish()
            }
        }
    }

    private fun isAlreadySentToday(context: Context): Boolean {
        val prefs = getPrefs(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return prefs.getString("last_sent", "") == today
    }

    private fun markSent(context: Context) {
        val prefs = getPrefs(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit().putString("last_sent", today).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences("canteen_email", Context.MODE_PRIVATE)
}
