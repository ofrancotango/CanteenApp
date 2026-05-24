package com.example.canteen.data

object EmailConfig {
    const val SMTP_HOST = "smtp.gmail.com"
    const val SMTP_PORT = 587

    // ─────────────────────────────────────────────────────────────────
    // SETUP:
    // 1. Use any Gmail account you own as the sender.
    // 2. In that Google account → Security → Enable 2-Step Verification.
    // 3. Go to myaccount.google.com/apppasswords
    // 4. Create an App Password for "Mail" on "Android device".
    // 5. Paste the 16-character password (with spaces) into SENDER_PASSWORD.
    // ─────────────────────────────────────────────────────────────────
    const val SENDER_EMAIL    = "your-gmail@gmail.com"   // ← change this
    const val SENDER_PASSWORD = "xxxx xxxx xxxx xxxx"    // ← change this (App Password)

    const val RECIPIENT_EMAIL = "f.tonelli@streicher.it"
    const val SEND_HOUR   = 22   // 22:00 every evening
    const val SEND_MINUTE = 0
}
