package com.example.canteen.work

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.canteen.data.EmailConfig
import com.example.canteen.data.db.AppDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.concurrent.TimeUnit
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class DailyReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private fun getPrefs(): SharedPreferences = applicationContext.getSharedPreferences("canteen_email", Context.MODE_PRIVATE)

    private fun isAlreadySentToday(): Boolean {
        val prefs = getPrefs()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return prefs.getString("last_sent", "") == today
    }

    private fun markSent() {
        val prefs = getPrefs()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit().putString("last_sent", today).apply()
    }

    private fun isAfterSendTime(): Boolean {
        val now = Calendar.getInstance()
        return now.get(Calendar.HOUR_OF_DAY) >= EmailConfig.SEND_HOUR
    }

    override suspend fun doWork(): Result {
        if (EmailConfig.SENDER_EMAIL == "your-gmail@gmail.com") return Result.success()
        val forceSend = inputData.getBoolean("force_send", false)
        if (!forceSend && !isAfterSendTime()) return Result.success()
        if (!forceSend && isAlreadySentToday()) return Result.success()

        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.scanEventDao()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L

            val events = dao.getEventsByDate(startOfDay, endOfDay)
            if (events.isEmpty()) return Result.success()

            val dateStr = SimpleDateFormat("EEEE, MMMM dd yyyy", Locale.ENGLISH).format(Date(startOfDay))
            val csvDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startOfDay))

            val html = EmailSender.buildHtmlReport(events, dateStr)
            val csvData = EmailSender.buildCsv(events)
            val csvFilename = "scan_logs_$csvDate.csv"
            sendEmail("Canteen Report – $dateStr", html, csvData, csvFilename)
            markSent()
            scheduleNextDay()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(applicationContext, "Email sent successfully!", android.widget.Toast.LENGTH_LONG).show()
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(applicationContext, "Email error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
            Result.retry()
        }
    }

    private fun scheduleNextDay() {
        val workManager = WorkManager.getInstance(applicationContext)
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, EmailConfig.SEND_HOUR)
            set(Calendar.MINUTE, EmailConfig.SEND_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delayMs = target.timeInMillis - now.timeInMillis

        val request = OneTimeWorkRequestBuilder<DailyReportWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "daily_canteen_report",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun sendEmail(subject: String, htmlBody: String, csvData: String, csvFilename: String) {
        val props = Properties().apply {
            put("mail.smtp.host", EmailConfig.SMTP_HOST)
            put("mail.smtp.port", EmailConfig.SMTP_PORT.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.ssl.protocols", "TLSv1.2")
        }
        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(EmailConfig.SENDER_EMAIL, EmailConfig.SENDER_PASSWORD)
        })
        MimeMessage(session).apply {
            setFrom(InternetAddress(EmailConfig.SENDER_EMAIL, "Canteen Access"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(EmailConfig.RECIPIENT_EMAIL))
            this.subject = subject
            val multipart = MimeMultipart().apply {
                addBodyPart(MimeBodyPart().apply { setContent(htmlBody, "text/html; charset=utf-8") })
                addBodyPart(MimeBodyPart().apply {
                    dataHandler = javax.activation.DataHandler(
                        javax.mail.util.ByteArrayDataSource(csvData.toByteArray(), "text/csv")
                    )
                    fileName = csvFilename
                })
            }
            setContent(multipart)
        }.let { Transport.send(it) }
    }
}
