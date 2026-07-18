# Bulk Messenger — Test Cases

Manual QA test-case matrix covering every feature area. "Status" reflects what was actually
executed live against a real device during this test pass (Vivo V2031, Android 13, single SIM)
versus what's documented for manual execution later (primarily anything needing a second
physical SIM, which this test device doesn't have).

Legend: ✅ Executed & passed · 📝 Documented, not executed this pass (environment limitation)

---

## 1. Permissions & App Launch

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| PERM-01 | Fresh install, no permissions granted | Launch app | System prompts for SEND_SMS, READ_CONTACTS, READ_PHONE_STATE, POST_NOTIFICATIONS (API 33+) in one batch | High | ✅ |
| PERM-02 | SEND_SMS already granted from a prior run | Launch app | App goes straight to Welcome → Home, no "Grant Permission" screen flash | High | ✅ |
| PERM-03 | App updated with a newly added permission (e.g. READ_PHONE_STATE) not yet granted, but SEND_SMS already granted | Launch app | App still opens to Home immediately, but the OS permission dialog for the new permission appears shortly after in the background | High | ✅ |
| PERM-04 | SEND_SMS permission denied | Launch app | "This app needs SMS permission..." screen shown with a "Grant Permission" retry button; app is unusable until granted | High | 📝 |

## 2. Splash & Onboarding

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| ONB-01 | Fresh install / cleared app data, zero profiles | Launch app | OS splash (icon only) → custom Welcome screen with logo scale/fade-in and "Welcome" text → routes to onboarding | High | ✅ |
| ONB-02 | At least one profile exists | Launch app | Welcome screen shows "Hello, {name}" instead of "Welcome" | High | ✅ |
| ONB-03 | On Onboarding screen | Enter a name, pick a color, tap "Get Started" | New profile created, becomes active, app routes straight to Home | High | ✅ (via earlier session) |
| ONB-04 | On Onboarding screen | Leave name blank | "Get Started" button stays disabled | Medium | 📝 |
| ONB-05 | On Onboarding screen, have a backup file available | Tap "Restoring on a new device? Restore from backup" → "Restore from file" → pick backup JSON → confirm | Confirmation dialog appears, then all profiles/drafts/jobs from the backup are restored and app routes to Home | High | ✅ |

## 3. Multi-user Profiles

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| USR-01 | Logged in as Profile A with data | Tap profile avatar (top bar) | Bottom sheet lists all profiles with avatar + name, checkmark on active one, "Add profile" at bottom | High | ✅ |
| USR-02 | Profile switcher open | Tap "Add profile" | Inline name + color form appears within the sheet | High | ✅ |
| USR-03 | Two profiles exist | Tap the non-active profile in the switcher | App switches active profile; Home reflects new avatar; Drafts/Job History for the new profile only | High | 📝 |
| USR-04 | Profile A has drafts/jobs, Profile B is new | Switch to Profile B | Profile B's Drafts and Job History are empty (no data leakage between profiles) | High | 📝 |
| USR-05 | Profile A theme = Dark | Switch to Profile B (theme = System) | Theme changes to match Profile B's stored preference, independent of Profile A | High | 📝 |

## 4. Broadcast (same message → many numbers)

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| BC-01 | On Broadcast screen | Type a phone number, tap "Add" | Number appears in Recipients list with a "#" avatar | High | ✅ |
| BC-02 | On Broadcast screen | Tap "Pick from Contacts", choose a contact | Contact added to Recipients with name + number shown | High | 📝 (contact picker not exercised this pass) |
| BC-03 | Recipients list has entries | Tap ✕ on a recipient | Recipient removed from the list, count updates | Medium | ✅ (implicit via UI) |
| BC-04 | No recipients or empty message | — | "Send" button stays disabled | High | ✅ |
| BC-05 | Recipients + message filled | Tap "Send to N recipient(s)" | Job created, appears in Job History as SCHEDULED→SENDING→COMPLETED; "Sending messages" foreground notification shows during send | High | ✅ (earlier session — real SMS sent) |
| BC-06 | Recipients + message filled | Tap "Schedule for later", pick a future date/time, then Send | Button label changes to "Schedule for N recipient(s)"; job shows as SCHEDULED with the chosen time until it fires | High | 📝 |
| BC-07 | Device has 2+ active SIMs | — | SIM picker chips appear above the schedule selector | Medium | 📝 (device is single-SIM) |
| BC-08 | Device has 1 active SIM | — | SIM picker is completely hidden | Medium | ✅ |

## 5. Personalized Batch (different message per number)

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| PB-01 | On Personalized list (empty) | Tap "Add Recipient" | Opens dedicated Add Recipient page (number, Pick from Contacts, message) | High | ✅ |
| PB-02 | On Add Recipient page | Fill number + message, tap "Add to Batch" | Returns to list; new row shows number + one-line message snippet | High | ✅ |
| PB-03 | Existing row in list | Tap the row | Opens the same page pre-filled for editing, with a delete icon in the top bar | High | 📝 |
| PB-04 | Editing an existing row | Tap the delete icon | Row removed, returns to list | Medium | 📝 |
| PB-05 | On Personalized list | Tap "Paste CSV" | Dialog opens with format explanation + example block + paste field | High | ✅ (dialog content verified) |
| PB-06 | Paste CSV dialog open | Paste `number,message` lines, tap Import | Rows appended to the list | High | 📝 |
| PB-07 | On Personalized list | Tap "Import .csv", pick a real `.csv` file | File content loads into the paste dialog automatically | Medium | 📝 |
| PB-08 | 2+ rows added | Tap "Send All (N)" | Job created with per-row messages; Job History shows per-item message snippets when expanded | High | ✅ (earlier session — real SMS sent) |

## 6. Drafts

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| DR-01 | On Drafts (empty) | Tap "+" FAB | New Draft dialog opens (Title, Message fields) | High | ✅ |
| DR-02 | New Draft dialog open | Fill title + message, tap Save | Draft appears in list showing title + message preview | High | ✅ |
| DR-03 | Draft exists | Tap the draft card | Edit dialog opens pre-filled | Medium | 📝 |
| DR-04 | Draft exists | Tap the trash icon | Draft deleted from the list | Medium | 📝 |

## 7. Job History

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| JH-01 | No jobs sent yet for active profile | Open Job History | "No jobs yet" empty state shown | Medium | ✅ |
| JH-02 | Jobs exist | Open Job History | "Total messages sent" summary card + list of jobs, most recent first | High | ✅ |
| JH-03 | Summary card visible | Tap "Show by day" | Expands a per-date breakdown (e.g. "18 Jul 2026 — 6 sent") | Medium | ✅ |
| JH-04 | Multiple job types exist | Tap "Broadcast" / "Personalized" filter chip | List filters to only that mode; "All" shows everything | High | ✅ |
| JH-05 | Job card visible | Tap the status chip | Expands to show sent/failed/total counts and per-recipient status list | High | ✅ |
| JH-06 | Broadcast job, mode = SAME_MESSAGE | Expand the job | Message snippet shown under the date on the collapsed card | Medium | ✅ |
| JH-07 | Personalized job expanded | — | Each recipient row shows its own message, ellipsized to fit | Medium | ✅ (verified via code path; no failed personalized job available to re-check live) |
| JH-08 | Job has 0 failed items | Expand job | No "Retry Failed" button shown | High | ✅ |
| JH-09 | Job has 1+ failed items | Expand job | "Retry Failed (N)" button appears below the item list | High | 📝 (see instrumented test `RetryFailedItemsInstrumentedTest` for automated coverage) |
| JH-10 | Failed items exist, single SIM | Tap "Retry Failed" | Failed items reset to PENDING and are immediately re-sent; button disappears once all succeed | High | 📝 |
| JH-11 | Failed items exist, 2+ SIMs | Tap "Retry Failed" | Dialog asks which SIM to retry from before resending | Medium | 📝 (device is single-SIM) |

## 8. Scheduling

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| SCH-01 | On Broadcast or Personalized screen | Tap "Schedule for later" | Native date picker opens, then native time picker | High | 📝 |
| SCH-02 | Date/time picked | — | Chip shows "Scheduled for {date}, {time}" with a clear (✕) option | High | 📝 |
| SCH-03 | Scheduled job created | Wait until scheduled time | `SmsSendWorker` fires, job transitions SCHEDULED → SENDING → COMPLETED/FAILED | High | 📝 (long-running; not practical to wait out live in this pass) |

## 9. Settings

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| SET-01 | App not battery-optimization-exempt | Open Settings | "Disable battery optimization" button shown with explanatory text | High | ✅ |
| SET-02 | Tap "Disable battery optimization" | — | System dialog opens requesting the exemption | High | 📝 |
| SET-03 | Single SIM device | Open Settings | "Only one active SIM detected... nothing to choose from." message shown, no chips | Medium | ✅ |
| SET-04 | Dual SIM device | Open Settings | FilterChip per SIM shown; selecting one persists as the profile's default | Medium | 📝 (device is single-SIM) |
| SET-05 | On Settings | Tap "Backup to file" | System file picker opens with a pre-filled filename; after saving, "Backup saved." confirmation text appears | High | ✅ |
| SET-06 | On Settings, backup file exists | Tap "Restore from file", pick the file | Confirmation dialog → "Replace data" → all local data replaced with backup contents | High | ✅ |

## 10. Notifications

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| NOTIF-01 | Job actively sending | — | Ongoing low-priority "Sending messages — X of Y sent" notification with progress bar, promotes the worker to a foreground service | High | ✅ |
| NOTIF-02 | Job completes, 0 failures | — | Notification updates to "{Mode} sent" / "All N sent successfully" | High | ✅ |
| NOTIF-03 | Job completes, 1+ failures | — | Notification shows "{Mode} finished with errors" / "X sent, Y failed" | Medium | 📝 |
| NOTIF-04 | Tap a completion notification | — | Opens the app | Medium | 📝 |

## 11. Theme

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| TH-01 | Home screen | Tap the theme icon in the top bar | Cycles System → Light → Dark → System; icon updates each tap | High | ✅ |
| TH-02 | Theme set to Dark | Restart the app | Dark theme persists across restarts (Room-backed, per-profile) | High | ✅ (implicitly verified via restore test) |

---

## Reliability regression (crash fix)

| ID | Preconditions | Steps | Expected Result | Priority | Status |
|----|--------------|-------|------------------|----------|--------|
| REL-01 | Send a broadcast to 3+ recipients on an aggressive-battery-management OEM device (e.g. Vivo) | Trigger a send, then background the app | Foreground service notification keeps the process alive; job completes without being killed mid-send | High | 📝 (requires extended real-world observation; foreground-service fix confirmed present in code + notification visually confirmed firing) |
