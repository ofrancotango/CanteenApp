package com.example.canteen.work

import android.content.Context
import com.example.canteen.data.EmailConfig
import com.example.canteen.data.db.AppDatabase
import com.example.canteen.data.db.ScanEvent
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

object EmailSender {

    fun sendDailyReport(context: Context, events: List<ScanEvent>) {
        val dateStr = SimpleDateFormat("EEEE, MMMM dd yyyy", Locale.ENGLISH).format(Date())
        val csvDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        val admitted = events.count { it.result == "SUCCESS" }
        val bonus    = events.count { it.result == "BONUS" }
        val denied   = events.count { it.result == "DENIED" }

        // Shift breakdown by unique workers
        val dayEvents = events.filter { it.shift == "DAY" }
        val nightEvents = events.filter { it.shift == "NIGHT" }
        val dayAdmitted = dayEvents.count { it.result == "SUCCESS" }
        val dayBonus    = dayEvents.count { it.result == "BONUS" }
        val dayDenied   = dayEvents.count { it.result == "DENIED" }
        val nightAdmitted = nightEvents.count { it.result == "SUCCESS" }
        val nightBonus    = nightEvents.count { it.result == "BONUS" }
        val nightDenied   = nightEvents.count { it.result == "DENIED" }
        val shiftSummary = """
  <div style="padding:0 32px 24px;">
    <div style="background:#FAFAFA;border-radius:10px;padding:16px 20px;border:1px solid #F0F0F0;">
      <p style="margin:0 0 12px;font-size:12px;color:#888;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Entrate per turno</p>
      <div style="display:flex;gap:16px;">
        <div style="flex:1;background:#F0FDF4;border-radius:6px;padding:12px 14px;">
          <div style="font-size:11px;color:#888;margin-bottom:2px;">DAY (06-15)</div>
          <div style="font-size:18px;font-weight:700;color:#22C55E;">${dayAdmitted + dayBonus}</div>
          <div style="font-size:10px;color:#aaa;">${dayAdmitted} ammessi / ${dayBonus} bonus / ${dayDenied} negati</div>
        </div>
        <div style="flex:1;background:#EFF6FF;border-radius:6px;padding:12px 14px;">
          <div style="font-size:11px;color:#888;margin-bottom:2px;">NIGHT (15-21:30)</div>
          <div style="font-size:18px;font-weight:700;color:#2563EB;">${nightAdmitted + nightBonus}</div>
          <div style="font-size:10px;color:#aaa;">${nightAdmitted} ammessi / ${nightBonus} bonus / ${nightDenied} negati</div>
        </div>
      </div>
    </div>
  </div>"""

        val noteEvents = events.filter { !it.note.isNullOrBlank() }
        val notesSection = if (noteEvents.isNotEmpty()) {
            val noteRows = noteEvents.joinToString("") { e ->
                val name = e.matchedName ?: e.scannedCode
                val time = timeFmt.format(Date(e.timestamp))
                "<tr>" +
                "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;'>$name</td>" +
                "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;color:#888;'>${e.company ?: ""}</td>" +
                "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;color:#B45309;font-style:italic;'>${e.note}</td>" +
                "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;color:#888;'>$time</td>" +
                "</tr>"
            }
            """
  <div style="padding:0 32px 24px;">
    <div style="background:#FFFBEB;border-radius:10px;padding:16px 20px;border:1px solid #FEF3C7;">
      <p style="margin:0 0 12px;font-size:12px;color:#B45309;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Jim Catering Notes</p>
      <table style="width:100%;border-collapse:collapse;">
        <thead><tr style="background:#FEF3C7;">
          <th style="padding:8px 12px;text-align:left;font-size:11px;color:#B45309;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Name</th>
          <th style="padding:8px 12px;text-align:left;font-size:11px;color:#B45309;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Company</th>
          <th style="padding:8px 12px;text-align:left;font-size:11px;color:#B45309;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Note</th>
          <th style="padding:8px 12px;text-align:left;font-size:11px;color:#B45309;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Time</th>
        </tr></thead>
        <tbody>$noteRows</tbody>
      </table>
    </div>
  </div>"""
        } else ""

        val rows = events.joinToString("") { e ->
            val color = when (e.result) {
                "SUCCESS" -> "#22C55E"
                "BONUS"   -> "#F59E0B"
                else      -> "#EF4444"
            }
            val noteCell = if (e.note != null) "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;color:#666;font-size:11px;font-style:italic;'>${e.note}</td>" else "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;color:#aaa;font-size:11px;'>-</td>"
            "<tr>" +
            "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;'>${e.matchedName ?: e.scannedCode}</td>" +
            "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;color:#888;'>${e.company ?: ""}</td>" +
            "<td style='padding:8px 12px;border-bottom:1px solid #F0F0F0;font-weight:600;color:$color;'>${e.result}</td>" +
            noteCell +
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
$shiftSummary
$notesSection
  <div style="padding:0 32px 32px;">
    <table style="width:100%;border-collapse:collapse;">
      <thead><tr style="background:#F8F8F8;">
        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#888;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Name</th>
        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#888;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Company</th>
        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#888;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Result</th>
        <th style="padding:10px 12px;text-align:left;font-size:11px;color:#888;font-weight:600;text-transform:uppercase;letter-spacing:0.5px;">Note</th>
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

        val csvData = buildCsv(events)
        val csvFilename = "scan_logs_$csvDate.csv"
        sendEmail("Canteen Report – $dateStr", html, csvData, csvFilename)
    }

    fun buildCsv(events: List<ScanEvent>): String {
        val sb = StringBuilder()
        sb.append("ID;Time;Code;MatchedName;Company;Result;Reason;Shift;Note\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        fun csvField(value: String?): String {
            val raw = (value ?: "")
                .trim()
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .replace(Regex("\\s+"), " ")
            return if (raw.contains(";") || raw.contains("\"")) {
                "\"" + raw.replace("\"", "\"\"") + "\""
            } else raw
        }

        events.forEach { e ->
            val timeStr = sdf.format(Date(e.timestamp))
            sb.append("${e.id};$timeStr;${csvField(e.scannedCode)};${csvField(e.matchedName)};${csvField(e.company)};${csvField(e.result)};${csvField(e.reason)};${csvField(e.shift)};${csvField(e.note)}\n")
        }
        return sb.toString()
    }

    private fun sendEmail(subject: String, htmlBody: String, csvData: String? = null, csvFilename: String = "scan_logs.csv") {
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
                csvData?.let { data ->
                    addBodyPart(MimeBodyPart().apply {
                        dataHandler = javax.activation.DataHandler(
                            javax.mail.util.ByteArrayDataSource(data.toByteArray(), "text/csv")
                        )
                        fileName = csvFilename
                    })
                }
            }
            setContent(multipart)
        }.let { Transport.send(it) }
    }
}
