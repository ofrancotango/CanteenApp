---
name: Daily report data source
description: Where the daily email report must read its events from, so any device can send the complete summary.
---

The daily report email must be built from **Firebase (`scans/<today>`)**, not from the local Room database.

**Why:** Each device only writes its own scans to the local DB. A phone sending the report would otherwise include only the scans it had physically performed, leaving out scans from other devices. The user reported receiving a report with just the test scans from a single phone instead of 300+ scans from all devices.

**How to apply:**
- `AlarmReceiver`, `DailyReportWorker`, and the manual test-email action in `MainActivity` should all fetch the day's events from Firebase before building/sending the report.
- Keep the local DB for live UI, offline resilience, and logging, but treat Firebase as the source of truth for the aggregated daily report.
- Add a network constraint to the `DailyReportWorker` so it only runs when the device can reach Firebase.
- If fetching from Firebase fails, do not claim the distributed send lock (`config/lastReportSentDate`), so another device can try later.
