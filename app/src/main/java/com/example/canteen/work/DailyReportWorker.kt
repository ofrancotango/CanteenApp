package com.example.canteen.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.canteen.data.EmailConfig
import com.example.canteen.data.db.AppDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Properties
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

    override suspend fun doWork(): Result {
        if (EmailConfig.SENDER_EMAIL == "your-gmail@gmail.com") return Result.success()

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
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

            val admitted = events.count { it.result == "SUCCESS" }
            val bonus    = events.count { it.result == "BONUS" }
            val denied   = events.count { it.result == "DENIED" }

            val rows = events.joinToString("") { e ->
                val color = when (e.result) {
                    "SUCCESS" -> "#22C55E"
                    "BONUS"   -> "#F59E0B"
                    else      -> "#EF4444"
                }
                "<tr>" +
                "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;'>${e.matchedName ?: e.scannedCode}</td>" +
                "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;color:#888;'>${e.company ?: ""}</td>" +
                "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;font-weight:600;color:$color;'>${e.result}</td>" +
                "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;color:#888;'>${timeFmt.format(Date(e.timestamp))}</td>" +
                "</tr>"
            }

            val html = """
<!DOCTYPE html><html><body style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#F8F8F8;margin:0;padding:24px;">
<div style="max-width:640px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 1px 4px rgba(0,0,0,0.08);">
  <div style="background:#111;padding:28px 32px;">
    <h1 style="color:#fff;margin:0;font-size:22px;font-weight:700;">Canteen Daily Report</h1>
    <p style="color:#aaa;margin:4px 0 0;font-size:14px;">$dateStr</p>
  </div>
  <div style="padding:24px 32px;display:flex;gap:16px;">
    <div style="flex:1;background:#F0FDF4;border-radius:8px;padding:16px 20px;">
      <div style="font-size:32px;font-weight:700;color:#22C55E;">${admitted + bonus}</div>
      <div style="font-size:11px;color:#888;margin-top:2px;letter-spacing:0.5px;">ADMITTED</div>
    </div>
    <div style="flex:1;background:#FFFBEB;border-radius:8px;padding:16px 20px;">
      <div style="font-size:32px;font-weight:700;color:#F59E0B;">$bonus</div>
      <div style="font-size:11px;color:#888;margin-top:2px;letter-spacing:0.5px;">BONUS</div>
    </div>
    <div style="flex:1;background:#FFF1F2;border-radius:8px;padding:16px 20px;">
      <div style="font-size:32px;font-weight:700;color:#EF4444;">$denied</div>
      <div style="font-size:11px;color:#888;margin-top:2px;letter-spacing:0.5px;">DENIED</div>
    </div>
  </div>
  <div style="padding:0 32px 32px;">
    <table style="width:100%;border-collapse:collapse;">
      <thead><tr style="background:#F8F8F8;">
        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#888;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Name</th>
        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#888;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Company</th>
        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#888;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Result</th>
        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#888;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Time</th>
      </tr></thead>
      <tbody>$rows</tbody>
    </table>
  </div>
  <div style="padding:16px 32px;background:#F8F8F8;border-top:1px solid #F0F0F0;">
    <p style="margin:0;font-size:12px;color:#aaa;">Sent automatically by Canteen Access System</p>
  </div>
</div>
</body></html>""".trimIndent()

            sendEmail("Canteen Report – $dateStr", html)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendEmail(subject: String, htmlBody: String) {
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
            val part = MimeBodyPart().apply { setContent(htmlBody, "text/html; charset=utf-8") }
            setContent(MimeMultipart().apply { addBodyPart(part) })
        }.let { Transport.send(it) }
    }
}
