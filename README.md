# Bulk Messenger

A personal/business-use Android app for sending SMS to many people at once — either the same message to everyone, or a different message per person — with automated, throttled sending so you're not manually opening chat after chat.

## Core sending

- **Broadcast** (same message → many numbers): pick contacts or paste numbers, one message, one tap send.
- **Personalized Batch** (different message per number): a two-page flow —
  - a **list page** showing every recipient as a card (number + a one-line message preview), with add/edit/delete
  - a dedicated **add/edit page** (number field, "Pick from Contacts", message field) for each recipient
- **CSV import** for batches — paste rows directly or load a real `.csv` file from your phone. The import dialog shows a formatted example of the expected `number,message` layout.
- **Scheduled sending** — choose "Send now" or pick a future date/time via the native date/time pickers, for both Broadcast and Personalized Batch. Scheduled jobs show as *Scheduled* in Job History until they fire.
- **Automated sending**: a `WorkManager` worker (`SmsSendWorker`) sends items one-by-one with a delay between sends (default 3s) so it doesn't trip carrier spam filters — this replaces the manual "open thread → type → send → repeat" you were doing by hand.

## Organization

- **Drafts**: save/edit/delete reusable message templates.
- **Job History**: every send creates a `BulkJob`; expand it to see per-number status (Sent/Failed/Pending).
  - Total messages sent (all-time), plus a day-by-day breakdown
  - Message snippet on the job card for Broadcasts, per-recipient snippet for Personalized batches
  - Filter by **All / Broadcast / Personalized**
- **Notifications**: a notification fires when a job finishes, showing sent/failed counts; tapping it reopens the app.

## Polish

- **Light / Dark theme toggle** (System / Light / Dark) from the Home screen, remembered across launches.
- Distinct accent color per feature area instead of a single flat palette.
- Built for **personal/business sideload use** — not published to Play Store, so it doesn't need to register as the default SMS app.

## How to open and run

1. Open this folder (`BulkMessenger/`) in **Android Studio** (Koala/2024.1 or newer recommended).
2. Let Gradle sync — it pulls dependencies from Google's Maven and Maven Central automatically.
3. Connect your phone (USB debugging on) or use an emulator, hit Run — or from the command line: `gradlew installDebug` with a device connected.
4. On first launch, grant SMS + Contacts (+ Notifications on Android 13+) permission when prompted.
5. Since this isn't installed from the Play Store, Android may show a "not your default SMS app" warning the first time you send — that's expected and fine; `SmsManager` still works for apps holding `SEND_SMS` permission.

## Known limits to be aware of

- Carriers commonly cap outbound SMS per SIM per day (varies by carrier/region) — the app automates the clicking, not the carrier's own throttling.
- Dual-SIM number/SIM-slot selection isn't wired up yet (uses the default SIM).
- No delivery-report tracking yet (only send success/failure, not whether the recipient's phone confirmed receipt).
- Scheduled sends rely on `WorkManager`'s delayed enqueue, not exact alarms — on aggressive battery-management OEMs (Vivo/Oppo/Realme/Xiaomi), a scheduled send can fire a few minutes late unless the app is exempted from background restrictions.
- Schema changes to the local database currently use a destructive migration (no upgrade path preserves old job history) — fine for this MVP stage, but worth revisiting before this app is relied on long-term.

## Suggested next increments

1. Retry-failed-items button on the Job History screen.
2. Per-job configurable send delay + dual-SIM picker in Settings.
3. `{name}` placeholder substitution in drafts (auto-fill from contact name).
4. Delivery report tracking via `SmsManager` sent/delivered `PendingIntent`s.
