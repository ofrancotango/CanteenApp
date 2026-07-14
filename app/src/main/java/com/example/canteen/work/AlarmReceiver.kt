package com.example.canteen.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import com.example.canteen.data.EmailConfig
import com.example.canteen.data.db.AppDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fired by AlarmManager at the configured send time.
 *
 * Single-sender guarantee (multi-device):
 *  1. Fast local check — if this device's SharedPreferences already records today's date,
 *     skip without touching Firebase (covers the normal "already ran" case).
 *  2. Firebase transaction on config/lastReportSentDate — atomically sets the date to today
 *     only if it is not already today. Only the device that *commits* the transaction proceeds
 *     to send the email; all others abort silently.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()

        if (EmailConfig.SENDER_EMAIL == "your-gmail@gmail.com") {
            pending.finish()
            return
        }

        // Fast path: this device already sent today
        if (isAlreadySentToday(context)) {
            EmailAlarmScheduler.schedule(context)
            pending.finish()
            return
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val sentDateRef = FirebaseDatabase
            .getInstance("https://app-cant-default-rtdb.firebaseio.com/")
            .getReference("config")
            .child("lastReportSentDate")

        sentDateRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val stored = currentData.getValue(String::class.java)
                if (stored == today) return Transaction.abort()   // another device already won
                currentData.value = today
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (!committed) {
                    // Another device already claimed today's send — just reschedule
                    EmailAlarmScheduler.schedule(context)
                    pending.finish()
                    return
                }

                // This device won the distributed lock — send the email
                CoroutineScope(Dispatchers.IO).launch {
                    try {
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
                            markSent(context) // record locally so the fast-path works next time
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Mail inviata con successo!", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Errore invio mail: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        EmailAlarmScheduler.schedule(context)
                        pending.finish()
                    }
                }
            }
        })
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
