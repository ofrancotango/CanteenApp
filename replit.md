# CanteenApp

A native Android app for canteen access management using QR code scanning. Controls employee entry based on shift schedules, company rules, and a blacklist — backed by Firebase Realtime Database and a local Room database.

## Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Database:** Room (local) + Firebase Realtime Database (cloud sync)
- **Background jobs:** WorkManager + AlarmManager (daily email reports)
- **Build:** Gradle 8.6, compileSdk 34, minSdk 24

## Key features

- QR code scanning with shift-based access control (Day: 06:00–15:00, Night: 15:00–21:30)
- Real-time Firebase sync for allowed/forbidden companies and blacklisted employees
- Admin panel for managing rules, manual whitelist entries, and app kill-switch
- Daily stats and scan history screens
- Automatic daily email report via background worker

## Project structure

```
app/src/main/java/com/example/canteen/
  data/             # Business logic, repositories, Firebase sync, Employee model
  data/db/          # Room database, DAOs, ScanEvent and DailyStats entities
  ui/               # Compose screens (Home, QRScanner, Result, Admin, Stats)
  utils/            # String normalization/matching helpers
  work/             # WorkManager tasks for email scheduling
```

## Running / building

This is a native Android app — it cannot run in a browser preview. To build an APK:

```bash
./gradlew assembleDebug
```

The APK will be output to `app/build/outputs/apk/debug/app-debug.apk`.

## User preferences

- Keep existing project structure and stack — do not restructure or migrate.
