# Picklelog — Comprehensive End-to-End Test Plan

| | |
|---|---|
| **Application** | Picklelog — local-only pickleball match journal for Android (`com.maxeydev.picklelog`) |
| **Version under test** | `versionName 0.1.0`, `versionCode 1` (from `gradle/libs.versions.toml`) |
| **Source inspected** | `main` at commit `00ec962` ("wrap the animated ducks in composables on a shared clock", 2026-10-10 10:10 PHT) **plus ~60 uncommitted working-tree changes**. The repo was being committed to while this plan was written, so re-check §13 against the build you actually test. |
| **Written** | 2026-10-10 |
| **Supersedes** | `documentation/TEST-CHECKLIST.md` (2026-09-29, stops at RFC-013 and predates onboarding, export/import, erase, reminders, support page and the mascot work). Keep it for history; run this one. |
| **Source of truth** | The code. Where a doc (RFC, FEATURES, CHANGE-REQUEST-001) disagrees with the code, the code's behaviour is written as "Expected" and the disagreement is listed in §13. |

### How to read this document

- Every test has a unique ID (`LOG-07`, `PAY-12`…). IDs never get reused. If a test is retired, strike it through and leave the ID.
- **Pri** uses the risk scale in §4.2 (P0 – P3). **Type** codes: `FUN` functional · `UI` · `NEG` negative · `BVA` boundary value · `EDGE` edge case · `INT` integration (Android system / Google Play) · `E2E` · `SEC` security/privacy · `A11Y` · `PERF` · `COMP` compatibility · `DATA` data integrity · `REG` regression.
- Quoted text is the literal string from `ui/src/main/res/values/strings*.xml`. If the app shows different wording, either the test or the string is wrong: decide which, then fix one of them.
- **Status** values: `Not Tested` · `Pass` · `Fail` · `Blocked` · `N/A`. "Mostly right" is `Fail`. Write what you actually saw in **Actual**.
- 🔴 highlights the most critical tests. **Every P0 test is a release blocker whether or not it carries the mark**; the P1s that block release are listed in §14.
- All shell commands are **Windows PowerShell** run from `C:\emgiStuff\codingProjects\picklelog`.

### What was actually executed while writing this plan

Only the following were run. **No device, emulator, UI or Play test was executed.** Every manual test below is `Not Tested`.

| Run | Where | Result |
|---|---|---|
| `:domain` unit tests (29 classes) — compiled with the repo's own Kotlin 2.3.21 compiler jar and run with JUnit 4.13.2 against the working tree as of 2026-10-10 10:05 PHT | Cloud sandbox (no Gradle; Maven was not reachable) | **309 run, 309 passed.** 2 tests (`EntitlementInvariantTest` source scans) first failed only because the sandbox's working directory was wrong; re-run with the correct directory: pass. The cross-module part of that scan was repeated against `app`/`data`/`ui` sources on your machine: only `EntitlementRules.kt` and `UserProfileSerializer.kt` write `isPro = false`, both allow-listed. |
| Previously recorded JUnit XML in `*/build/test-results` (from your own Gradle runs, **not** re-run by me) | Your machine | `:domain:test` 309/309 pass (2026-10-09 21:38 PHT) · `:data:testDebugUnitTest` 77/77 pass (2026-10-09 23:03 PHT) · `:ui:testDebugUnitTest` 257/257 pass (2026-10-09 23:04 PHT). These predate the newest commits and working-tree edits, so treat them as stale. |
| Instrumented suites (`connectedDebugAndroidTest` in `:app`, `:data`, `:ui`) | — | **No recorded result found.** Unverified. |

---

## 1. Application Overview and Scope

### 1.1 What the app is

A single-user, **offline, account-less** Android app for logging pickleball matches. All data lives on the device (Room database `picklelog.db`, three DataStore files, a `photos/` folder). The only network traffic is Google Play Billing for a one-time **Pro** unlock. There is no backend, no API, no login.

**Architecture (4 Gradle modules + 1 benchmark):**

| Module | Contents | Test tooling already present |
|---|---|---|
| `:domain` | Pure Kotlin rules: streaks, insurance, stats, entitlement, free-tier caps, filters, search, suggestions, import cap, export-prompt policy, reminder schedule | JUnit 4 + coroutines-test (29 test classes) |
| `:data` | Room DB, DataStore stores, photo compression/storage, JSON export/import, Play Billing wrapper, WorkManager workers, erase | JUnit (9 classes) + instrumented (24 classes) |
| `:ui` | Jetpack Compose screens, ViewModels, navigation, WebView share-card renderer, sound effects, mascot | JUnit VM tests (21 classes) + Compose UI tests (20 classes) incl. card goldens |
| `:app` | `MainActivity`, `PicklelogApplication`, DI container, manifest, backup rules, **debug-only `DebugToolsProvider`** | Instrumented (5 classes) |
| `:benchmark` | Macrobenchmark `MatchListScrollBenchmark` | — |

Platform: `minSdk 26` (Android 8.0) · `targetSdk 37` · `compileSdk 37` · Kotlin 2.3.21 · AGP 9.4.0 · Compose BOM 2026.09.00 · Play Billing 9.1.0 · product id `picklelog_pro`.

### 1.2 Screens and routes (from `ui/navigation/Routes.kt` + `PicklelogNavHost.kt`)

| Route | Screen | Entered from | Notes |
|---|---|---|---|
| `OnboardingRoute` | 4-page intro (Logging → Stats → Share → Privacy) with a **required** name field on the last page | Start destination when onboarding is required; after **Erase all data** | Skip jumps to the last page; Back goes to the previous page |
| `HomeRoute` | Dashboard header + match list + search/filter/sort + notices + FAB | Start destination otherwise | Hosts the **Settings drawer** (not a route), first-match sheet, streak-milestone sheet |
| `MatchEditRoute(matchId?, logAnotherFrom?)` | Bottom-sheet-style editor: new / edit / "log another" | FAB, empty-state button, Detail → Edit, "Log another", streak notification tap | |
| `MatchDetailRoute(matchId)` | Match detail, photo viewer, delete | Tap a list row | |
| `ShareRoute(matchId)` | Share-card preview + variant picker | Detail → Share | WebView-rendered 1080×1920 / 1080×1080 JPEG |
| `StatsRoute` | "Your stats" (free + Pro sections) | Tap the dashboard header | Shares the Home dashboard ViewModel (same filters) |
| `PaywallRoute` | "Picklelog Pro" | Settings Pro card, cap block on save, upgrade prompts, missed-skip notice, locked stats, Support page | |
| `BackupRoute` | "Export & backup" | Settings, export-prompt banner, erase dialog "Export my matches first" | |
| `PrivacyRoute` · `AboutRoute` · `SupportRoute` | Static info screens | Settings | Support has Pro / Rate / Share / Donate rows |

There are **no deep links** declared in the manifest. The only external entry points are the launcher icon and the streak-reminder notification (`EXTRA_OPEN_LOGGING`).

### 1.3 User roles

There is one user per install. Behaviour differs by **entitlement state**, which is the only "role":

| State | How to reach it for testing |
|---|---|
| **Free** (default) | Fresh install |
| **Free at/near cap** | `Start-PickleScenario cap-40` / `cap-48` / `cap-50` |
| **Pro** (verified Play purchase) | Real purchase as a license tester, or `Set-PicklePro -State on` (debug build only) |
| **Pro pending** | Play "slow test card, declines/approves after a few minutes" |
| **Onboarding required** | Fresh install, or after Erase all data |

### 1.4 In scope

Every screen and flow above; local persistence; photos and camera; share-card rendering and Android share sheet; Play Billing purchase/restore; JSON export/import; Android Auto Backup rules; streak engine and insurance; WorkManager-driven streak reminders and export prompts; notifications; erase-all; settings; sound effects; dark theme; accessibility; compatibility API 26 → 37; performance at 1,000 matches; security/privacy of a local-only app.

### 1.5 Out of scope / not built (do not count as failures)

| Not present in code | Evidence |
|---|---|
| Accounts, login, registration, password reset, sessions, roles, server API, email | No network layer besides Play Billing; no auth code. Section 4C of the brief is **N/A** except the "onboarding gate", entitlement and local-data isolation checks in §8. |
| Home-screen widget (F44), custom app icons, CSV export (RFC-020) | No `glance`/`AppWidget` code |
| Donations | "Donate!" row exists on the Support page **but has no click action** (see §13 R-05) |
| "Hide my name on share cards" (S-1) | Not adopted per CHANGE-REQUEST-001 §7 |
| Following the system theme after the Dark-mode switch has been flipped once | Owner decision (two-way switch); see §13 R-07 |
| Rename / merge / delete a person | No UI for it |

---

## 2. Environment and Testing Prerequisites

### 2.1 Devices and builds

| ID | Environment | Use it for |
|---|---|---|
| **ENV-A** | Emulator **API 26**, `default` image (no Play Store), Pixel 6 profile | minSdk behaviour, photo-picker backport, notification permission *not* required, **billing unavailable** path |
| **ENV-B** | Emulator **API 37**, `google_apis` image | Current platform; same as CI matrix; `POST_NOTIFICATIONS` runtime prompt |
| **ENV-C** | Physical phone with Google Play, signed in as a **license tester**, app installed from the **internal testing** track | Real billing, real share targets, real camera, performance, Auto Backup |
| **ENV-D** | Second physical phone, same Google account as ENV-C | Restore-on-install / device transfer (BAK-29, BAK-30, E2E-06) |
| **ENV-E** | Small-screen emulator (e.g. 4.7" 720×1280, 320 dpi) and a tablet/foldable emulator (e.g. Pixel Tablet, Pixel Fold) | Responsive layout (§8.3) |

Builds:

| Build | Command | Notes |
|---|---|---|
| Debug | `.\gradlew.bat :app:assembleDebug` then `adb install -r app\build\outputs\apk\debug\app-debug.apk` | Includes `DebugToolsProvider` (test hooks below). **Never** ship. |
| Release | `.\gradlew.bat :app:bundleRelease` (needs `keystore\keystore.properties`) | Minified + shrunk. Without the keystore it is debug-signed and not uploadable (a Gradle warning says so). |
| Benchmark | `:app:assembleBenchmark` | Release-like, debug-signed, `.benchmark` suffix, used by `:benchmark` |

### 2.2 Test hooks (debug build only)

Load once per PowerShell session: `. .\verification\debug-tools.ps1`

| Function | Effect |
|---|---|
| `Reset-PickleApp` | `pm clear` — wipes all app data (back to first run) |
| `Start-PickleApp` / `Stop-PickleApp` | Launch / force-stop |
| `Get-PickleStatus` | Returns today, isPro, proSince, matchCount, streakCurrent, streakLongest, skippedWeeks, skipsHeld (computed by the same engine the UI uses) |
| `Set-PicklePro -State on/off [-DaysAgo n]` | Grants Pro with `proSince = now − n days` / records a refund for the debug token |
| `Add-PickleMatches -Count n [-DaysAgo d]` | Seeds n singles matches vs "Debug Dave" at "Debug Courts", every 3rd a loss, scores 11–7, 11–9, dated d days ago (default 400) |
| `Add-PickleStreak -Weeks n [-EndOffset k] [-Gaps "a,b"]` | One match per week for weeks `k … k+n−1` ago, skipping listed offsets |
| `Add-PickleMatchToday [-DaysAgo d]` | One match |
| `Clear-PickleMatches` | Deletes every match through the repository (photos too) |
| `Complete-PickleOnboarding` | Sets name "Tester", marks onboarding done |
| `Send-PickleReminder -Kind plain/skip` | Posts the streak notification immediately (bypasses the schedule) |
| `Start-PickleScenario <name> [-Reset]` | `first-match`, `cap-40/48/50`, `milestone-4/8/12/26/52`, `missed-skip`, `skip-used`, `pro-unlocked`, `pro-themes`, `export-prompt`, `reminder` |

> These hooks write through the real repositories, so they are valid setup for UI tests. They are **not** valid evidence for billing tests: `Set-PicklePro` bypasses Google Play.

### 2.3 Device switches you will need

Airplane mode · Developer options → **Don't keep activities** · Font size **Largest** + Display size **Largest** · TalkBack · Auto date & time **off** (date/time-zone tests) · Dark theme (system) · Settings → Apps → Picklelog → Notifications · a device without a camera (`-camera-back none`).

### 2.4 Accounts and sandboxes

| Need | Setup |
|---|---|
| Play Console license tester | Add the tester's Google account under Play Console → Settings → License testing |
| In-app product | `picklelog_pro`, one-time, **active**, on a testing track the tester has joined |
| Test cards | Use Play's tester cards ("Test card, always approves", "…always declines", "Slow test card, approves after a few minutes", "Slow test card, declines after a few minutes"). **Never use a real payment method.** |
| Refunds | Play Console → Order management → Refund (for PAY-20 only) |
| Google backup | ENV-C/ENV-D on the same Google account, Backup on, Wi-Fi |

No other third-party sandbox exists.

### 2.5 Reference dataset RD-7 (used throughout §5)

Enter by hand once on a fresh install (name **Matthew** at onboarding). **Week = ISO week, Monday–Sunday.** `W0` = current week, `W-1` = last week, etc. If today is Monday or Tuesday, still use those weekdays from the earlier weeks.

| # | Week / day | Format | Result | Time | Location | Partner | Opponent(s) as typed | Games | Paddle | Notes | Photo |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **D1** | W0, today | Doubles | Win | 18:00–19:30 | Rizal Park Courts | Ana | Dave R., Maya | 11–7, 11–9 | Selkirk Vanguard | Long rally on match point | – |
| **D2** | W-1, Wed | Singles | Loss | 07:00–07:45 | Alabang Club | – | Dave R. | 9–11, 8–11 | Joola Hyperion | – | – |
| **D3** | W-2, Wed | Doubles | Win | – (clear start) | Rizal Park Courts | Ana | Leo | – | – | – | – |
| **D4** | W-3, Wed | Singles | Loss | – | – | – | – | – | – | – | – |
| **D5** | W-5, Mon | Doubles | Loss | – | Alabang Club | Ben | `DAVE R.` (all caps) | 5–11 | – | – | – |
| **D6** | W-5, Wed | Singles | Win | – | – | – | José | – | – | – | 1 photo |
| **D7** | W-6, Fri | Doubles | Win | 23:00–00:30 | – | – | – | – | – | – | – |

> A new match pre-fills **today** and the **current time as Start**. For "Time –" rows, tap "Clear start time".

**Expected figures for RD-7** (derived from the code's rules; use as oracles):

| Figure | Value |
|---|---|
| Total / record / win rate | 7 · **4W – 3L** · **57%** |
| Singles / doubles | 1–2 (33%) · 3–1 (75%) |
| Current / longest streak | **4 weeks / 4 weeks** (W0…W-3; gap W-4) |
| "Last 5" dots, oldest → newest | D6 W · D4 L · D3 W · D2 L · D1 W |
| People created | Dave R., Maya, Ana, Leo, Ben, José (**6**; `DAVE R.` binds to Dave R.) |
| Newest first | D1, D2, D3, D4, D6, D5, D7 |
| Oldest first | D7, D5, D6, D4, D3, D2, D1 |
| Wins first | D1, D3, D6, D7, D2, D4, D5 |
| Losses first | D2, D4, D5, D1, D3, D6, D7 |
| Opponent A–Z (no opponent last) | D1, D2, D5 (Dave R.) · D6 (José) · D3 (Leo) · D4, D7 |
| Location A–Z (no location last) | D2, D5 (Alabang Club) · D1, D3 (Rizal Park Courts) · D4, D6, D7 |
| Shortest first (no duration last) | D2 (45 min) · D1, D7 (1 h 30 min, tie → newest first) · D3, D4, D6, D5 |
| Longest match first | D1, D7 · D2 · D3, D4, D6, D5 |
| Pro head-to-head | Dave R. 1–2 (3 matches) · Maya 1–0 · Leo 1–0 · José 1–0 |
| Pro partners | Ana 2–0, Ben 0–1 (both "Needs 3+ matches together") |
| Pro courts / paddles | Rizal Park Courts 2–0, Alabang Club 0–2 · Selkirk Vanguard 1–0, Joola Hyperion 0–1 |

### 2.6 Other synthetic data

| Data | Value |
|---|---|
| Long text (BVA) | 31-char name `ABCDEFGHIJKLMNOPQRSTUVWXYZ12345`; 30-char `ABCDEFGHIJKLMNOPQRSTUVWXYZ1234`; 500-char opponent/location/notes strings (generate: `'x'*500` in PowerShell) |
| International text | `José Ñúñez`, `Đặng Thị Ngọc`, `山田太郎`, `محمد`, `Zoë 👋🏽`, `ⓐⓑⓒ` |
| Injection-shaped text | `Dave'; DROP TABLE match;--`, `%`, `_`, `\`, `<img src=x onerror=alert(1)>`, `</div><script>alert(1)</script>` |
| Photos | A 12 MP JPEG; a 4000×3000 portrait JPEG with EXIF rotation 90°; a PNG with transparency; a HEIC (API 30+ device); a 0-byte `.jpg`; a GIF; a cloud-only Google Photos item (to simulate unreadable) |
| Import fixtures | Appendix B (all synthetic) |

### 2.7 Setup and cleanup

- **Clean slate:** `Reset-PickleApp` (or uninstall). Then onboarding runs.
- **Skip onboarding for non-onboarding tests:** `Reset-PickleApp; Complete-PickleOnboarding; Start-PickleApp`.
- **Reset only matches/Pro:** `Reset-PickleState`.
- **Seed 1,000 matches** (performance): `Add-PickleMatches -Count 1000` with Pro on (`Set-PicklePro -State on` first, otherwise matches beyond 50 are still written by the hook — the hook bypasses the cap — which makes it an "over-cap free" state, useful only for §8 edge tests). The benchmark build has its own `SeedProvider`.
- **Clear notifications/alarms:** force-stop the app, then `adb shell cmd notification cancel_all` is not universal — clear from the shade instead.
- Delete exported JSON files from Downloads/Drive after `BAK-*` tests.

### 2.8 Known limitations of the test environment

- Billing cannot be exercised on ENV-A (no Play) and gives `BILLING_UNAVAILABLE`; that is itself a test (`PAY-15`).
- Cloud backup restore needs real devices and a Play-track install; `adb install` does not trigger restore.
- WorkManager timing is approximate; reminder tests allow ±15 min.
- The share-sheet result is not observable by the app (Android gives no callback). Tests check what the app does, not whether Instagram posted.

---

## 3. Feature Inventory

Status values: `Not Tested` · `Pass` · `Fail` · `Blocked` · `N/A`. Update the **Status** column from the tracker (§11) once every linked test has run.

| ID | Feature | Purpose | Route / module | Entitlement | Dependencies | Expected behaviour (summary) | Test cases | Status |
|---|---|---|---|---|---|---|---|---|
| F-01 | App launch & bootstrap | Start app, build DI container, sweep orphan photos, start billing check, schedule export prompts, re-arm reminder | `MainActivity`, `PicklelogApplication` | Any | Room, DataStore, WorkManager, Billing | Opens to Onboarding or Home with no crash, theme applied before first frame | NAV-01…03, PRF-01, CMP-01, CMP-02 | Not Tested |
| F-02 | Onboarding + required name | First-run intro, privacy/backup honesty, collect display name | `OnboardingRoute` | Any | ProfileRepository, AppSettingsStore | 4 pages; Skip → last page; name 1–30 chars after trim required; "Get started" → Home; never shown again unless erased | ONB-01…16 | Not Tested |
| F-03 | Log a match (create) | Record format, result, date, times, players, games, location, paddle, notes, photos | `MatchEditRoute()` | Free ≤ 50 / Pro | MatchRepository, PersonRepository, LastUsedFormatStore | Only Result is required; saves in one tap; returns Home with confirmation | LOG-01…30 | Not Tested |
| F-04 | Edit a match | Change any field | `MatchEditRoute(id)` | Any | same | Pre-filled; title "Edit match"; createdAt kept, updatedAt changes; no cap check | EDT-01…08 | Not Tested |
| F-05 | Players & autocomplete | Reuse people, suggest names, prevent duplicates | Match editor | Any | PersonRepository | Prefix/word-start, accent-insensitive suggestions (max 5); case/space-insensitive identity; duplicate slot blocks save | AUT-01…12 | Not Tested |
| F-06 | Game scores & result advisory | Optional per-game scores, advisory when result disagrees | Match editor | Any | — | 0–999 digits only; incomplete game blocks save; advisory "Scores suggest a win/loss" never auto-changes result | LOG-10…18 | Not Tested |
| F-07 | Time range & duration | Start/end, duration, crosses midnight | Editor, detail, card, sort | Any | — | End < start ⇒ "(next day)", duration wraps 24 h | LOG-19…23, SRT-07…08 | Not Tested |
| F-08 | Log another | Clone context of a just-saved match | Home confirmation / first-match sheet | Any | — | Copies format, date, players, location; not result/scores/notes/photos/paddle/end time; start = now | LAN-01…06 | Not Tested |
| F-09 | Match list | Paged list (50, prefetch 15), rows with result badge, scores, thumbnail, date | `HomeRoute` | Any | Room | Correct order, paging, empty/no-results states | LST-01…12 | Not Tested |
| F-10 | Sort | 8 orders, persisted | Home | Any | MatchSortStore | Order per §2.5; survives restart | SRT-01…10 | Not Tested |
| F-11 | Search | Opponents, partners, places, paddle, notes | Home | Any | Room `LIKE` | 300 ms debounce; trims; `%`/`_` literal; survives rotation, not restart | SCH-01…13 | Not Tested |
| F-12 | Filters | Format, result, date range (presets/custom), opponent, location | Home filter sheet + chips | Any | Room | AND-combined; chips removable; live count "Show N matches" | FLT-01…17 | Not Tested |
| F-13 | Dashboard header | Name, record, WR, last 5, streak; info sheet | Home | Any | Stats engine | Record/WR follow filters; streak and last-5 always all matches | DSH-01…12 | Not Tested |
| F-14 | Stats screen (free) | Record, streak, by format, locked Pro preview | `StatsRoute` | Free | — | Same filter as Home; "More with Pro" teaser → paywall | STA-01…08 | Not Tested |
| F-15 | Pro stats | Key numbers, heatmap, form, weekdays, rivals, partners, courts, paddles, months | `StatsRoute` | Pro | AdvancedStats, ProInsights | Figures per §2.5; <3 matches ⇒ no %; form needs 11 matches | PST-01…14 | Not Tested |
| F-16 | Weekly streak | Weeks in a row with ≥1 match | Dashboard, stats, paywall | Any | StreakEngine, WeekKey | ISO weeks; alive if this or last week played | STK-01…10 | Not Tested |
| F-17 | Streak insurance (Pro) | Auto-skip a missed week, 1 earned/month, max 2 held | Dashboard, stats, notices | Pro | StreakInsurance | Skipped weeks don't add to count; notice shown once | INS-01…08 | Not Tested |
| F-18 | Celebration sheets | First match "Your log has hatched", streak milestones 4/8/12/26/52, Pro unlocked | Home / app root | Any | — | Shown once per trigger, only in foreground | MIL-01…07 | Not Tested |
| F-19 | Match detail | View all fields, number, games won, points, photos | `MatchDetailRoute` | Any | — | Shows #N chronological number; Edit/Share/Delete | DET-01…05, DET-11 | Not Tested |
| F-20 | Delete match | Remove match + photo files | Detail | Any | Room, PhotoStore | Confirm dialog; cascade; people kept | DET-06…10, DAT-03 | Not Tested |
| F-21 | Photos | Gallery multi-pick, camera, reorder, cover, remove, viewer, compression | Editor, detail, list | Free 1 / Pro 10 per match | Photo Picker, Camera, PhotoImportQueue, ImageCompressor (2048 px, JPEG 80) | Over-limit picks truncated with prompt; failures recoverable | PHO-01…22 | Not Tested |
| F-22 | Share card | Render 1080-wide JPEG via locked-down WebView, share intent | `ShareRoute` | Any | WebView, FileProvider | No result on card; name on card; wordmark on free | SHR-01…07, SHR-14…20 | Not Tested |
| F-23 | Card variants | Story/Square, Dark/Light (+Court/Sunset Pro), show/hide details, photo on/off | Share | Pro themes | CardFormatStore | Persisted format; details hidden per session only | SHR-08…13 | Not Tested |
| F-24 | Free-tier cap & warnings | 50-match cap; banners at 40 and 48; block at 50 | Home, editor, paywall | Free | CanAddMatch, CapWarning | Never deletes or hides data; save-after-unlock | CAP-01…12 | Not Tested |
| F-25 | Paywall & Play Billing | Price, buy, restore, pending, errors, unlocked sheet | `PaywallRoute` | Free | Play Billing 9.1 | Pro granted only on verified PURCHASED; never revoked except refund signal | PAY-01…24 | Not Tested |
| F-26 | Upgrade prompts | Photo limit, Pro photo limit, Pro card theme, missed skip, locked stats | Editor, share, home, stats | Free/Pro | — | "See Pro"/"Not now"/"Got it" | PHO-03…05, SHR-11, INS-06, STA-02, STA-03 | Not Tested |
| F-27 | Export (JSON) | Share a `picklelog-export-YYYY-MM-DD.json` | `BackupRoute` | Any | JsonExporter, FileProvider | No photos in file; "Last export" updated | BAK-01…07 | Not Tested |
| F-28 | Import (JSON) | Merge an export file | `BackupRoute` | Free capped / Pro | JsonImporter, ImportCap | All-or-nothing; merge by id; summary; 6 rejection messages | BAK-08…22, BAK-32…34 | Not Tested |
| F-29 | Export reminders | Banner after +25 matches or 30 days | Home | Any | WorkManager, ExportPromptPolicy | One pending prompt at a time; dismiss persists | BAK-23…27 | Not Tested |
| F-30 | Android Auto Backup / device transfer | OS backup of DB + prefs; photos only in device transfer | Manifest + XML rules | Any | Google backup | Cloud restore keeps matches, drops photos | BAK-28…31 | Not Tested |
| F-31 | Settings drawer | Pro card, name, dark mode, sound, reminder, backup, cache, privacy, rate, support, about, erase, version | Home drawer | Any | various | Each row works; state persists | SET-01…22 | Not Tested |
| F-32 | Streak reminders | Weekly notification Fri→Sun when streak at risk; custom time | Settings + WorkManager | Any | POST_NOTIFICATIONS (API 33+) | At most one per week; tap opens logger | RMD-01…17 | Not Tested |
| F-33 | Erase all data | Wipe matches, photos, name, prefs; keep Pro & theme | Settings | Any | RoomLocalDataEraser | Two confirmations; back to onboarding | ERS-01…10 | Not Tested |
| F-34 | Privacy / About / Support | Static info, licences, mascot, rate/share app | Routes | Any | Play Store intent | Correct copy; Donate row inert | SET-14…22 | Not Tested |
| F-35 | Dark theme | Light/dark app theme | Settings | Any | AppSettingsStore | Starts following system; once flipped, fixed | SET-06…08, CMP-08 | Not Tested |
| F-36 | Sound effects | ~30 cues, toggle | Global | Any | SoundPool | Off ⇒ silent everywhere | SET-09…10 | Not Tested |
| F-37 | Mascot (duck) | Animated states in onboarding, empty states, sheets, banners, about (pettable) | Many | Any | Compose animation | Never blocks input. **No reduced-motion handling found in code** (see A11Y-12, §13 R-12) | UI-01…06, A11Y-12 | Not Tested |
| F-38 | Process-death & state restoration | Drafts, filters, search, dialogs survive recreation | All | Any | SavedStateHandle | No lost input, no duplicate saves | NAV-04…12 | Not Tested |
| F-39 | Debug tools provider | Test hooks | Debug build only | — | — | Must **not** exist in release | SEC-08 | Not Tested |

---

## 4. Test Strategy and Priorities

### 4.1 Approach

1. **Automated first (§12, `AUTO-*`).** The repo already has ~640 JVM tests and ~49 instrumented test classes. Run them before any manual pass; a red automated suite blocks manual testing of the same area.
2. **Smoke (§9.1)** on every new build, on ENV-B. Stop if any smoke test fails.
3. **Feature tests (§5)** module by module on ENV-B, using RD-7 (§2.5) where stated.
4. **End-to-end journeys (§6)** on ENV-C (real phone, real Play).
5. **Negative, edge, non-functional (§7, §8)** across ENV-A…E.
6. **Exploratory** 30-minute charters at the end of each module (suggested charters in §13.3).

### 4.2 Risk-based priorities (adapted to a local-only app)

| Priority | Meaning for Picklelog | Examples |
|---|---|---|
| **P0 — Critical** | User data lost or corrupted; Pro wrongly revoked or wrongly granted for free in release; any data leaves the device without the user asking; crash that blocks launching or logging a match; release build ships debug hooks | Import partially writes then fails; erase removes Pro; Save crashes; card WebView fetches a remote URL |
| **P1 — High** | A major feature broken with no workaround; a data-integrity error the user can see (wrong stats/streak); a core journey blocked | Filter returns wrong matches; streak count wrong; export file unreadable; reminder never fires |
| **P2 — Medium** | Partial malfunction with a workaround; moderate usability or accessibility problem | Suggestion list doesn't dismiss; TalkBack label missing on one control; wrong plural |
| **P3 — Low** | Cosmetic; rare edge case with negligible impact | 1 dp misalignment; sound plays twice |

**Severity vs priority.** *Severity* is the impact of a defect (Critical / Major / Minor / Trivial). *Priority* is how soon it must be fixed (P0–P3). They usually align, but not always: a typo in the paywall price line is Trivial severity but may be P1 because it's on the money screen.

### 4.3 Entry and exit criteria per test cycle

| Entry | Exit |
|---|---|
| Build installs and launches on ENV-A and ENV-B (CI device-matrix green) | All P0 tests executed and **Pass** |
| `AUTO-01…06` green | ≥ 95% of P1 tests executed and Pass; every P1 Fail has an agreed decision |
| Test data RD-7 can be entered | No open Critical/Major defects without a written waiver (§14) |

### 4.4 Test-type coverage map

| Category (brief §4) | Where |
|---|---|
| A Functional | §5 |
| B User flow / E2E | §6, NAV-* |
| C Authentication & authorization | **N/A** (no accounts). Replaced by onboarding gate (ONB), entitlement gating (CAP/PAY/PST/SHR), local data isolation (SEC) |
| D UI/UX | UI-* in §5.20, plus per-feature UI tests |
| E Form validation | §7.1 (all forms: onboarding name, match editor, settings name, reminder time, filter custom range, import file) |
| F API / backend / DB | No API. Database + Play Billing + WorkManager integration: DAT-*, PAY-*, BAK-*, RMD-* |
| G Error handling | §7.2 |
| H Security & privacy | §8.1 |
| I Responsive & compatibility | §8.3 |
| J Accessibility | §8.2 |
| K Performance | §8.4 |
| L Data integrity | §7.3 (DAT-*) |
| M Notifications & integrations | RMD-*, PAY-*, SHR-*, BAK-*, PHO-* |

---

## 5. Detailed Test Cases by Feature

Column key: **Pri** priority · **Type** see header · **Pre / Data** preconditions and test data · **Actual / Status / Evidence / Bug** are for the tester. Default Status is `Not Tested`.

Unless stated: ENV-B, debug build, onboarding completed (`Complete-PickleOnboarding`), Free, sound on, light theme.

### 5.1 Onboarding (F-02)

Pre for all: `Reset-PickleApp; Start-PickleApp` (fresh install state).

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| ONB-01 | Fresh install opens onboarding page 1 | P0 | FUN | Fresh install | 1. Launch app | Page shows lead "Track every match, every point, every memory.", title "Log a match in seconds", phone illustration; indicator reads "Step 1 of 4"; buttons "Next" and "Skip". No permission dialog appears. | | Not Tested | | |
| ONB-02 | Next walks all 4 pages in order | P1 | FUN | — | 1. Tap "Next" ×3 | Titles in order: "Watch your game add up" → "Share your wins" → "Yours, and only yours". Indicator "Step 2/3/4 of 4". Last page shows name field "Your name" (placeholder "e.g. Matthew"), "Get started" button and the note "Automatic backup depends on your Google storage…". | | Not Tested | | |
| ONB-03 | Skip jumps to the last page | P1 | FUN | Page 1 | 1. Tap "Skip" | Pager animates to page 4 (name field visible). App does **not** go to Home. | | Not Tested | | |
| ONB-04 | System Back goes to previous page, then exits | P2 | FUN | Page 3 | 1. Press Back 2. Press Back 3. Press Back | 1 → page 2; 2 → page 1; 3 → app closes (Back on page 1 is not intercepted). | | Not Tested | | |
| ONB-05 | Swipe between pages | P3 | UI | Page 1 | 1. Swipe left ×3 2. Swipe right ×1 | Pages follow the swipe; indicator stays in sync. | | Not Tested | | |
| ONB-06 🔴 | Valid name completes onboarding | P0 | FUN | Page 4 | 1. Type `Matthew` 2. Tap "Get started" | Welcome cue plays; Home opens with header showing "Matthew"; Back from Home exits the app (onboarding is not on the back stack). | | Not Tested | | |
| ONB-07 | Empty name is rejected | P1 | NEG | Page 4, field empty | 1. Tap "Get started" | Field shakes, turns error colour, supporting text "A name is required."; stays on page 4. Tap again: same (no crash, no navigation). | | Not Tested | | |
| ONB-08 | Whitespace-only name is rejected | P1 | NEG | `"   "` | 1. Type 3 spaces 2. Tap "Get started" | Same as ONB-07. | | Not Tested | | |
| ONB-09 | Name is trimmed | P2 | BVA | `"  Matthew  "` | 1. Enter 2. Get started 3. Open Settings | Header and Settings → Name show `Matthew` with no leading/trailing spaces. | | Not Tested | | |
| ONB-10 | 30-character limit | P2 | BVA | 31-char string (§2.6) | 1. Type/paste 31 characters | Field stops at 30 chars (`…1234`); the 31st is dropped. Get started succeeds. Header shows the full 30 chars without clipping off-screen. | | Not Tested | | |
| ONB-11 | Emoji/surrogate at the limit is not split | P3 | EDGE | 29 letters + `👋🏽` | 1. Paste | Field never contains half an emoji (no `�`). | | Not Tested | | |
| ONB-12 | Keyboard "Done" submits | P3 | FUN | Page 4, `Matthew` | 1. Press IME Done | Same as tapping Get started. | | Not Tested | | |
| ONB-13 | Not shown again after completion | P0 | REG | ONB-06 done | 1. Force-stop 2. Relaunch | Home opens directly. | | Not Tested | | |
| ONB-14 | Install with existing data skips onboarding | P1 | EDGE | Debug: `Reset-PickleApp; Add-PickleMatchToday` *before* first UI launch (provider call starts the app process) then `Start-PickleApp` | 1. Launch | Onboarding is **skipped** because matches exist (rule: name present or matches > 0 ⇒ onboarding complete). Header shows no name. | | Not Tested | | |
| ONB-15 | Notification tap during onboarding doesn't bypass it | P1 | INT | Fresh install, `Send-PickleReminder` | 1. Tap the notification | Onboarding page is shown (not the logger). After completing it, Home opens — the logger does **not** auto-open. | | Not Tested | | |
| ONB-16 | Rotation keeps page and typed name | P2 | FUN | Page 4, typed `Matt` | 1. Rotate 2. Rotate back | Still page 4, text `Matt` kept. | | Not Tested | | |

### 5.2 Log a match — create (F-03, F-06, F-07)

Pre: Home, onboarding done. "Editor" = the New match sheet.

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| LOG-01 🔴 | Minimum match: result only | P0 | FUN | Empty list | 1. Tap "Log your first match" 2. Tap "Win" 3. Tap "Save" | Sheet closes; list has 1 row "Doubles match", badge **W**, date "Today"; first-match sheet "Your log has hatched" appears (MIL-01). | | Not Tested | | |
| LOG-02 | Defaults on a new match | P1 | FUN | First ever match | 1. Tap FAB "Log a match" | Title "New match"; format **Doubles** (first-launch default); date = today; Start = current time (hh:mm); End not set (pill "End time"); no result selected; "Required" shown next to Result; 2 opponent slots + Partner visible. | | Not Tested | | |
| LOG-03 | Last-used format is remembered | P2 | FUN | Save a Singles match | 1. Open FAB again | Format pre-selected **Singles**. Survives force-stop/relaunch. | | Not Tested | | |
| LOG-04 🔴 | Save without result is blocked | P0 | NEG | New match, nothing selected | 1. Tap "Save" | Not saved. Message "Pick Win or Loss to save this match." appears near Result; soft error cue. Tap Save again → still blocked, cue plays again. Selecting "Win" removes the message. | | Not Tested | | |
| LOG-05 | Full doubles match (D1) persists every field | P0 | FUN | RD-7 D1 | 1. Enter all D1 fields 2. Save 3. Open the row | Detail shows Doubles, Win, today, 6:00 PM – 7:30 PM, duration 1 h 30 min, partner Ana, opponents Dave R. & Maya, games 11–7 / 11–9, "Won 2 of 2", location, paddle, notes. | | Not Tested | | |
| LOG-06 | Singles shows one opponent slot, no partner | P1 | UI | — | 1. Tap "Singles" | Players heading "OPPONENT", one field "Opponent"; Partner hidden. | | Not Tested | | |
| LOG-07 | Doubles → Singles clears extra players with a notice | P1 | FUN | Doubles with Opponent 2 `Maya` and Partner `Ana` | 1. Tap "Singles" 2. Tap "Doubles" | 1: "Switched to singles — cleared the partner and second opponent." shown; Opponent 1 kept. 2: Opponent 2 and Partner are empty (not restored). | | Not Tested | | |
| LOG-08 | Doubles → Singles with nothing to clear shows no notice | P3 | UI | Doubles, only Opponent 1 filled | 1. Tap "Singles" | No "Switched to singles…" message. | | Not Tested | | |
| LOG-09 | Doubles with fewer than 2 opponents / no partner saves | P2 | FUN | Doubles, Opponent 1 only | 1. Win 2. Save | Saves; row "vs <name>", no "with". | | Not Tested | | |
| LOG-10 | Add a game row | P1 | FUN | — | 1. Tap "Game" pill | Row "Game 1" with fields "Me" and "Them", numeric keyboard. Pop-in cue. | | Not Tested | | |
| LOG-11 | Scores accept digits only, max 3 | P1 | BVA | Game 1 | 1. Type `1a2` in Me 2. Type `12345` in Them | Me shows `12`; Them shows `123`. No minus sign, decimal point or space accepted (paste `-5`, `5.5`, ` 7` → `5`, `55`, `7`). | | Not Tested | | |
| LOG-12 | Incomplete game blocks save | P1 | NEG | Game 1 Me=11, Them empty, result Win | 1. Tap Save | Not saved; game row shows "Enter both scores or remove this game."; Save is not tappable/does nothing. Filling Them=7 clears the error and Save works. | | Not Tested | | |
| LOG-13 | Blank game row is ignored on save | P2 | FUN | Add 2 games, fill only Game 1 (11–7), leave Game 2 fully blank | 1. Win 2. Save 3. Open detail | Saved with **one** game (11–7). | | Not Tested | | |
| LOG-14 | Remove a game renumbers the rest | P2 | FUN | Games 11–7, 9–11, 11–5 | 1. Tap "Remove game 2" | Remaining rows read "Game 1" 11–7 and "Game 2" 11–5. Saved games numbered 1, 2. | | Not Tested | | |
| LOG-15 | Advisory when scores contradict the result | P2 | FUN | Games 9–11, 8–11 | 1. Tap "Win" | "Scores suggest a loss" shown. Result stays **Win** (not auto-changed). Save works. | | Not Tested | | |
| LOG-16 | No advisory when scores agree or tie | P3 | FUN | (a) 11–7 + Win (b) 11–7, 7–11 + Win (c) 10–10 + Loss | 1. Observe | No advisory in a, b (1–1 games) or c (tied game counts for nobody). | | Not Tested | | |
| LOG-17 | Zero and 999 scores | P3 | BVA | 0–0, 999–0 | 1. Save | Saves; detail shows "Game 1: 0–0", "999–0"; list row doesn't overflow. | | Not Tested | | |
| LOG-18 | Many games | P3 | BVA | 10 game rows | 1. Fill all 2. Save | All 10 saved in order; detail and list readable; card render works (SHR-17). | | Not Tested | | |
| LOG-19 | Date picker blocks future dates | P1 | BVA | — | 1. Tap the Date button ("Date, <today>. Change date") 2. Try tomorrow | Future days disabled; hint "You can't log a match in the future!"; today selectable. | | Not Tested | | |
| LOG-20 | Past date | P1 | FUN | Pick a date 400 days ago | 1. Save | Row dated with full date; sorts correctly; streak unaffected unless within last 2 weeks. | | Not Tested | | |
| LOG-21 | Clear start time | P2 | FUN | — | 1. Tap "Clear start time" 2. Save | Saved without time; detail has no Time row; no duration. | | Not Tested | | |
| LOG-22 | End time and duration | P1 | FUN | Start 07:00 | 1. Tap "End time" 2. Pick 07:45 | "Duration: 45 min" shown live. Detail "7:00 AM – 7:45 AM". | | Not Tested | | |
| LOG-23 | Crossing midnight | P1 | EDGE | Start 23:00, End 00:30 (D7) | 1. Set times | "Duration: 1 h 30 min (ends the next day)"; detail "12:30 AM (next day)". Equal start/end → duration 0 min, no "next day". | | Not Tested | | |
| LOG-24 | Location, paddle, notes trimmed; blanks become empty | P2 | FUN | Location `"  Rizal  "`, paddle `"   "`, notes `"\n\n"` | 1. Save 2. Edit | Location `Rizal`; paddle and notes empty (not saved as spaces). | | Not Tested | | |
| LOG-25 | Multi-line notes keep line breaks | P3 | FUN | Notes `line1⏎line2 🎉` | 1. Save 2. Open detail | Detail shows two lines and the emoji. | | Not Tested | | |
| LOG-26 | "Recent" location chips | P2 | FUN | RD-7 entered | 1. New match | Up to 3 recent locations (most recent play date first, de-duplicated ignoring case/accents) shown under "Recent"; tap fills Location. | | Not Tested | | |
| LOG-27 | Close discards a new match | P1 | FUN | Fill Win + opponent | 1. Tap "Close" (or system Back) | Sheet closes; **no** match created. ⚠ No "discard changes?" confirmation exists — record whether that is acceptable (§13 R-04). | | Not Tested | | |
| LOG-28 🔴 | Double-tap Save creates one match | P0 | NEG | Win selected | 1. Double-tap "Save" quickly (or tap 5× rapidly) | Exactly **one** new row; count +1. | | Not Tested | | |
| LOG-29 | Save confirmation and "Log another" on 2nd+ match | P1 | FUN | ≥1 match exists | 1. Save a new match | Home shows "Match saved" with "Log another" action; match-saved cue. Dismissing removes it. | | Not Tested | | |
| LOG-30 | Sheet animation never shows a blank sheet | P3 | UI | Slow device | 1. Tap FAB 10× in a row (open/close) | Sheet slides up only after content is ready; no empty white sheet, no stuck scrim. | | Not Tested | | |

### 5.3 Edit a match (F-04)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| EDT-01 🔴 | Editor pre-fills every field | P0 | FUN | RD-7 D1 | 1. Open D1 2. Tap "Edit" | Title "Edit match"; every field, game, time, photo matches detail. | | Not Tested | | |
| EDT-02 🔴 | Change and save persists; no duplicate | P0 | FUN | D1 | 1. Edit result → Loss, location → `Alabang Club` 2. Save | Detail updates immediately; list row updates; total count unchanged (7); match-updated cue (not match-saved). | | Not Tested | | |
| EDT-03 | Created timestamp kept, number kept | P1 | DATA | D2 (number #6 of 7 by date order) | 1. Edit notes 2. Save | Detail still shows the same "#N"; list position unchanged (sort keys date/createdAt unchanged). | | Not Tested | | |
| EDT-04 | Changing the date re-sorts and renumbers | P1 | DATA | D7 (oldest) | 1. Change date to today 2. Save | D7 moves to the top (Newest first); its #N becomes 7; D1 becomes #6. Streak recomputed. | | Not Tested | | |
| EDT-05 | Edit at the free cap is allowed | P1 | FUN | `cap-50` | 1. Open any match → Edit 2. Change notes 3. Save | Saves; no paywall (cap only applies to new matches). | | Not Tested | | |
| EDT-06 | Removing all players | P2 | FUN | D1 | 1. Clear all player fields 2. Save | Row reads "Doubles match"; people Dave R./Maya/Ana still exist (autocomplete still suggests them). | | Not Tested | | |
| EDT-07 | Close discards edits | P1 | FUN | D1 | 1. Edit notes 2. Close | Detail unchanged. (No discard confirmation — see LOG-27.) | | Not Tested | | |
| EDT-08 | Edit survives process death | P1 | FUN | Don't keep activities ON | 1. Edit notes to `abc` 2. Home button 3. Return | Draft `abc` still there; Save works once. | | Not Tested | | |

### 5.4 Players & autocomplete (F-05)

Pre: RD-7 entered.

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| AUT-01 | Prefix suggestion | P1 | FUN | — | 1. New match 2. Type `da` in Opponent 1 | Suggestion list shows "Dave R." within ~150 ms; tap → field filled, list closes, snap cue. | | Not Tested | | |
| AUT-02 | Word-start match | P2 | FUN | — | 1. In Opponent 1 type `r` 2. Clear, type `av` | 1: "Dave R." suggested (a word in the name starts with r). Typing `av` (mid-word) does **not** suggest it. | | Not Tested | | |
| AUT-03 | Accent- and case-insensitive suggestion | P2 | FUN | José exists | 1. Type `jose` 2. Type `JOS` | Both suggest "José". | | Not Tested | | |
| AUT-04 | Most recently played first, max 5 | P2 | FUN | Add people `Ab1`…`Ab7` on different dates | 1. Type `ab` | At most 5 suggestions, most recent play date first. | | Not Tested | | |
| AUT-05 | Already-used people are excluded | P2 | FUN | Opponent 1 = Dave R. (picked) | 1. In Opponent 2 type `da` | "Dave R." not suggested. | | Not Tested | | |
| AUT-06 | Same person in two slots blocked | P1 | NEG | Opponent 1 `Ana`, Partner `ana ` | 1. Observe 2. Win 3. Save | Partner shows "Already entered as Opponent 1"; Save does nothing until fixed. | | Not Tested | | |
| AUT-07 🔴 | Case/space variants resolve to one person | P0 | DATA | Typed `DAVE  R.` (double space) without picking a suggestion | 1. Save 2. Filters → Opponent list | Only one "Dave R." in the list; the match row shows "vs Dave R." (original display name kept). | | Not Tested | | |
| AUT-08 | New person created when no match | P1 | FUN | `Zoë 👋🏽` | 1. Type 2. Save 3. New match, type `zo` | Saved; suggestion "Zoë 👋🏽" appears next time. | | Not Tested | | |
| AUT-09 | Picking then editing the text unbinds | P2 | FUN | Pick "Dave R." then append `x` | 1. Save | A new person `Dave R.x` is created; Dave R. unchanged. | | Not Tested | | |
| AUT-10 | Suggestions hide when field loses focus / after pick | P3 | UI | — | 1. Type `da` 2. Tap Notes | List disappears; no stray popup over the keyboard. | | Not Tested | | |
| AUT-11 | Location and paddle suggestions | P2 | FUN | — | 1. Type `riz` in Location 2. Type `sel` in Paddle | "Rizal Park Courts" / "Selkirk Vanguard" suggested. Exact existing value typed fully → no suggestion of itself. | | Not Tested | | |
| AUT-12 | Whitespace-only name ignored | P2 | NEG | Opponent 1 `"   "` | 1. Win 2. Save | Saves with no opponent; no blank person created (Filters → Opponent list unchanged). | | Not Tested | | |

### 5.5 Log another (F-08)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| LAN-01 | Copies context, not outcome | P1 | FUN | Save D1 (2nd+ match so the "Match saved" action shows) | 1. Tap "Log another" | New match: Doubles, same date as D1, Dave R./Maya/Ana, Rizal Park Courts. **Empty**: result, games, paddle, notes, photos, end time. Start = current time. Title "New match". | | Not Tested | | |
| LAN-02 | Saves as a separate match | P0 | FUN | LAN-01 | 1. Win 2. Save | Count +1; original D1 unchanged. | | Not Tested | | |
| LAN-03 | From the first-match sheet | P1 | FUN | Fresh install, save 1st match | 1. On "Your log has hatched" tap "Log another" | Editor opens pre-filled from match 1; sheet gone. | | Not Tested | | |
| LAN-04 | Source deleted meanwhile | P3 | EDGE | Save match, then (debug) `Clear-PickleMatches` before tapping Log another | 1. Tap "Log another" | Plain new match opens (no crash). | | Not Tested | | |
| LAN-05 | At the free cap | P1 | FUN | 49 matches; save #50 | 1. Tap "Log another" 2. Win 3. Save | Paywall opens instead of saving (CAP-04). | | Not Tested | | |
| LAN-06 | Confirmation not shown after editing | P3 | UI | — | 1. Edit any match, Save | No "Match saved / Log another" on Home. | | Not Tested | | |

### 5.6 Match list (F-09)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| LST-01 | Empty state | P1 | UI | No matches | 1. Open Home | Duck mascot, "No matches yet", "Log your first match to start your record. Play at least once a week to build a streak.", button "Log your first match". No sort/search/filter clutter that does nothing. | | Not Tested | | |
| LST-02 🔴 | RD-7 default order | P0 | FUN | RD-7 | 1. Open Home | Rows in order D1, D2, D3, D4, D6, D5, D7; toolbar "Sort: Newest first". | | Not Tested | | |
| LST-03 | Row content — doubles with partner | P1 | UI | D1 | 1. Inspect row | Badge W; headline "vs Dave R. & Maya"; details include "Doubles · with Ana" (or format/date line) and location "Rizal Park Courts"; scores "11–7", "11–9"; date "Today". | | Not Tested | | |
| LST-04 | Row content — no players | P2 | UI | D4 | 1. Inspect | Headline "Singles match"; badge L; no score chips; no thumbnail. | | Not Tested | | |
| LST-05 | Thumbnail shows the cover photo | P2 | UI | D6 (1 photo) | 1. Inspect | Thumbnail shows the photo, correctly oriented, not stretched. | | Not Tested | | |
| LST-06 | Win/Loss not conveyed by colour alone | P1 | A11Y | RD-7 | 1. Grayscale (Developer options → Simulate color space → Monochromacy) | Badges still readable as "W"/"L". | | Not Tested | | |
| LST-07 | Count line | P2 | UI | RD-7 | 1. Observe count near toolbar | Unfiltered: "7 matches" style count; with a filter that leaves 3: "3 of 7 matches" (`list_count_of_total`). | | Not Tested | | |
| LST-08 | Paging loads beyond 50 | P1 | FUN | Pro, `Add-PickleMatches -Count 120` | 1. Fling to the bottom | All 120 rows reachable; no duplicates; no jump back to top while loading; scroll continues smoothly at rows ~35, ~85. | | Not Tested | | |
| LST-09 | Row tap opens detail | P0 | FUN | RD-7 | 1. Tap D2 | Detail of D2 ("Match", "#6"). | | Not Tested | | |
| LST-10 | List updates live after edit/delete | P1 | FUN | RD-7 | 1. Edit D2 → Win 2. Back 3. Delete D4 | Row badge updates without manual refresh; D4 disappears; header stats update. | | Not Tested | | |
| LST-11 | Header collapses on scroll | P3 | UI | 20+ matches | 1. Scroll down 2. Scroll up | Header collapses smoothly and re-expands; no overlap with toolbar; FAB stays reachable. | | Not Tested | | |
| LST-12 | Dates older than this year show the year | P3 | UI | Match 400 days ago | 1. Inspect | Short date includes year (e.g. "Sep 5, 2025" style); this-year dates omit year. | | Not Tested | | |

### 5.7 Sort (F-10)

Pre: RD-7. Open "Sort: …" menu.

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| SRT-01 | Oldest first | P1 | FUN | — | 1. Choose "Oldest first" | D7, D5, D6, D4, D3, D2, D1; label "Sort: Oldest first"; shuffle cue. | | Not Tested | | |
| SRT-02 | Wins first | P1 | FUN | — | 1. "Wins first" | D1, D3, D6, D7, D2, D4, D5. | | Not Tested | | |
| SRT-03 | Losses first | P1 | FUN | — | 1. "Losses first" | D2, D4, D5, D1, D3, D6, D7. | | Not Tested | | |
| SRT-04 | Opponent A–Z, no-opponent last | P1 | FUN | — | 1. "Opponent A–Z" | D1, D2, D5, D6, D3, D4, D7 (José before Leo; matches without opponents last). | | Not Tested | | |
| SRT-05 | Opponent sort ignores case | P2 | EDGE | Add match vs `aaron` (lowercase) | 1. "Opponent A–Z" | `aaron` sorts **first**, not after Z. | | Not Tested | | |
| SRT-06 | Location A–Z, no-location last | P1 | FUN | — | 1. "Location A–Z" | D2, D5, D1, D3, D4, D6, D7. Case-insensitive (`alabang club` would sort with `Alabang Club`). | | Not Tested | | |
| SRT-07 | Shortest first | P1 | FUN | — | 1. "Shortest first" | D2 (45 min), D1, D7, then D3, D4, D6, D5 (no duration last). D7 is treated as 90 min, not −22.5 h. | | Not Tested | | |
| SRT-08 | Longest match first | P1 | FUN | — | 1. "Longest match first" | D1, D7, D2, then D3, D4, D6, D5. | | Not Tested | | |
| SRT-09 | Sort persists | P1 | FUN | "Wins first" chosen | 1. Force-stop 2. Relaunch | Still "Sort: Wins first". | | Not Tested | | |
| SRT-10 | Sort combines with filter and search | P2 | FUN | Filter Doubles + sort Losses first | 1. Observe | D5, D1, D3, D7. | | Not Tested | | |

### 5.8 Search (F-11)

Pre: RD-7. Field "Search opponents, places, notes".

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| SCH-01 | Opponent name, any case | P1 | FUN | `dave` / `DAVE` | 1. Type | D1, D2, D5 after ~300 ms. | | Not Tested | | |
| SCH-02 | Partner name | P2 | FUN | `ben` | 1. Type | D5 only. | | Not Tested | | |
| SCH-03 | Location substring | P1 | FUN | `rizal` | 1. Type | D1, D3. | | Not Tested | | |
| SCH-04 | Notes and paddle | P2 | FUN | `rally`; `joola` | 1. Type each | D1; D2. | | Not Tested | | |
| SCH-05 | Leading/trailing spaces ignored | P2 | BVA | `"  maya  "` | 1. Type | D1. | | Not Tested | | |
| SCH-06 | Wildcards are literal | P1 | SEC | `%`, `_`, `\` | 1. Type each | "No matches with these filters" (no row contains them). Not "all matches". | | Not Tested | | |
| SCH-07 | SQL-shaped input is harmless | P0 | SEC | `Dave'; DROP TABLE match;--` | 1. Type 2. Clear | No crash; no results; after clearing, all 7 matches still there. | | Not Tested | | |
| SCH-08 | Accented name with accent | P2 | FUN | `josé`, `JOSÉ` | 1. Type | D6. | | Not Tested | | |
| SCH-09 | Accent-less query for accented name | P2 | EDGE | `jose` | 1. Type | **Code behaviour: no result** (search matches the stored lowercase name, which keeps the accent; autocomplete *does* strip accents). Record result; see §13 R-09. | | Not Tested | | |
| SCH-10 | Non-ASCII case in location/notes | P3 | EDGE | Location `ÑANDÚ Court`; search `ñandú` | 1. Type | Record result. SQLite `LIKE` folds only ASCII case, so this likely **fails to match**. See §13 R-09. | | Not Tested | | |
| SCH-11 | No-results state and recovery | P1 | FUN | `zzz` | 1. Type 2. Tap "Clear filters and search" | "No matches with these filters", "Try removing a filter or changing your search."; button clears search and filters, all 7 back. | | Not Tested | | |
| SCH-12 | Clear (✕) and persistence | P2 | FUN | `dave` | 1. Rotate 2. Tap "Clear search" 3. Type `dave`, force-stop, relaunch | 1: search kept. 2: cleared. 3: search **not** kept after a cold start (by design: kept only in saved state). | | Not Tested | | |
| SCH-13 | Search does not change the dashboard | P2 | FUN | `dave` | 1. Observe header | Header still "4W – 3L", 57% (search filters the list only; filters affect the header). | | Not Tested | | |

### 5.9 Filters (F-12)

Pre: RD-7. Open "Filters".

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| FLT-01 | Format filter | P1 | FUN | — | 1. Format → Singles | List D2, D4, D6; button "Show 3 matches"; toolbar "Filters (1)"; chip "Singles"; header "1W – 2L", 33%, "Filtered: Singles". | | Not Tested | | |
| FLT-02 | Result filter | P1 | FUN | — | 1. Result → Losses | D2, D4, D5; chip "Losses". | | Not Tested | | |
| FLT-03 | Opponent filter | P1 | FUN | — | 1. Opponents → Dave R. | D1, D2, D5; chip "vs Dave R.". | | Not Tested | | |
| FLT-04 | Location filter | P1 | FUN | — | 1. Location → Alabang Club | D2, D5; chip "At Alabang Club". | | Not Tested | | |
| FLT-05 | Date preset "This month" | P1 | FUN | — | 1. Date → This month | Only matches dated 1st of this month → today; chip shows the range. | | Not Tested | | |
| FLT-06 | Date presets "Last 3 months", "This year" | P2 | FUN | Add a match 100 days ago | 1. Each preset | Last 3 months = today minus 3 calendar months → today; This year = Jan 1 → today. The 100-day match excluded from Last 3 months, included in This year only if this year. | | Not Tested | | |
| FLT-07 | Custom range | P1 | FUN | — | 1. "Custom range…" 2. Pick W-3 Mon → W-1 Sun 3. OK | D2, D3, D4. Chip "<from> – <to>". | | Not Tested | | |
| FLT-08 | Custom range boundaries are inclusive | P2 | BVA | Range exactly W-1 Wed → W-1 Wed | 1. Apply | D2 included. | | Not Tested | | |
| FLT-09 | Combined filters are ANDed | P1 | FUN | Doubles + Wins | 1. Apply | D1, D3, D7; "Filters (2)". | | Not Tested | | |
| FLT-10 | Remove one chip | P2 | FUN | FLT-09 | 1. Tap ✕ on "Wins" chip ("Remove filter: Wins") | Only Doubles remains: D1, D3, D5, D7; tick-clear cue. | | Not Tested | | |
| FLT-11 | Clear all | P2 | FUN | FLT-09 | 1. Tap "Clear all" (chip row) 2. Re-apply, open sheet, tap "Reset" | All 7 back each time; sweep cue. | | Not Tested | | |
| FLT-12 | Empty opponent/location lists | P3 | UI | Fresh install, 1 match with no players/location | 1. Open sheet | "No opponents logged yet", "No locations logged yet". | | Not Tested | | |
| FLT-13 | Opponent list contains only real opponents | P2 | FUN | RD-7 | 1. Opponents dropdown | **Code behaviour: lists every person, including partner-only people (Ana, Ben).** Choosing Ana gives "No matches with these filters". Record; see §13 R-01. | | Not Tested | | |
| FLT-14 | Deleted opponent still listed | P2 | EDGE | Delete D6 (only José match) | 1. Opponents dropdown | **Code behaviour: José still listed** (people are never deleted); selecting gives 0 results. See §13 R-01. | | Not Tested | | |
| FLT-15 | Location variants by case | P2 | EDGE | Add match at `rizal park courts` (lowercase) | 1. Location dropdown 2. Pick "Rizal Park Courts" | **Code behaviour:** both spellings appear as separate options; filter matches the exact spelling only (lowercase match excluded). Pro "Courts" merges them. See §13 R-02. | | Not Tested | | |
| FLT-16 | Filters survive rotation and process death, not cold start | P2 | FUN | FLT-09, Don't keep activities ON | 1. Background/return 2. Force-stop, relaunch | 1: filters kept. 2: no filters. | | Not Tested | | |
| FLT-17 | Filtered to zero | P2 | FUN | Singles + Wins + Opponent Leo | 1. Apply | "No matches with these filters"; header shows "0W – 0L"-style empty figures without crash or "NaN"; streak still 4 wk. | | Not Tested | | |

### 5.10 Dashboard header (F-13)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| DSH-01 🔴 | RD-7 figures | P0 | FUN | RD-7, name Matthew | 1. Observe header | "Matthew"; "4W – 3L"; "57%" with "WR"; "Last 5" dots W L W L W (oldest left); "4 wk" streak. | | Not Tested | | |
| DSH-02 | Header with no matches | P2 | UI | Empty | 1. Observe | No "NaN"/"—%" glitches; "No streak". | | Not Tested | | |
| DSH-03 | Win-rate boundaries | P2 | BVA | Log by hand: (a) 1 W (b) +1 L (c) +1 L | 1. Observe after each | (a) 100% (b) 50% (c) 33%. The "never 100% with a loss / never 0% with a win" guard (e.g. 199W/1L → 99%, 1W/199L → 1%) is covered by `BasicStatsTest` in AUTO-04 — re-check visually only if Pro with ≥200 matches is set up for PRF tests. | | Not Tested | | |
| DSH-04 | Filters change record & WR, not streak/last 5 | P1 | FUN | Filter Singles | 1. Observe | "1W – 2L", 33%, "Filtered: Singles"; streak still 4 wk; last-5 dots unchanged. | | Not Tested | | |
| DSH-05 | Info sheet | P2 | UI | — | 1. Tap "What's this?" | Sheet "What your header shows" explaining Win rate, Record, Recent results ("Your last 5 matches, oldest on the left…"), Streak ("…even when filters are on."), filters note; "Got it" closes. | | Not Tested | | |
| DSH-06 | Header opens full stats | P1 | FUN | — | 1. Tap the header body | "Your stats" screen opens. | | Not Tested | | |
| DSH-07 | Name updates live | P2 | FUN | — | 1. Settings → Name → `Matt` → Save name 2. Close drawer | Header shows "Matt" without restart. | | Not Tested | | |
| DSH-08 | Empty name (pre-onboarding install, ONB-14) | P3 | EDGE | ONB-14 state | 1. Observe | Header renders without a blank gap or placeholder text. | | Not Tested | | |
| DSH-09 | Numbers animate correctly | P3 | UI | — | 1. Log a win | Record/WR roll to the new values; final values correct. | | Not Tested | | |
| DSH-10 | Long name | P3 | BVA | 30-char name, largest font | 1. Observe | Name ellipsized or wrapped; doesn't overlap the settings icon. | | Not Tested | | |
| DSH-11 | Recent dots use newest 5 regardless of sort | P2 | FUN | Sort Oldest first | 1. Observe | Dots still W L W L W. | | Not Tested | | |
| DSH-12 | Day rollover | P3 | EDGE | App open across midnight | 1. Leave app on Home over midnight 2. Return | "Today" labels and streak refresh correctly after the next data change or reopen; no crash. Record whether a stale "Today" remains until restart. | | Not Tested | | |

### 5.11 Stats screen — free (F-14)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| STA-01 | Free stats content | P1 | FUN | RD-7, Free | 1. Tap header | Title "Your stats", Back arrow. Record: Matches 7, Wins – losses 4–3, Win rate 57%. Streak: Current 4 weeks, Longest 4 weeks, "Your longest streak ever". By format: Singles 1–2, Doubles 3–1. No "Streak skips" row (Pro only). | | Not Tested | | |
| STA-02 | Locked preview | P1 | FUN | Free | 1. Scroll | "You vs Dave R." (top opponent) with sample "7–3" hidden behind "Unlock with Pro"; "More with Pro" list. TalkBack reads "Your record against Dave R. is hidden. Double tap to see it with Pro." | | Not Tested | | |
| STA-03 | Locked preview → paywall | P1 | FUN | Free | 1. Tap "Unlock with Pro" | Paywall opens; Back returns to stats. | | Not Tested | | |
| STA-04 | Stats honour Home filters | P1 | FUN | Filter Losses on Home | 1. Open stats | Filter indicator shown; record 0–3; streak still 4 weeks. | | Not Tested | | |
| STA-05 | No matches | P2 | UI | Empty | 1. Open stats via header | "—"/"None yet" values; "No matches" per format; no crash. | | Not Tested | | |
| STA-06 | Current is not the longest | P2 | FUN | `Add-PickleStreak -Weeks 5 -EndOffset 3` + `Add-PickleMatchToday` | 1. Open stats | Current 1 week, Longest 5 weeks; no "Your longest streak ever". | | Not Tested | | |
| STA-07 | Back returns Home with state | P2 | FUN | Filter + scroll position | 1. Back | Same filter and list position. | | Not Tested | | |
| STA-08 | Rotation | P3 | UI | — | 1. Rotate | Layout fits landscape; no crash. | | Not Tested | | |

### 5.12 Pro stats (F-15)

Pre: RD-7, then `Set-PicklePro -State on` (debug) or a real purchase.

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| PST-01 | Pro badge, sections present | P1 | FUN | Pro | 1. Open stats | "Pro" badge in top bar; locked preview gone; sections: key numbers, Activity heatmap, Form, "When you play", Rivals, Partner chemistry, Courts, Paddles, Month by month. "Streak skips" row present. | | Not Tested | | |
| PST-02 | Rivals table | P1 | FUN | — | 1. Rivals | Dave R. 1–2 first (most matches), then Maya/Leo/José 1–0 ordered by most recent play. Win % only where ≥3 matches (Dave R. 33%); others show "Too few matches for a win rate"-style marker. | | Not Tested | | |
| PST-03 | Partner chemistry | P2 | FUN | — | 1. Partner chemistry | Ana 2–0, Ben 0–1; subtitle "vs your 75% doubles"; footnote "…Needs 3+ matches together."; no delta for <3. | | Not Tested | | |
| PST-04 | Courts and paddles | P2 | FUN | — | 1. Observe | Courts: Rizal Park Courts 2–0, Alabang Club 0–2. Paddles: Selkirk Vanguard 1–0, Joola Hyperion 0–1. | | Not Tested | | |
| PST-05 | Courts merge case/space variants | P2 | DATA | Add W at `rizal  park courts` | 1. Courts | One row "rizal  park courts"-or-latest spelling with 3–0, not two rows. | | Not Tested | | |
| PST-06 | Month by month includes empty months | P2 | FUN | RD-7 spans ~2 months | 1. Month by month | One entry per month from first match month to current month; months without matches show "No matches"; "Wins above the line, losses below it." | | Not Tested | | |
| PST-07 | Form needs 11 matches | P2 | BVA | 10 matches → 11 matches | 1. Form section | 10: "Log 11 matches to see how your form is moving." 11: chart with "Now · N%" and "All-time N%". | | Not Tested | | |
| PST-08 | 90-day trend | P2 | FUN | ≥3 matches in each of the last two 90-day windows | 1. Key numbers | "90-day trend" shows "+N pts"/"−N pts" "vs the 90 days before"; otherwise "Needs 3+ matches in each period". | | Not Tested | | |
| PST-09 | Matches per week / days played / longest run | P2 | FUN | RD-7 | 1. Key numbers | Matches per week **1.0** (7 matches ÷ 7 weeks, W-6…W0 inclusive); days played **7**; longest run of consecutive days **1** (no two RD-7 dates are adjacent); "Last 10 matches" = 4–3, "57% win rate". | | Not Tested | | |
| PST-10 | Heatmap ranges | P3 | UI | — | 1. Toggle "6 mo" / "1 yr" | Grid redraws; today's cell highlighted; legend "Less … More". | | Not Tested | | |
| PST-11 | Weekday chart | P3 | FUN | RD-7 (3 Wednesdays) | 1. "When you play" | Wednesday busiest; best-day sentence uses days with ≥3 matches only. | | Not Tested | | |
| PST-12 | "Show all N" expands long lists | P3 | UI | 12 opponents | 1. Rivals → "Show all 12" → "Show less" | Expands/collapses; scroll position sensible. | | Not Tested | | |
| PST-13 | Pro stats honour filters | P1 | FUN | Filter Singles | 1. Open stats | Rivals: Dave R. 0–1, José 1–0; Partner chemistry empty "No doubles partners logged yet." | | Not Tested | | |
| PST-14 | Pro with zero matches | P2 | EDGE | Pro, no matches | 1. Open stats | "Log a match and your breakdowns will appear here." | | Not Tested | | |

### 5.13 Weekly streak (F-16)

Use `Get-PickleStatus` as a cross-check oracle (it runs the same engine).

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| STK-01 | First match starts a 1-week streak | P1 | FUN | Empty | 1. Log today | "1 wk"; stats "1 week" + "Your longest streak ever". | | Not Tested | | |
| STK-02 | Several matches in one week count once | P1 | FUN | 3 matches this week | 1. Observe | Streak 1. | | Not Tested | | |
| STK-03 | Streak alive if last week played, this week not yet | P1 | FUN | `Add-PickleStreak -Weeks 3 -EndOffset 1` | 1. Observe | 3 wk (W-1, W-2, W-3). | | Not Tested | | |
| STK-04 | Streak broken by a missed week | P1 | FUN | `Add-PickleStreak -Weeks 3 -EndOffset 2` (W-1 missed) | 1. Observe | "No streak"; longest 3. | | Not Tested | | |
| STK-05 | Gap in the middle | P1 | FUN | `Add-PickleStreak -Weeks 6 -EndOffset 1 -Gaps "3"` | 1. Observe | Current 2, longest 3. | | Not Tested | | |
| STK-06 | Week boundary is Monday 00:00 local | P1 | BVA | Device time Sunday 23:58, last match previous week | 1. Log a match 2. Wait past 00:00 Monday 3. Reopen | Sunday match counts for the week just ending. | | Not Tested | | |
| STK-07 | Year boundary / ISO week 53 | P2 | BVA | Matches on 2026-12-28 (Mon, W53) and 2027-01-04 (Mon, W1) — set device date | 1. Observe | Consecutive weeks → 2. | | Not Tested | | |
| STK-08 | Time-zone change | P2 | EDGE | Match logged at 23:30 Sunday in UTC+8; switch device to UTC−5 | 1. Observe | Match date (a calendar date) doesn't move; streak identical. "Today" re-evaluates in the new zone. | | Not Tested | | |
| STK-09 | Back-dated match extends history | P2 | FUN | STK-04 data | 1. Log a match dated in W-1 | Streak becomes 4 (W-1…W-4). Milestone sheet for 4 appears (MIL-03). | | Not Tested | | |
| STK-10 | Deleting a match can break a streak | P2 | FUN | STK-03 | 1. Delete the W-2 match | Current 1, longest 1. | | Not Tested | | |

### 5.14 Streak insurance — Pro (F-17)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| INS-01 | Missed week is covered for Pro | P1 | FUN | `Start-PickleScenario skip-used` (W-2…W-4 played, W-1 missed, Pro since 70 days) | 1. Open Home | Streak **3 wk** (skipped week not counted, but chain unbroken); notice "A streak skip covered the week of <W-1 Monday date>, so your streak is safe." + "N skip(s) left". `Get-PickleStatus` skippedWeeks lists W-1. | | Not Tested | | |
| INS-02 | Skip notice dismiss persists | P2 | FUN | INS-01 | 1. Tap "Dismiss" 2. Force-stop, relaunch | Notice gone and stays gone. | | Not Tested | | |
| INS-03 | Stats show skips held | P2 | FUN | INS-01 | 1. Open stats | "Streak skips" row with "N held" (max 2) and "You earn one each month, up to 2…". | | Not Tested | | |
| INS-04 | Max 2 skips held | P2 | BVA | Pro since 400 days, `Add-PickleStreak -Weeks 6 -EndOffset 1` | 1. Stats | "2 held" (never 3+). | | Not Tested | | |
| INS-05 | Two consecutive misses need two skips | P2 | EDGE | Pro since 400 d; played W-3, W-4, W-5; missed W-1, W-2 | 1. Observe | Both covered if 2 held → streak 3; with only 1 available, streak broken. Compare with `Get-PickleStatus`. | | Not Tested | | |
| INS-06 | Free user sees the missed-skip upsell | P1 | FUN | `Start-PickleScenario missed-skip` (Free) | 1. Open Home | "Missing the week of <date> ended your 3-week streak. A Pro streak skip would have kept it alive." with "See Pro" and "Dismiss". See Pro → Paywall. | | Not Tested | | |
| INS-07 | Missed-skip dismiss persists; no notice for a 1-week streak | P2 | FUN | INS-06; then data with only W-2 played | 1. Dismiss, relaunch 2. Swap data | 1: stays dismissed. 2: no notice (needs ≥2-week streak). | | Not Tested | | |
| INS-08 | Eligibility window around the Pro start date | P1 | DATA | Rule in `StreakInsurance`: a missed week is coverable only if Pro started on or before the **Sunday of the week after** the miss. (a) `missed-skip` data (W-2…W-4 played, W-1 missed) + `Set-PicklePro -State on -DaysAgo 0`. (b) `Reset-PickleState; Add-PickleStreak -Weeks 4 -EndOffset 1 -Gaps "2"` (W-1, W-3, W-4 played, W-2 missed) + `Set-PicklePro -State on -DaysAgo 0` | 1. Observe each | (a) W-1 **is** covered (Pro today ≤ Sunday of W0): streak 3 wk, skip notice shown. (b) W-2 is **not** covered (Sunday of W-1 is before today): streak 1 wk, no skip notice. Both agree with `Get-PickleStatus`. | | Not Tested | | |

### 5.15 Celebration sheets (F-18)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| MIL-01 | First match sheet | P1 | FUN | Empty, save first match | 1. Observe | Hatchling duck, "Your log has hatched", "That's match one, and week one of your streak…", "Log another" and "Got it". No separate "Match saved" banner at the same time. | | Not Tested | | |
| MIL-02 | First-match sheet only once | P2 | FUN | MIL-01 dismissed | 1. Log a 2nd match | Normal "Match saved / Log another" instead. Delete back to 0 and log again → the sheet may show again (record; acceptable). | | Not Tested | | |
| MIL-03 | 4-week milestone | P2 | FUN | `Start-PickleScenario milestone-4` | 1. Observe Home | Sheet "4-week streak", "A month without missing a week.", flame icon, milestone cue. | | Not Tested | | |
| MIL-04 | 8/12/26/52 copy and major cue | P3 | FUN | scenarios `milestone-8/12/26/52` | 1. Observe each | Bodies "Two months…", "Three months…", "Half a year…", "A full year…"; 26 and 52 play the **major** cue. | | Not Tested | | |
| MIL-05 | Not shown for non-milestones or on cold start | P2 | FUN | Streak already 4 at launch | 1. Cold start | No sheet (only a transition observed while the app is open triggers it). 5 → no sheet. | | Not Tested | | |
| MIL-06 | Waits for foreground | P3 | FUN | milestone-4 seeded while Home is behind Detail | 1. Return to Home | Sheet appears only once Home is resumed. | | Not Tested | | |
| MIL-07 | Pro unlocked / restored sheet | P1 | FUN | Purchase (PAY-04) or Restore (PAY-09) | 1. Observe after paywall closes | Ball-buddy duck sheet; body "Unlimited matches, advanced stats, streak insurance and more card styles are yours for good."; restored variant after Restore. | | Not Tested | | |

### 5.16 Match detail & delete (F-19, F-20)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| DET-01 | Full detail | P1 | FUN | D1 | 1. Open D1 | Title "Match", number "#7"; Win; Doubles; date; time "6:00 PM – 7:30 PM" (locale format) with duration; "Where" Rizal Park Courts; PARTNER Ana; OPPONENTS Dave R., Maya; games table G1 11–7, G2 11–9 with "You" column; "Won 2 of 2"; "22–16 points"; paddle; notes. Buttons Edit, Share, Delete. | | Not Tested | | |
| DET-02 | Sparse detail | P2 | UI | D4 | 1. Open | Only format, result, date shown; no empty headings ("Games", "Where", players) with blank content. | | Not Tested | | |
| DET-03 | Chronological number | P2 | FUN | RD-7 | 1. Open D7, D5, D6 | #1, #2, #3 (same-day ties broken by creation time). | | Not Tested | | |
| DET-04 | Crossing midnight shown | P2 | FUN | D7 | 1. Open | "11:00 PM – 12:30 AM (next day)" style text; duration 1 h 30 min. | | Not Tested | | |
| DET-05 | Photo viewer | P2 | FUN | Match with 3 photos (Pro) | 1. Tap a photo 2. Swipe 3. Pinch / double-tap 4. Close | Opens full-screen with "1 of 3"; swipe pages; pinch zoom 1×–4×, double-tap 2.5×; paging disabled while zoomed; "Close photo" returns. | | Not Tested | | |
| DET-06 🔴 | Delete with confirmation | P0 | FUN | D4 | 1. Delete 2. Dialog "Delete this match?" / "The match and its photos will be permanently deleted. This can't be undone." 3. Tap "Delete" | Returns to Home; D4 gone; count 6; streak recomputed (4 → 3? D4 is W-3 → current becomes 3: W0…W-2). Delete cue. | | Not Tested | | |
| DET-07 | Cancel delete | P1 | FUN | D4 | 1. Delete 2. "Cancel" (also try tapping outside, Back) | Dialog closes; match intact. | | Not Tested | | |
| DET-08 | Delete removes photo files | P0 | DATA | D6 with photo | 1. Note file: `adb shell run-as com.maxeydev.picklelog ls files/photos` 2. Delete D6 3. List again | The photo file is gone. | | Not Tested | | |
| DET-09 | Delete keeps people | P1 | DATA | Delete D6 (only José match) | 1. New match, type `jo` | José no longer suggested (suggestions come from people used in matches) but still in the filter Opponent list (FLT-14). Record both. | | Not Tested | | |
| DET-10 | Delete dialog survives rotation | P3 | FUN | Dialog open | 1. Rotate | Dialog still shown; confirming deletes once. | | Not Tested | | |
| DET-11 | Back from detail | P2 | FUN | — | 1. Back | Home with the same scroll position, filters, search. | | Not Tested | | |

### 5.17 Photos (F-21)

Pre: Free unless stated. Use ENV-B (Photo Picker) and ENV-A (backport) for PHO-01/02.

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| PHO-01 🔴 | Add one gallery photo (free) | P0 | FUN | — | 1. New match 2. "Add photos" 3. Pick one image 4. Win, Save | System photo picker (images only) opens with **no** storage permission prompt; thumbnail shows "processing" then the photo with "Cover" label; saved; list thumbnail and detail show it. | | Not Tested | | |
| PHO-02 | Photo picker on API 26 | P1 | COMP | ENV-A | 1. Add photos | Backported picker / document UI opens; selection works; no permission prompt. | | Not Tested | | |
| PHO-03 | Free: picking 3 keeps 1 and explains | P1 | FUN | Free | 1. Add photos → pick 3 | Only the first is attached; dialog "One photo per match on the free version" / "Your first photo is attached. Pro removes the limit…" with "See Pro" and "Not now". | | Not Tested | | |
| PHO-04 | Free: second add when one exists | P1 | NEG | Free, 1 photo attached | 1. Add photos → pick 1 | Nothing added; same prompt. "See Pro" → paywall; Back returns to the editor with the draft intact. | | Not Tested | | |
| PHO-05 | Pro: up to 10 | P1 | BVA | Pro | 1. Pick 12 | 10 attached; dialog "Up to 10 photos per match" / "This match holds as many photos as it can. Remove one to add another." with "Got it" (no See Pro). | | Not Tested | | |
| PHO-06 | Reorder and cover | P2 | FUN | Pro, 3 photos | 1. "Move photo 3 earlier" ×2 2. Save | Photo 3 becomes first and labelled "Cover"; list thumbnail and card use it. Earlier/later buttons disabled at the ends. | | Not Tested | | |
| PHO-07 | Remove a photo (unsaved) | P2 | FUN | New match with 1 photo | 1. "Remove photo 1" 2. Save | Saved without a photo; no orphan file remains after 24 h sweep (DAT-08). | | Not Tested | | |
| PHO-08 🔴 | Remove a saved photo, then Close without saving | P0 | DATA | Saved match with 1 photo | 1. Edit 2. Remove photo 3. Close | Photo **still** on the match and file still exists (removal only applies on Save). | | Not Tested | | |
| PHO-09 | Remove a saved photo and Save | P1 | DATA | Same | 1. Edit 2. Remove 3. Save | Photo gone from detail; file deleted from `files/photos`. | | Not Tested | | |
| PHO-10 | Take photo — first time | P1 | INT | Device with camera, permission never asked | 1. "Take photo" | Rationale dialog "Use your camera?" (Continue / Not now) → system permission → camera app → photo attached. | | Not Tested | | |
| PHO-11 | Camera "Not now" / deny | P1 | NEG | — | 1. Not now 2. Retry → Continue → Deny | Nothing attached; editor intact; gallery still works. | | Not Tested | | |
| PHO-12 | Camera permanently denied | P2 | NEG | Denied twice ("Don't ask again") | 1. Take photo | "Camera access is off" dialog with "Open settings" → app settings page opens. | | Not Tested | | |
| PHO-13 | Cancel inside the camera app | P2 | NEG | — | 1. Take photo 2. Back out of camera | Nothing attached; no empty tile; temp capture file deleted (cache `camera/`). | | Not Tested | | |
| PHO-14 | No camera hardware | P2 | COMP | Emulator `-camera-back none -camera-front none` | 1. Open editor | "Take photo" not shown; "Add photos" works. Install still allowed (camera not required). | | Not Tested | | |
| PHO-15 | Large & rotated images are normalised | P1 | FUN | 12 MP JPEG, EXIF-rotated portrait | 1. Add 2. Save 3. `run-as … ls -l files/photos` | Displayed upright; stored JPEG long edge ≤ 2048 px, quality ~80 (expect a few hundred KB). | | Not Tested | | |
| PHO-16 | PNG / HEIC / GIF sources | P2 | COMP | Each format | 1. Add | Each is converted and shown (GIF: first frame). HEIC on API 30+. | | Not Tested | | |
| PHO-17 | Unreadable/corrupt source | P1 | NEG | 0-byte `.jpg`, or a cloud-only item while offline | 1. Add | Tile disappears; message "Couldn't read that photo — it may be damaged or no longer available. The match can still be saved without it." with OK; editor still saves. | | Not Tested | | |
| PHO-18 | Save while photo still processing | P1 | FUN | Slow device, pick 3 large images (Pro) | 1. Save immediately | Match saves at once; the still-processing photos attach in the background once ready; detail shows them within seconds; no duplicates. | | Not Tested | | |
| PHO-19 | Process death during import | P2 | EDGE | Don't keep activities ON, Pro | 1. Pick 5 images 2. Immediately Home 3. Return | Draft restored; pending photos resume or show processing; save works; no orphan duplicates. | | Not Tested | | |
| PHO-20 | Storage nearly full | P2 | NEG | Fill emulator storage to < 50 MB | 1. Add a 12 MP photo | Either attaches or shows the unreadable-photo message; no crash, no half-written file referenced. | | Not Tested | | |
| PHO-21 | Photos excluded from cloud backup | P1 | INT | See BAK-28 | — | — | | Not Tested | | |
| PHO-22 | TalkBack labels on photo tiles | P2 | A11Y | 3 photos | 1. TalkBack swipe | "Photo 1 of 3, used as the cover", "Move photo 1 later", "Remove photo 1", "Photo 2 of 3, still processing" while importing. | | Not Tested | | |

### 5.18 Share card (F-22, F-23)

Pre: RD-7. Open a match → "Share".

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| SHR-01 🔴 | Card renders | P0 | FUN | D1 | 1. Share | "Share match" screen; rendering placeholder then card preview within ~2 s (first time after cold start) and < 1 s after; card-ready cue. | | Not Tested | | |
| SHR-02 | Card content | P1 | FUN | D1, free | 1. Inspect preview | Shows "Matthew", "Doubles · <date>", With Ana, Time 1 h 30 min, Against Dave R. & Maya, Games 11–7 11–9, Rizal Park Courts, "Picklelog" wordmark. **No "Win"/"Loss" or W/L anywhere** (owner decision). | | Not Tested | | |
| SHR-03 🔴 | Share intent | P0 | INT | SHR-01 | 1. Tap "Share" 2. Pick Files/Drive/Messages | Chooser "Share your match"; target receives a JPEG (1080×1920 Story); file opens. Share-send cue. | | Not Tested | | |
| SHR-04 | Real social targets | P2 | INT | ENV-C with Instagram/Facebook/Messenger | 1. Share to each | Image arrives intact (Instagram Story accepts 9:16). | | Not Tested | | |
| SHR-05 | Square ratio | P1 | FUN | — | 1. Size → "Square" 2. Share | Preview re-renders 1:1; shared file 1080×1080. Card-swap cue. | | Not Tested | | |
| SHR-06 | Light theme | P2 | FUN | — | 1. Style → Light | Light card; text contrast OK. | | Not Tested | | |
| SHR-07 | Format persists across matches and restarts | P2 | FUN | Square + Light | 1. Share another match 2. Force-stop, relaunch, share | Square + Light pre-selected. | | Not Tested | | |
| SHR-08 | Hide details | P1 | FUN | D1 | 1. "Show on card": untick Game scores, Opponent names, Partner name, Location | Each disappears from the preview after re-render. Only details the match has are offered (D4 offers none). | | Not Tested | | |
| SHR-09 | Hidden details are per session | P3 | FUN | SHR-08 | 1. Back, Share again | All details shown again (only ratio/theme persist). Rotation during the session keeps them hidden. | | Not Tested | | |
| SHR-10 | Photo layout | P1 | FUN | D6 with photo | 1. Share 2. Toggle "Photo" off/on | Default PHOTO layout with the cover photo; off → no-photo layout; persisted override. D1 (no photo) shows no Photo switch. | | Not Tested | | |
| SHR-11 | Pro themes locked for free | P1 | FUN | Free | 1. Style → "Court · Pro" | Theme **not** applied; dialog "That card style is part of Pro" / "Pro adds more card styles and takes the Picklelog wordmark off your shared cards." See Pro / Not now. | | Not Tested | | |
| SHR-12 | Pro themes and wordmark | P1 | FUN | Pro | 1. Court 2. Sunset | Applied; **no "Picklelog" wordmark** on Pro cards. | | Not Tested | | |
| SHR-13 🔴 | Pro theme saved, then Pro lost | P1 | EDGE | Pro, Sunset saved; `Set-PicklePro -State off` | 1. Share | Falls back to Dark; wordmark back; no crash. | | Not Tested | | |
| SHR-14 | Render failure and retry | P1 | NEG | Force by disabling Android System WebView (Settings → Apps → Android System WebView → Disable, on emulator) | 1. Share | "Couldn't make the card this time. Your match is safe — try again." + "Try again"; Share disabled. Re-enable WebView → Try again → card renders. | | Not Tested | | |
| SHR-15 | Share file write failure | P2 | NEG | Fill storage | 1. Share | "Couldn't prepare the image to share. Check that your phone has free storage, then try again." | | Not Tested | | |
| SHR-16 | Match deleted while on share screen | P3 | EDGE | Two windows (split screen) or debug `Clear-PickleMatches` | 1. Trigger re-render (change ratio) | Screen closes back (isGone), no crash. | | Not Tested | | |
| SHR-17 | Long/unusual text on the card | P1 | BVA | Opponents 2×30-char names, 10 games, location 200 chars, name 30 chars, Arabic/CJK/emoji names | 1. Share in Story and Square | Text wraps/ellipsizes inside the card; nothing cut off mid-glyph; RTL names render correctly; emoji render. | | Not Tested | | |
| SHR-18 🔴 | Injection-shaped text renders as text | P0 | SEC | Opponent `<img src=x onerror=alert(1)>`, location `</div><script>alert(1)</script>` | 1. Share | Literal text appears on the card; no script runs, no layout break, no network request (see SEC-05). | | Not Tested | | |
| SHR-19 | Card accessibility description | P2 | A11Y | TalkBack | 1. Focus preview | Reads "Share card preview: Matthew. Doubles · … With Ana. …" | | Not Tested | | |
| SHR-20 | Offline | P1 | FUN | Airplane mode | 1. Render 2. Share to Files | Works fully offline. | | Not Tested | | |

### 5.19 Free-tier cap & upgrade prompts (F-24, F-26)

⚠ **The code caps free users at 50 matches** (`FreeTier.MATCH_LIMIT = 50`, warnings at 40 and 48). Your most recent stated decision elsewhere was a 100-match cap. Tests below follow the **code**. Resolve §13 R-00 before release.

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| CAP-01 | No banner below 40 | P2 | BVA | `cap-40` then delete 1 (39) | 1. Home | No cap banner. | | Not Tested | | |
| CAP-02 | Approaching banner at 40 | P1 | BVA | `cap-40` | 1. Home | Banner with smiling duck "10 free matches left" + "Dismiss". | | Not Tested | | |
| CAP-03 | Imminent banner at 48 | P1 | BVA | `cap-48` | 1. Home | Stronger banner "2 free matches left". At 49 → "1 free match left" (singular). | | Not Tested | | |
| CAP-04 🔴 | Save blocked at 50, data kept | P0 | FUN | `cap-50` | 1. FAB 2. Win, opponent `Leo` 3. Save | Paywall opens; **no** match 51 saved; Close paywall → editor still shows Win + Leo. Banner "You've used all your free matches. Everything you've logged stays yours." | | Not Tested | | |
| CAP-05 🔴 | Save completes after unlocking | P0 | E2E | CAP-04 on ENV-C (real purchase) or debug Pro | 1. From the blocked save → Unlock Pro (or `Set-PicklePro -State on` while paywall open) | Paywall closes; the pending match is **saved automatically exactly once**; count 51; Pro-unlocked sheet. | | Not Tested | | |
| CAP-06 | Dismiss banner is per level | P2 | FUN | `cap-40` | 1. Dismiss 2. Add matches to 48 | Approaching hidden after dismiss; Imminent appears at 48 (higher level). | | Not Tested | | |
| CAP-07 | Banner dismissal lifetime | P3 | FUN | CAP-06 dismissed | 1. Force-stop, relaunch | Record: dismissal is kept only in saved state, so the banner **returns on cold start**. Acceptable? | | Not Tested | | |
| CAP-08 | Settings Pro card shows usage | P2 | FUN | `cap-48` | 1. Open Settings | "Free · 48 of 50 matches"; "See Pro"-style action. | | Not Tested | | |
| CAP-09 | Deleting frees a slot | P1 | FUN | `cap-50` | 1. Delete one 2. Log a match | Saves normally (count 50). | | Not Tested | | |
| CAP-10 | Pro has no cap and no banners | P1 | FUN | Pro + 60 matches | 1. Home 2. Log a match | No banner; saves; Settings "Unlocked on this device". | | Not Tested | | |
| CAP-11 | Over-cap free state never hides data | P0 | DATA | Debug: `Add-PickleMatches -Count 55` (hook bypasses the cap) | 1. Browse list, stats, export | All 55 visible and exportable; only new saves blocked. | | Not Tested | | |
| CAP-12 | Cap applies to "Log another" and notification entry | P1 | FUN | `cap-50` | 1. Log another / reminder tap → Save | Paywall, no save. | | Not Tested | | |

### 5.20 Paywall & Google Play Billing (F-25)

Real billing tests (marked ENV-C) must run on a Play-track install with a license tester and Play test cards. **Never use a real card.**

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| PAY-01 | Paywall content | P1 | UI | Free, RD-7 | 1. Settings → Pro card | "Picklelog Pro", tagline "One-time unlock. No subscription.", "What you get" table: Matches 50 / Unlimited; Photos per match 1 / 10; Advanced stats ✗/✓; Streak insurance ✗/✓; More card styles ✗/✓; Export & backup ✓/✓. "Your free matches 7 / 50". Record summary (4–3, streak). | | Not Tested | | |
| PAY-02 | Price loads | P1 | INT | ENV-C | 1. Open | Button "Unlock Pro · <local price>" (e.g. ₱…); price-terms text visible. | | Not Tested | | |
| PAY-03 | Price unavailable offline | P1 | NEG | ENV-C airplane mode | 1. Open | "Couldn't reach Google Play. Check your connection and try again. Nothing you've bought is lost."; Buy disabled; "Try loading the price again" → works once online. | | Not Tested | | |
| PAY-04 🔴 | Successful purchase | P0 | INT | ENV-C, "Test card, always approves" | 1. Unlock Pro 2. Complete Play sheet | Paywall closes on its own as soon as Pro is recorded and the Pro-unlocked sheet appears; Settings "Unlocked on this device"; Pro stats/themes unlocked; cap gone. Purchase acknowledged (Play Console order not auto-refunded after 3 days). | | Not Tested | | |
| PAY-05 | User cancels the Play sheet | P1 | NEG | ENV-C | 1. Unlock Pro 2. Back out | No message, still Free, button re-enabled. | | Not Tested | | |
| PAY-06 | Declined card | P1 | NEG | "always declines" | 1. Purchase | Error message (e.g. "Something went wrong talking to Google Play…"); still Free; can retry. | | Not Tested | | |
| PAY-07 🔴 | Pending purchase | P0 | INT | "Slow test card, approves after a few minutes" | 1. Purchase 2. Wait 3. Return to app / relaunch | Immediately: "Your purchase is still being confirmed by Google Play. Pro unlocks as soon as it goes through." **Not** Pro yet. After approval: Pro granted automatically (on next launch or via purchase update listener) and acknowledged. | | Not Tested | | |
| PAY-08 | Pending then declined | P1 | INT | "Slow test card, declines after a few minutes" | 1. Purchase 2. Wait | Stays Free; no crash; can buy again. | | Not Tested | | |
| PAY-09 🔴 | Restore on reinstall | P0 | INT | ENV-C, already purchased | 1. Uninstall 2. Install from Play 3. Paywall → "Restore purchase" | "Pro restored. Welcome back."; Pro restored sheet. Also restored **without** tapping Restore at the next app start (startup sync). | | Not Tested | | |
| PAY-10 | Restore with nothing to restore | P1 | NEG | ENV-C, account without purchase | 1. Restore purchase | "No Pro purchase was found for the Google account on this device."; still Free. | | Not Tested | | |
| PAY-11 | Restore offline | P1 | NEG | Airplane | 1. Restore | Offline message; still Free; nothing revoked. | | Not Tested | | |
| PAY-12 🔴 | Pro is never revoked by failures | P0 | NEG | Pro (ENV-C) | 1. Airplane mode, relaunch ×3 2. Sign out of Play Store, relaunch 3. Clear Play Store cache, relaunch | Still Pro every time (only a confirmed refund of the same token may revoke; see PAY-20). | | Not Tested | | |
| PAY-13 | Already owned | P1 | INT | ENV-C purchased but app data cleared (`pm clear`) | 1. Relaunch 2. If still Free, Unlock Pro | Startup sync should restore Pro on launch by itself. If not yet, Unlock Pro → Play reports "already owned" → app syncs and unlocks. No second charge either way. | | Not Tested | | |
| PAY-14 | Double-tap Buy | P1 | NEG | ENV-C | 1. Tap Unlock Pro twice fast | One Play sheet; button disabled while working. | | Not Tested | | |
| PAY-15 | Billing unavailable device | P1 | COMP | ENV-A (no Play) | 1. Open paywall 2. Restore | "Purchases aren't available on this device right now. That's a Google Play setting, not something you did." (or Play-unavailable message); no crash; app otherwise fully usable. | | Not Tested | | |
| PAY-16 | Product not configured | P2 | NEG | Debug install not from Play (sideloaded) on a Play device | 1. Open paywall | "Pro isn't available from Google Play right now. Try again later." or similar product/billing message; no crash. | | Not Tested | | |
| PAY-17 | Paywall closes when Pro arrives from elsewhere | P2 | FUN | Paywall open; `Set-PicklePro -State on` | 1. Observe | Paywall pops automatically; unlocked sheet. | | Not Tested | | |
| PAY-18 | Back/close returns to origin | P2 | FUN | Opened from editor (CAP-04), share (SHR-11), stats (STA-03), support page | 1. Close (✕) / Back | Returns to the originating screen with its state intact. | | Not Tested | | |
| PAY-19 | Rotation during purchase | P2 | EDGE | ENV-C | 1. Start purchase 2. Rotate while Play sheet shows 3. Complete | Unlock still recorded once; paywall closes. | | Not Tested | | |
| PAY-20 | Refund behaviour (business rule) | P1 | INT | ENV-C purchased; refund via Play Console | 1. Refund 2. Relaunch app, Restore | **Code behaviour: Pro is kept** — no production path emits a refund signal, and "no purchase found" never revokes. Record result; confirm this is intended (§13 R-03). | | Not Tested | | |
| PAY-21 | Acknowledgement retry | P2 | INT | ENV-C, purchase while network drops right after approval | 1. Relaunch online | Startup sync re-checks and acknowledges (retries after 1 min, 5 min and 30 min within the same app session); order not auto-refunded after 3 days (check Play Console). If still unacknowledged, the next app launch retries. | | Not Tested | | |
| PAY-22 | `BillingFlowTest` on device | P1 | INT | ENV-C | `.\gradlew.bat :app:connectedDebugAndroidTest --tests "*BillingFlowTest*"` | Pass: no `DEVELOPER_ERROR`; a Pro device stays Pro. | | Not Tested | | |
| PAY-23 | Paywall in dark theme & large font | P2 | UI | Dark + Largest font | 1. Open | Table readable; buttons reachable; no clipping. | | Not Tested | | |
| PAY-24 | TalkBack reads plan rows | P2 | A11Y | TalkBack | 1. Swipe table | "Matches. Free: 50. Pro: Unlimited." etc. | | Not Tested | | |

### 5.21 Export, import, export reminders, Android backup (F-27 … F-30)

Pre: RD-7, Settings → "Export & backup". Fixtures: Appendix B.

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| BAK-01 | Backup screen content | P2 | UI | Never exported | 1. Open | Title "Export & backup"; intro "Your matches live on this device…"; "Last export: Never"; notes "Stored on this phone only", "Photos aren't included", "Android's own backup may not be running"; buttons "Export matches", "Import from a file". | | Not Tested | | |
| BAK-02 🔴 | Export produces a valid file | P0 | FUN | RD-7 | 1. Export matches 2. Dialog "Export your matches?" (photos note) → "Export" 3. Chooser "Save your export" → Files/Drive → save | File `picklelog-export-<today>.json` saved. Open it: `"format": "picklelog-export"`, `"schemaVersion": 1`, 6 `people`, 7 `matches`, games and photo **records** present, **no image data**. Export-ready cue. | | Not Tested | | |
| BAK-03 | Last export updates | P1 | FUN | BAK-02 | 1. Return to the screen | "Last export" shows today's date/time. Export-prompt banner (if any) cleared. | | Not Tested | | |
| BAK-04 | Cancel the export dialog | P2 | FUN | — | 1. Export matches 2. "Cancel" | No file, no chooser, Last export unchanged. | | Not Tested | | |
| BAK-05 | Cancel the share chooser | P1 | EDGE | — | 1. Export → Export 2. Back out of the chooser | **Code behaviour: "Last export" is still updated** (recorded when the chooser launches; Android gives no result). The export prompt is reset as if backed up. Record; see §13 R-06. | | Not Tested | | |
| BAK-06 | Export with zero matches | P3 | EDGE | Fresh install | 1. Export | Valid file with empty arrays; no crash. | | Not Tested | | |
| BAK-07 | Export with 1,000 matches | P2 | PERF | Pro + 1,000 seeded | 1. Export | "Preparing your export…" then chooser within ~5 s; file valid. | | Not Tested | | |
| BAK-08 🔴 | Round-trip restore after erase | P0 | E2E | BAK-02 file; then Erase all data (ERS-03) and onboard again | 1. Export & backup → Import from a file → pick the file | Summary "Added 7 matches."; list, stats, streak identical to RD-7; photos: "1 photo could not be restored because its file isn't on this device." (D6's file was erased). | | Not Tested | | |
| BAK-09 | Re-import the same file | P1 | DATA | After BAK-08 | 1. Import the same file again | "7 matches were already in Picklelog and were skipped."; count unchanged (merge by id, never overwrite). | | Not Tested | | |
| BAK-10 | Merge into existing different data | P1 | DATA | Device with 3 other matches incl. opponent `dave r.` | 1. Import RD-7 file | Added 7; total 10; `Dave R.` from file and existing `dave r.` resolve to **one** person (existing name wins). | | Not Tested | | |
| BAK-11 | Free cap on import | P1 | BVA | Free with 45 matches; fixture B-6 (10 matches) | 1. Import | "Added 5 matches." + "5 older matches were held back because the free version holds 50 matches. Upgrade to Pro, then import the same file again to add them." Newest 5 (by date) imported. Held-back matches create no people. | | Not Tested | | |
| BAK-12 | Pro imports everything | P1 | FUN | Pro + 45, B-6 | 1. Import | Added 10, nothing held back. | | Not Tested | | |
| BAK-13 | Not JSON | P1 | NEG | B-1 (a `.txt` or an image) | 1. Import | "Couldn't read that file. It may be incomplete, or it may not be a Picklelog export. Nothing was changed." Data unchanged. | | Not Tested | | |
| BAK-14 | JSON but not a Picklelog export | P1 | NEG | B-2 | 1. Import | "That file isn't a Picklelog export. Nothing was changed." | | Not Tested | | |
| BAK-15 | Newer schema | P1 | NEG | B-3 (`schemaVersion: 2`) | 1. Import | "That export was made by a newer version of Picklelog. Update the app and try again. Nothing was changed." | | Not Tested | | |
| BAK-16 🔴 | Corrupt content is all-or-nothing | P0 | DATA | B-4 (2 valid matches + 1 with `"result": "DRAW"`) | 1. Note count 2. Import | "That export file is damaged or has been edited, so it wasn't imported. Nothing was changed." Count unchanged — the 2 valid matches were **not** added. | | Not Tested | | |
| BAK-17 🔴 | Path traversal in photo path rejected | P0 | SEC | B-5 (`"relativePath": "photos/../databases/picklelog.db"`) and variants `photos/a/b.jpg`, `photos\\x.jpg`, `../x` | 1. Import each | Each rejected as damaged; nothing changed; app files intact. | | Not Tested | | |
| BAK-18 | Too large | P2 | BVA | 33 MB file (B-8) and exactly 32 MB minus 1 KB of valid padding | 1. Import | > 32 MB: "That file is too large to be a Picklelog export. Nothing was changed." ≤ 32 MB valid: imports. | | Not Tested | | |
| BAK-19 | Byte-order mark tolerated | P3 | EDGE | B-7 (UTF-8 BOM + valid JSON) | 1. Import | Imports normally. | | Not Tested | | |
| BAK-20 | Unknown extra fields ignored | P3 | EDGE | Valid file + `"futureField": 1` in a match | 1. Import | Imports normally. | | Not Tested | | |
| BAK-21 | Picker cancelled | P3 | NEG | — | 1. Import from a file 2. Back out | Nothing happens; no message. | | Not Tested | | |
| BAK-22 | Busy state blocks double actions | P2 | NEG | Large import running | 1. Tap Import / Export again | Ignored while "Importing…" / "Preparing your export…" shows. | | Not Tested | | |
| BAK-23 | Export prompt after 25 new matches | P1 | FUN | Fresh install, onboarding done; `Start-PickleScenario export-prompt` (26 matches) | 1. Open Home (allow ~1 min for WorkManager) | Banner "You've logged 25 or more matches since your last backup. Export a copy so they're safe." with "Export now" and dismiss. | | Not Tested | | |
| BAK-24 | "Export now" goes to backup | P2 | FUN | BAK-23 | 1. Export now | Backup screen opens; banner gone on return. | | Not Tested | | |
| BAK-25 | Dismiss persists until the next trigger | P2 | FUN | BAK-23 | 1. Dismiss ("Dismiss backup reminder") 2. Relaunch | Gone after relaunch; returns only after another +25 matches or 30 days. | | Not Tested | | |
| BAK-26 | Periodic prompt after 30 days | P2 | FUN | Matches exist, never exported; set device date +31 days | 1. Launch, wait for daily worker (or relaunch) | Banner "It's been a while since your last backup…". | | Not Tested | | |
| BAK-27 | Import resets the +25 counter | P3 | FUN | Import 30 matches | 1. Observe | No "25 or more" prompt caused by the import itself. | | Not Tested | | |
| BAK-28 | Backup rules packaged | P1 | INT | Release APK | `.\gradlew.bat checkBackupRules` and inspect the merged manifest | `allowBackup=true`; cloud backup excludes `photos/`; device transfer includes database and files. | | Not Tested | | |
| BAK-29 | Cloud restore on a new phone | P1 | INT | ENV-C → ENV-D, procedure in `documentation/backup-restore-verification.md` | Follow it | Matches/people/scores identical; photos absent but photo-bearing matches open without crashing; no migration error. | | Not Tested | | |
| BAK-30 | Device-to-device transfer | P2 | INT | Same as BAK-29 via "copy your data" | Follow it | Photos present too. | | Not Tested | | |
| BAK-31 | Restored entitlement | P1 | SEC | BAK-29 with Pro on ENV-C | 1. Check Pro on ENV-D **before** any network | Record: the profile file (with `isPro`) is **not** excluded from backup, so Pro may be restored from the backup itself rather than from Play. Acceptable when the account is the same; see §13 R-08. | | Not Tested | | |
| BAK-32 | Import values the UI could never create | P2 | EDGE | Fixture B-9: a match dated **tomorrow**, scores 5000–0, game numbers 3 and 7, location `"   "` (spaces), notes 20,000 chars | 1. Import 2. View list, detail, stats, card | **Code behaviour: accepted** (import validates structure, not UI rules). Record how each screen copes: future-dated match vs streak/"Today", 4-digit scores in rows, gap in game numbers, whitespace-only location (detail/card/filter list). See §13 R-14. | | Not Tested | | |
| BAK-33 | Import names that collide after normalisation | P2 | NEG | Fixture with two people `Ana` and `ana` (different ids) | 1. Import | Rejected as damaged ("Duplicate person name"); nothing changed. | | Not Tested | | |
| BAK-34 | Imported data appears everywhere without restart | P1 | FUN | Empty device, RD-7 export file | 1. Import 2. Back to Home 3. Open stats, filters, autocomplete | List, header, streak, stats, filter options and name suggestions all reflect the 7 imported matches immediately. | | Not Tested | | |

### 5.22 Settings drawer & info screens (F-31, F-34, F-35, F-36)

Pre: Home → "Settings" icon.

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| SET-01 | Drawer opens/closes | P1 | UI | — | 1. Tap Settings 2. Close via ✕ ("Close settings"), scrim tap, system Back | Drawer slides in titled "Settings"; each close method works; drawer cues. | | Not Tested | | |
| SET-02 | Rows present in order | P2 | UI | Free | 1. Scroll drawer | Pro card ("Picklelog Pro", "Free · 7 of 50 matches"); Name; Dark mode; Sound effects; Streak reminder; Export & backup; Clear cache (size); Privacy policy; Rate us; Support the creator; About; version "v0.1.0"; then "Erase all data" last, visually separated. | | Not Tested | | |
| SET-03 | Pro card when Pro | P2 | UI | Pro | 1. Open | "Unlocked on this device"; card not tappable to paywall. | | Not Tested | | |
| SET-04 | Change name | P1 | FUN | Name Matthew | 1. Name → dialog "Your name" 2. `Matt` 3. "Save name" | Header and share card show "Matt". | | Not Tested | | |
| SET-05 | Name validation | P1 | NEG | — | 1. Clear the field 2. Try `   ` 3. Type same name `Matthew` 4. 31 chars | 1–2: Save name disabled + "A name is required."; 3: Save disabled (no change); 4: capped at 30. Cancel restores the saved name. | | Not Tested | | |
| SET-06 | Dark mode follows system initially | P1 | FUN | Fresh install, system dark ON | 1. Open app | App dark; switch shows ON (mirrors system). Flip system theme → app follows (switch untouched). | | Not Tested | | |
| SET-07 | Dark mode override persists | P1 | FUN | — | 1. Toggle Dark mode OFF while system is dark 2. Force-stop, relaunch | App light; status/nav bars adapt; persists. System theme changes no longer affect the app (by design, §13 R-07). | | Not Tested | | |
| SET-08 | No wrong-theme flash on cold start | P3 | UI | Override = dark, system light | 1. Cold start ×5 | No white flash of the light theme before dark appears. | | Not Tested | | |
| SET-09 | Sound effects off silences everything | P2 | FUN | — | 1. Toggle off 2. Log, share, filter, erase | No app sounds anywhere. Toggling on plays a preview "pock". Persists after restart. | | Not Tested | | |
| SET-10 | Sounds respect device volume/silent mode | P3 | COMP | Ringer silent / media volume 0 | 1. Use app | No audible cue when media volume is 0. | | Not Tested | | |
| SET-11 | Clear cache | P2 | FUN | After sharing cards and exporting | 1. Note size 2. Clear cache | Size drops to ~0; sweep cue; matches, photos, settings intact; share and export still work after. | | Not Tested | | |
| SET-12 | Export & backup row | P3 | FUN | — | 1. Tap | Backup screen. | | Not Tested | | |
| SET-13 | Rate us | P2 | INT | ENV-C | 1. Tap | Play Store listing for `com.maxeydev.picklelog` opens (or web listing if no Play Store). On ENV-A with neither: nothing happens, no crash. | | Not Tested | | |
| SET-14 | Privacy policy | P1 | FUN | — | 1. Tap | "Privacy policy" screen: summary "Your matches stay on your phone.", 3 points, "Effective 3 October 2026", sections: What Picklelog stores, Permissions, Google services, Exports and sharing, Deleting your data, Children, Changes and contact. Duck art. | | Not Tested | | |
| SET-15 | About | P2 | FUN | — | 1. Tap About | "About", "Version 0.1.0", privacy bullets, "Open-source software" list; tapping the duck ("Pet the duck") cycles faces. | | Not Tested | | |
| SET-16 | About version matches build | P1 | REG | Release candidate | 1. Compare About and drawer version with `versionName` | Identical. | | Not Tested | | |
| SET-17 | Licences complete | P2 | FUN | — | 1. Compare listed libraries with `gradle/libs.versions.toml` runtime deps | Every shipped library (Compose, AndroidX, Room, DataStore, WorkManager, Coil, kotlinx, Play Billing, fonts) is credited with its licence. | | Not Tested | | |
| SET-18 | Support page — Free | P2 | FUN | Free | 1. Support the creator | Hero "Thanks for using Picklelog" + body; rows "Unlock Picklelog Pro" (→ paywall), "Rate Picklelog" (→ Play), "Share with your friends!" (→ share sheet with "I log my pickleball matches with Picklelog: https://play.google.com/store/apps/details?id=com.maxeydev.picklelog"), "Donate!". | | Not Tested | | |
| SET-19 | Support page — Pro | P3 | FUN | Pro | 1. Open | Pro row replaced by "You've unlocked Picklelog Pro" / "Thank you…". | | Not Tested | | |
| SET-20 | Donate row | P2 | FUN | — | 1. Tap "Donate!" | **Code behaviour: nothing happens** (row has no action). Decide: hide it, label it "coming soon", or wire it up before release. See §13 R-05. | | Not Tested | | |
| SET-21 | Settings survive rotation | P3 | FUN | Drawer open, name dialog open with draft | 1. Rotate | Drawer and dialog stay; draft kept. | | Not Tested | | |
| SET-22 | Back from sub-screens returns to Home | P3 | FUN | — | 1. Open Privacy/About/Support/Backup 2. Back | Home (drawer closed). Record whether returning to the open drawer would be preferable. | | Not Tested | | |

### 5.23 Streak reminders & notifications (F-32)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| RMD-01 | Off by default, no permission asked | P1 | FUN | Fresh install API 37 | 1. Onboard 2. Open Settings | Streak reminder OFF; no notification permission prompt has appeared anywhere. | | Not Tested | | |
| RMD-02 | Enable on API 33+ | P1 | INT | ENV-B | 1. Toggle Streak reminder | Dialog "Turn on streak reminders?" / "Picklelog sends a notification once a week, only when your streak is at risk." → "Turn on" → system permission → Allow → switch ON, supporting text "Weekly, only if your streak is at risk", "Reminder time 6:00 PM" row appears. | | Not Tested | | |
| RMD-03 | Permission denied | P1 | NEG | ENV-B | 1. Turn on → Don't allow | Switch stays OFF. Second attempt after "don't ask again" → "Notifications are off" dialog with "Open settings". | | Not Tested | | |
| RMD-04 | Enable on API 26–32 | P1 | COMP | ENV-A | 1. Toggle | Turns on directly (no runtime permission). If app notifications are blocked in system settings → settings dialog instead. | | Not Tested | | |
| RMD-05 | Notifications blocked after enabling | P2 | NEG | Enabled; then block in system settings | 1. Reopen drawer | Inline warning "Notifications are turned off for Picklelog in system settings, so no reminder can arrive." + "Open notification settings". | | Not Tested | | |
| RMD-06 | Notification content | P1 | UI | `Send-PickleReminder` (plain) and `-Kind skip` | 1. Inspect | Title "Keep your streak going"; body "You haven't played this week yet. One match keeps your streak alive." / skip variant "…A streak skip is ready if you can't make it."; cheering-duck large icon; custom sound; channel "Streak reminders". Content hidden on lock screen (private). | | Not Tested | | |
| RMD-07 🔴 | Tap opens the logger | P1 | E2E | Onboarded, app killed | 1. Tap notification | App opens straight into "New match"; Back → Home. Notification dismissed. | | Not Tested | | |
| RMD-08 | Tap when app already open | P2 | FUN | App on Detail screen | 1. Tap notification | New match opens once (no duplicate editors). | | Not Tested | | |
| RMD-09 | Fires Friday at the set time when at risk | P1 | INT | `Start-PickleScenario reminder` (W-1…W-3 played, nothing this week), enabled; device date = a Friday, reminder time = now + 3 min | 1. Lock phone, wait | Notification arrives within ~15 min of the set time. | | Not Tested | | |
| RMD-10 | Not sent when not at risk | P1 | FUN | RMD-09 setup + a match this week | 1. Wait past time | No notification. | | Not Tested | | |
| RMD-11 | Not sent with no active streak | P2 | FUN | No match last week or this week | 1. Wait | No notification. | | Not Tested | | |
| RMD-12 | At most once per week | P1 | FUN | RMD-09 fired | 1. Change reminder time to +3 min again the same Friday | No second notification that week. | | Not Tested | | |
| RMD-13 | Skip-available wording for Pro | P2 | FUN | Pro since ≥ 1 month, at risk | 1. Wait for fire | Skip variant body. | | Not Tested | | |
| RMD-14 | Change time | P2 | FUN | Enabled | 1. "Reminder time 6:00 PM. Change" → 08:30 | Row shows 8:30 AM; next fire Friday 08:30. | | Not Tested | | |
| RMD-15 | Survives reboot and app update | P1 | INT | ENV-C enabled | 1. Reboot 2. Wait for next Friday window (or set date) | Reminder still fires (WorkManager persisted + re-armed at app start). | | Not Tested | | |
| RMD-16 | Disable cancels | P1 | FUN | Enabled | 1. Toggle off | Toggle-off cue; no notification on the next Friday. | | Not Tested | | |
| RMD-17 | Window closes Sunday at the set time | P3 | BVA | At risk, enabled, device date Sunday after set time | 1. Wait | No notification (outside Fri→Sun window). | | Not Tested | | |

### 5.24 Erase all data (F-33)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| ERS-01 | First confirmation | P1 | UI | RD-7 | 1. Settings → Erase all data | Dialog "Erase all data?" with body "…Your Pro purchase is kept." and buttons "Export my matches first", "Erase everything", Cancel. | | Not Tested | | |
| ERS-02 | Export first | P2 | FUN | ERS-01 | 1. "Export my matches first" | Dialog closes, Backup screen opens. Nothing erased. | | Not Tested | | |
| ERS-03 🔴 | Erase permanently | P0 | DATA | RD-7 + Pro + Dark mode ON + reminder ON + sort "Wins first" | 1. Erase everything 2. Second dialog "Erase permanently?" "Your 7 matches, their photos and your name will be gone for good…" 3. "Erase permanently" | Onboarding page 1 appears; erase cue. After onboarding: 0 matches; no people in suggestions/filters; `files/photos` empty; export cache empty; sort back to Newest first; card format default; reminder OFF and no pending work; **Pro still active**; **Dark mode still as set** (kept by design). | | Not Tested | | |
| ERS-04 | Cancel at each step | P1 | FUN | — | 1. Cancel on dialog 1 2. Proceed, cancel on dialog 2 | Nothing erased either time. | | Not Tested | | |
| ERS-05 | Erase with zero matches | P3 | EDGE | No matches | 1. Erase | Dialog 2 says "Your name will be gone for good. There's no undo after this."; works. | | Not Tested | | |
| ERS-06 | Singular wording | P3 | UI | 1 match | 1. Dialog 2 | "Your 1 match, its photos and your name…". | | Not Tested | | |
| ERS-07 | Back/outside tap while erasing | P2 | NEG | Large data set (1,000 + photos) | 1. Confirm 2. Immediately press Back/tap outside | Dialog not dismissable while erasing; completes once. | | Not Tested | | |
| ERS-08 | Erase failure message | P2 | NEG | Hard to force; e.g. make an export cache dir unwritable via `run-as` | 1. Erase | "Couldn't erase everything. Some data may remain. Try again." Retrying works. | | Not Tested | | |
| ERS-09 | Erased data doesn't come back | P0 | DATA | ERS-03 | 1. Force-stop, relaunch 2. Wait for orphan sweep | Still empty. (Cloud backup may still hold an older copy — out of app control, stated in dialog.) | | Not Tested | | |
| ERS-10 | Post-erase Pro check | P0 | SEC | ERS-03 on ENV-C | 1. Restart offline | Still Pro (entitlement never cleared by erase). | | Not Tested | | |

### 5.25 Navigation, state restoration & general UI (F-01, F-37, F-38)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| NAV-01 🔴 | Cold start lands correctly | P0 | FUN | Onboarded | 1. Force-stop 2. Launch | Home with data in < 2 s (PRF-01); no crash in `adb logcat -b crash`. | | Not Tested | | |
| NAV-02 | Back stack sanity | P1 | FUN | — | 1. Home → Detail → Share → Back → Back 2. Home → Stats → Paywall → Close → Back 3. Home → Detail → Edit → Close → Back | Each Back returns exactly one level; Back on Home exits. No screen appears twice. | | Not Tested | | |
| NAV-03 | Predictive back (API 34+) | P3 | UI | ENV-B, gesture nav | 1. Slow back-swipe on Detail, Stats, Editor | Preview animation; cancelling the gesture keeps the screen. | | Not Tested | | |
| NAV-04 | Rapid double taps don't open screens twice | P2 | NEG | — | 1. Double-tap a row 2. Double-tap Share 3. Double-tap FAB | One screen each; one Back returns Home. | | Not Tested | | |
| NAV-05 | Rotation everywhere | P1 | COMP | — | 1. Rotate on every screen and dialog | No crash; no lost input; no duplicated dialogs. | | Not Tested | | |
| NAV-06 🔴 | Don't-keep-activities sweep | P0 | EDGE | Developer option ON | 1. On each screen (editor with data, filter sheet, delete dialog, share with hidden details, paywall, backup with summary) press Home and return | State restored; no crash; editor draft intact; no duplicate save. | | Not Tested | | |
| NAV-07 | Process killed in background | P1 | EDGE | Editor with data | 1. Home 2. `adb shell am kill com.maxeydev.picklelog` 3. Reopen from Recents | Editor with draft restored. | | Not Tested | | |
| NAV-08 | Multi-window / split screen | P3 | COMP | API 37 | 1. Split screen with Chrome 2. Use editor and list | Layout adapts; no crash on resize. | | Not Tested | | |
| NAV-09 | Language/locale change while running | P3 | EDGE | — | 1. Change device language to Filipino, return | App recreates; English UI (only English provided); dates use the new locale format; no crash. | | Not Tested | | |
| NAV-10 | Font/Display size change while running | P3 | EDGE | — | 1. Change font size, return | Recreates without losing state. | | Not Tested | | |
| NAV-11 | Time-zone/date change while running | P2 | EDGE | — | 1. Change time zone 2. Return | "Today", streak recompute; no crash. | | Not Tested | | |
| NAV-12 | App update keeps data | P0 | DATA | Install build N with RD-7, then `adb install -r` build N+1 | 1. Launch | All data intact; no migration error (DB version 1 → same). Pre-migration backup logic only triggers on schema bumps. | | Not Tested | | |
| UI-01 | Mascot appears in the right places | P3 | UI | — | 1. Visit empty list, first match, cap banner, milestones, share loading, backup working/error, privacy, about, support | Duck variants show without jank; never cover text or buttons. | | Not Tested | | |
| UI-02 | Mascot animation doesn't block input | P2 | UI | — | 1. Tap buttons while ducks animate | Taps register immediately. | | Not Tested | | |
| UI-03 | Consistent visual language | P3 | UI | Light + dark | 1. Compare headers, buttons, chips, dialogs across screens | Same spacing scale, corner radii, fonts, green palette; no default-Material leftovers. Use `Picklelog Design Audit.md` as reference. | | Not Tested | | |
| UI-04 | Destructive actions confirmed | P1 | UI | — | 1. Delete match 2. Erase all 3. Remove photo | Delete and Erase confirm; removing a photo in the editor doesn't (reversible by Close). | | Not Tested | | |
| UI-05 | Error copy is human and actionable | P2 | UI | During §7.2 tests | — | Every error says what happened and what to do; none shows a stack trace, code, or "null". | | Not Tested | | |
| UI-06 | Loading states | P3 | UI | Slow device | 1. Cold start, open stats with 1,000 matches, share | No blank screens > 1 s without an indicator (bobbing duck/placeholder). | | Not Tested | | |

---

## 6. End-to-End User Journeys

Run on **ENV-C** (real phone) unless noted. Each journey lists state, actions, the response after each action, data changes, failure points and recovery. Record the overall result in the tracker as `E2E-0n`.

### E2E-01 · First-time player logs their first week 🔴 (P0)

| | |
|---|---|
| **Start state** | App freshly installed from Play internal track; never opened; Free |
| **Final state** | Name saved; 2 matches; 1-week streak; Home shows record 1W – 1L, 50% |

| # | Action | Expected response | Data change |
|---|---|---|---|
| 1 | Launch | Onboarding page 1, no permission prompts | — |
| 2 | Skip → type `Matthew` → Get started | Home empty state with duck; header "Matthew", "No streak" | `displayName=Matthew`, onboarding complete |
| 3 | "Log your first match" → Doubles default → Win → Save | Sheet closes; "Your log has hatched" sheet | Match 1 (WIN, today, start time now) |
| 4 | "Log another" → Loss → Save | Editor pre-filled date; saved; "Match saved / Log another" | Match 2 |
| 5 | Kill app, relaunch | Home with 2 matches, "1W – 1L", "50%", "1 wk" | — |

**Failure points:** onboarding save fails (`"Couldn't save your name. Try again."`), sheet doesn't close, first-match sheet missing. **Recovery:** retry Get started; if the editor is stuck, Close and re-open — no partial match must exist.

### E2E-02 · A full doubles match with photos, then share it (P0)

| | |
|---|---|
| **Start** | Pro (or Free for 1 photo), RD-7 loaded, camera available |
| **Final** | Match with 2 photos (Pro), shared as a Story image to Messages/Drive |

| # | Action | Expected | Data |
|---|---|---|---|
| 1 | FAB → Doubles → partner via suggestion `an` → Ana; opponents `da`→Dave R., new `Kim` | Suggestions appear; Kim created on save | — |
| 2 | Game ×2: 11–9, 8–11; Game 3: 11–6 → Win | No advisory (2–1 games) | — |
| 3 | Location chip "Rizal Park Courts"; End time +1 h | Duration "1 h 0 min"-style | — |
| 4 | Take photo (grant camera) + Add photos (1 from gallery); move photo 2 earlier | Cover = gallery photo | Temp capture deleted after import |
| 5 | Save | List row with thumbnail; count +1 | Match + 2 photo rows + 2 files |
| 6 | Open → Share → Square → Court (Pro) → untick Location → Share → Drive | Card has no location; no wordmark; file in Drive is 1080×1080 | Card JPEG in cache |
| 7 | Back ×2 | Home | — |

**Failure points:** photo import fails (message, match still saves), WebView render fails ("Try again"), no share targets. **Recovery:** retry render; Clear cache then retry.

### E2E-03 · Hitting the free cap and upgrading mid-save 🔴 (P0)

| | |
|---|---|
| **Start** | Free, exactly 50 matches (`cap-50`), Play tester with "always approves" card |
| **Final** | Pro; 51 matches; the blocked match saved exactly once |

| # | Action | Expected | Data |
|---|---|---|---|
| 1 | Observe Home | "You've used all your free matches…" banner | — |
| 2 | FAB → Win → opponent `Leo` → Save | Paywall opens, "Your free matches 50 / 50" | Nothing written |
| 3 | Close paywall | Back in editor; Win + Leo intact | — |
| 4 | Save again → Unlock Pro · price → approve | Paywall auto-closes; editor auto-saves; Home; Pro-unlocked sheet | Entitlement isPro=true, token stored & acknowledged; match 51 |
| 5 | Relaunch offline | Still Pro, 51 matches, no banner | — |

**Failure points:** match saved twice; draft lost when paywall opens; Pro lost offline. **Recovery:** if purchase pending, app must show pending message and unlock later (PAY-07).

### E2E-04 · Streak journey across weeks with insurance (P1)

Use emulator date changes (Auto date & time OFF). Week numbers are relative to the starting week S.

| # | Action | Expected | Data |
|---|---|---|---|
| 1 | Week S Wed: log a match | 1 wk | — |
| 2 | Week S+1 Fri 18:05, reminder ON at 18:00, no match this week | Notification "Keep your streak going" | lastNotifiedWeek = S+1 |
| 3 | Tap notification → Win → Save | Logger opens; streak 2 wk | — |
| 4 | Weeks S+2, S+3: one match each | 4 wk + milestone sheet "A month without missing a week." | — |
| 5 | Skip week S+4 entirely; in S+5 open app (Free) | "No streak"; missed-skip upsell "…ended your 4-week streak…" | — |
| 6 | Buy Pro (in S+5) | Week S+4 is covered (Pro started before the Sunday of S+5): streak 4 again, skip-used notice | skips held decreases |

**Failure points:** wrong week boundaries; notification on a week already played; insurance applied before eligibility. **Recovery:** compare every step with `Get-PickleStatus`.

### E2E-05 · Backup, wipe, restore on the same phone 🔴 (P0)

| # | Action | Expected | Data |
|---|---|---|---|
| 1 | RD-7 loaded, Pro; Export → save to Drive | Last export = now | JSON file in Drive |
| 2 | Erase all data (both confirmations) | Onboarding; Pro kept | DB empty, photos deleted |
| 3 | Onboard; Import the Drive file | "Added 7 matches." + "1 photo could not be restored…" | 7 matches, 6 people |
| 4 | Compare | Stats, streak, sort orders identical to §2.5 | — |

**Failure points:** partial import; people duplicated; Pro lost on erase. **Recovery:** import is atomic — retry; erase failure message allows retry.

### E2E-06 · New phone via Google backup (P1)

ENV-C → ENV-D per `documentation/backup-restore-verification.md`. Expected: matches restored, photos absent, Pro present (from Play or from the backed-up profile — record which), no onboarding forced (matches exist), reminders re-armed only if the reminder preference was restored and the user opens the app.

### E2E-07 · Review history with filters, search, sort, stats (P1)

| # | Action | Expected |
|---|---|---|
| 1 | RD-7; Filters → Doubles + Wins | D1, D3, D7; header 3W – 0L, 100%; streak 4 wk |
| 2 | Search `ana` | D1, D3 (D7 has no players) |
| 3 | Sort Longest match first | D1, D3 (D1 has duration, D3 none) |
| 4 | Tap header → Your stats | Filter indicator; record 3–0 |
| 5 | Back; Clear all; clear search | 7 matches, 4W – 3L |

### E2E-08 · Correct a mistake (edit, then delete) (P1)

1. Log a Win vs `Ben` today by mistake (should be Loss vs `Benny`). → 2. Open → Edit → Loss, rename opponent `Benny` → Save → detail updates, count unchanged, a new person `Benny` exists. → 3. Realise it was a duplicate → Delete → confirm → count back, streak/record recomputed, `Benny` remains selectable in filters (people persist).

### E2E-09 · Interrupted flows (P1)

| Interruption | Where | Expected |
|---|---|---|
| Incoming call / Home button | Editor with data | Draft intact on return |
| Process death (`am kill`) | Editor with 3 importing photos | Draft restored; photos resume or are reported; one match on save |
| Battery saver + Doze | Reminder week | Notification may be delayed but still arrives within the window |
| Airplane mode toggled | During purchase | Pending/offline message; no false unlock; no revocation |
| Screen lock | Share preview rendering | Card ready on unlock or "Try again" |
| Abandoned | Open paywall from cap block, press Home, never return | Next launch: no phantom match saved |

### Journey results

| ID | Journey | Pri | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|
| E2E-01 🔴 | First-time player logs their first week | P0 | | Not Tested | | |
| E2E-02 | Full doubles match with photos, then share | P0 | | Not Tested | | |
| E2E-03 🔴 | Free cap → upgrade mid-save | P0 | | Not Tested | | |
| E2E-04 | Streak across weeks with insurance | P1 | | Not Tested | | |
| E2E-05 🔴 | Backup, wipe, restore on the same phone | P0 | | Not Tested | | |
| E2E-06 | New phone via Google backup | P1 | | Not Tested | | |
| E2E-07 | Review history with filters, search, sort, stats | P1 | | Not Tested | | |
| E2E-08 | Correct a mistake (edit, then delete) | P1 | | Not Tested | | |
| E2E-09 | Interrupted flows | P1 | | Not Tested | | |

---

## 7. Negative and Edge-Case Testing

### 7.1 Form validation matrix

Every input in the app. ✔ = covered by the listed test; the right-hand columns say what the code does.

| Form / field | Required | Min / Max | Allowed | Trim | Duplicate rule | Tests |
|---|---|---|---|---|---|---|
| Onboarding name | **Yes** | 1 / 30 chars after trim | Any Unicode | Yes | — | ONB-07…11 |
| Settings name | **Yes** | 1 / 30 | Any | Yes | Save disabled if unchanged | SET-05 |
| Match result | **Yes** | — | Win/Loss | — | — | LOG-04 |
| Match format | Yes (defaulted) | — | Singles/Doubles | — | — | LOG-02,06,07 |
| Date | Yes (defaulted) | ≤ today | Date picker only | — | — | LOG-19,20 |
| Start / end time | No | 00:00–23:59 | Time picker | — | end < start ⇒ next day | LOG-21…23 |
| Opponent 1/2, Partner | No | **no max length in code** | Any | Yes (normalised NFC, collapse spaces, lowercase for identity) | Same person can't fill two slots | AUT-06, 07, 12, NEG-01 |
| Game scores | No (but both or neither per game) | 0 / 999 (3 digits) | Digits only | — | Duplicate game numbers impossible in UI | LOG-11…18 |
| Location / Paddle | No | **no max length** | Any | Yes; blank ⇒ null | — | LOG-24, NEG-01 |
| Notes | No | **no max length** | Any, multi-line | Yes (outer) | — | LOG-25, NEG-01 |
| Photos | No | Free 1 / Pro 10 per match | Images via picker/camera | — | — | PHO-03…05 |
| Reminder time | Yes when on (default 18:00) | — | Time picker | — | — | RMD-14 |
| Filter custom range | No | from ≤ to | Date range picker | — | — | FLT-07,08, NEG-08 |
| Import file | — | ≤ 32 MB | Any MIME (`*/*`) picked; content validated | BOM tolerated | Merge by match id | BAK-13…22 |

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| NEG-01 | Very long free text | P1 | BVA | 500-char opponent name, location, paddle, notes; 5,000-char notes | 1. Save 2. View list, detail, stats, filter sheet, share card | Saves. Every screen stays usable: rows ellipsize, detail wraps, filter dropdown doesn't overflow, card wraps/ellipsizes. Record any layout break (no max length exists — §13 R-10). | | Not Tested | | |
| NEG-02 | International names everywhere | P1 | FUN | §2.6 international set | 1. Use as opponent, partner, location, name 2. Search, sort, card | Correct rendering incl. RTL (Arabic) and CJK; sorting stable; card shows glyphs (font fallback). | | Not Tested | | |
| NEG-03 | Emoji-only name | P3 | EDGE | `🏓` | 1. Onboarding name and opponent | Accepted (non-blank). Displays. | | Not Tested | | |
| NEG-04 | Zero-width / control characters | P3 | EDGE | `​` only; `Dave​` | 1. Opponent | Record whether a zero-width-only name is accepted as a person (it is not whitespace for `trim()` → likely creates an invisible person). | | Not Tested | | |
| NEG-05 | Paste huge text | P2 | PERF | 100,000-char paste into Notes | 1. Paste 2. Save | No ANR; saves or is bounded; list remains smooth. | | Not Tested | | |
| NEG-06 | Score field paste non-digits | P3 | NEG | `eleven`, `١١` (Arabic-Indic digits) | 1. Paste | Non-ASCII digits stripped (only '0'–'9' kept). | | Not Tested | | |
| NEG-07 | Date picker typing mode | P3 | NEG | Switch picker to text input, type a future date / invalid `13/45/2026` | 1. Confirm | Future date rejected; invalid date not accepted. | | Not Tested | | |
| NEG-08 | Custom filter range in the future / reversed | P3 | NEG | — | 1. Pick future dates 2. Try end before start | Future range simply returns 0 matches; picker prevents reversed range (no crash from `FilterState` `require`). | | Not Tested | | |
| NEG-09 | Device clock set backwards | P2 | EDGE | Log a match today; set clock −2 days | 1. Open list, editor | Match dated "in the future" relative to the clock still displays; date picker max = new today; no crash. | | Not Tested | | |
| NEG-10 | Repeated Save during slow DB | P1 | NEG | 1,000 matches, low-end emulator | 1. Spam Save | One match. | | Not Tested | | |

### 7.2 Error handling and resilience

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-01 🔴 | Entire app offline | P0 | FUN | Airplane mode | 1. Log, edit, delete, search, filter, sort, stats, share card, export, import, erase | All work. Only purchase/restore/price show network messages. | | Not Tested | | |
| ERR-02 | Storage full | P1 | NEG | Emulator storage filled | 1. Save match 2. Add photo 3. Export 4. Share | No crash. Export: "Couldn't create the export file…". Share: "Couldn't prepare the image…". Save: record behaviour (Room may throw `SQLiteFullException` — **no handler found in the save path**; see §13 R-11). | | Not Tested | | |
| ERR-03 | WebView missing/updating | P1 | NEG | Disable/Update Android System WebView mid-session | 1. Share | Render failure message + retry (SHR-14); no crash. | | Not Tested | | |
| ERR-04 | Renderer process killed | P2 | NEG | `adb shell am kill` the WebView sandbox process during render (or dev option "Kill renderer") | 1. Share | Failure state + retry; app process survives. | | Not Tested | | |
| ERR-05 | Play Billing disconnected mid-flow | P1 | INT | Force-stop Play Store during purchase | 1. Purchase | Error or pending message; retry works; no false unlock. | | Not Tested | | |
| ERR-06 | Corrupt DataStore file | P2 | EDGE | `run-as` truncate `files/datastore/*.preferences_pb` | 1. Launch | App launches with defaults (or a clear error); matches still present. Record behaviour. | | Not Tested | | |
| ERR-07 | Photo file missing on disk | P1 | EDGE | `run-as … rm files/photos/<one>.jpg` for D6 | 1. List 2. Detail 3. Share | Placeholder/blank thumbnail, no crash; share renders without photo (`loadPhotoDataUri` returns null). | | Not Tested | | |
| ERR-08 | Orphan photo sweep | P2 | DATA | Copy a stray `x.jpg` into `files/photos` older than 24 h (`touch -d`) | 1. Relaunch | Stray file deleted; referenced photos untouched. Files younger than 24 h kept. | | Not Tested | | |
| ERR-09 | Low memory | P2 | EDGE | `adb shell am send-trim-memory com.maxeydev.picklelog RUNNING_CRITICAL` on share/editor | 1. Continue | No crash; state intact. | | Not Tested | | |
| ERR-10 | Notifications channel deleted by user | P3 | EDGE | Delete channel in system settings | 1. Wait for reminder | Channel recreated at next post; no crash. | | Not Tested | | |

### 7.3 Data integrity and persistence

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| DAT-01 🔴 | Everything survives force-stop and reboot | P0 | DATA | RD-7 + photos + settings | 1. Force-stop 2. Reboot 3. Open | Matches, people, photos, sort, card format, name, theme, sound, reminder, Pro identical. | | Not Tested | | |
| DAT-02 | Consistency across screens | P1 | DATA | RD-7 | 1. Compare list count, header, stats, paywall usage, settings Pro card, export file | All say 7 matches / 4–3. | | Not Tested | | |
| DAT-03 | Cascade on delete | P0 | DATA | D1 | 1. Delete 2. `adb shell run-as com.maxeydev.picklelog sqlite3 databases/picklelog.db "select count(*) from game_score; select count(*) from match_person;"` (if sqlite3 present; else export and inspect) | No game/person-link rows for D1 remain; `person` rows remain. | | Not Tested | | |
| DAT-04 | Edit is atomic | P1 | DATA | D1 | 1. Edit players + games + photo removal 2. Save | All changes applied together; no half state on any screen. | | Not Tested | | |
| DAT-05 | Timestamps | P2 | DATA | — | 1. Export after create, then after edit | `createdAt` unchanged after edit; `updatedAt` increases; both epoch ms. | | Not Tested | | |
| DAT-06 | Time zone stability | P1 | DATA | Log D1 in UTC+8; change zone to UTC−10 | 1. View | Match date and start/end wall-clock times unchanged (stored as local date/time, not instants). | | Not Tested | | |
| DAT-07 | Concurrent writes | P2 | DATA | Debug: run `Add-PickleMatches -Count 50` while logging a match in the UI | 1. Observe | All 51 present; no crash; counts consistent. | | Not Tested | | |
| DAT-08 | No orphan files after cancelled imports | P2 | DATA | Pick 3 photos, Close editor before save | 1. Wait 24 h (or adjust clock) and relaunch | Temporary imported files removed (discard + sweep). | | Not Tested | | |
| DAT-09 | People identity is stable | P1 | DATA | `Dave R.` / `DAVE R.` / ` dave  r. ` | 1. Log 3 matches with each spelling | One person; display name stays the first-typed "Dave R.". | | Not Tested | | |
| DAT-10 | Two users editing the same record | N/A | — | Single-user, single-device app | — | Not applicable. | | N/A | | |

---

## 8. Security, Accessibility, Compatibility, and Performance

### 8.1 Security and privacy

Authorized, non-destructive checks on your own devices only. "Verified" means the code inspection or a test proves it; "Risk" means it needs confirming.

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| SEC-01 🔴 | No unexpected network traffic | P0 | SEC | ENV-B, release build; run `adb shell dumpsys netstats detail` or a proxy (mitmproxy) | 1. Use every feature except the paywall for 10 min 2. Open paywall | No connections at all until the paywall/billing check; then only Google Play endpoints. No analytics/ads SDK traffic. | | Not Tested | | |
| SEC-02 🔴 | Runtime permissions are minimal | P0 | SEC | Release | `adb shell dumpsys package com.maxeydev.picklelog \| Select-String "permission"` | Runtime: only `CAMERA` and `POST_NOTIFICATIONS`, neither granted until used. No `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE`, location, contacts. (Also `ManifestPermissionsTest`.) | | Not Tested | | |
| SEC-03 | Exported components | P1 | SEC | Release APK | `adb shell dumpsys package com.maxeydev.picklelog \| Select-String -Pattern "exported"` or inspect merged manifest | Only `MainActivity` (launcher) is exported. FileProvider `…fileprovider` not exported, grants per-URI only. | | Not Tested | | |
| SEC-04 | FileProvider scope | P1 | SEC | — | Inspect `file_paths.xml`; share a card and an export; try `adb shell content read --uri content://com.maxeydev.picklelog.fileprovider/cards/<name>` from shell | Only `cache/camera`, `cache/cards`, `cache/exports` are exposed; database, DataStore and `files/photos` are not reachable; shell access without a grant is denied. | | Not Tested | | |
| SEC-05 🔴 | Share-card WebView is locked down | P0 | SEC | `LockdownTest` + SHR-18 | 1. Run `:ui:connectedDebugAndroidTest --tests "*LockdownTest*"` 2. SHR-18 | Pass. Requests outside `file:///android_asset/` get 403; file/content access off; user text injected with `textContent` (verified in `card.js`), not HTML. | | Not Tested | | |
| SEC-06 🔴 | Import cannot touch other files | P0 | SEC | BAK-17 | — | Verified in code (`ExportCodec.parsePhoto` path rules) — confirm with BAK-17 on device. | | Not Tested | | |
| SEC-07 | SQL injection via search/names | P0 | SEC | SCH-07, AUT with `'; DROP…` names | — | Room uses bound parameters (verified); `LIKE` wildcards escaped. Confirm no crash/data loss. | | Not Tested | | |
| SEC-08 🔴 | Debug tools absent from release | P0 | SEC | Release AAB/APK | 1. `adb shell content call --uri content://com.maxeydev.picklelog.debugtools --method status` on a release install 2. Inspect merged release manifest | Call fails ("Could not find provider"). No `DebugToolsProvider` in the release manifest/dex. (It is exported **without permission** in debug — never distribute debug builds.) | | Not Tested | | |
| SEC-09 | No user data in logs | P1 | SEC | After RD-7 usage | `adb logcat -d \| Select-String -Pattern "Dave R\|Rizal\|Ana\|Matthew\|purchaseToken"` | No matches/person/location/name/token text. (Code logs only exception class names.) | | Not Tested | | |
| SEC-10 | Notification privacy | P2 | SEC | Lock screen "hide sensitive" ON | 1. `Send-PickleReminder` | Lock screen shows a redacted notification (VISIBILITY_PRIVATE). | | Not Tested | | |
| SEC-11 | Purchase token handling | P1 | SEC | ENV-C after purchase | 1. Export JSON 2. Search logs 3. Share card | Token not in export, logs or card. Stored only in the app-private profile file. | | Not Tested | | |
| SEC-12 | Client-side entitlement (risk) | P2 | SEC | Rooted/emulator debug | Edit `files/<profile file>` to `isPro=true` | **Risk, by design:** there is no server verification; a rooted user can grant themselves Pro. Document acceptance (local-only app). Do not attempt on production devices. | | Not Tested | | |
| SEC-13 | Backup contents | P1 | SEC | BAK-28/29 | — | Photos excluded from cloud; the profile with the entitlement **is** included (see BAK-31, §13 R-08). | | Not Tested | | |
| SEC-14 | Erase completeness | P1 | SEC | ERS-03 | `adb shell run-as com.maxeydev.picklelog ls -R files cache no_backup databases` | No matches in DB, no photos, no exports, no DB pre-migration backups; only entitlement + app settings remain. | | Not Tested | | |
| SEC-15 | Privacy policy matches behaviour | P1 | SEC | SET-14 vs SEC-01/02/13 | Compare each claim | Every claim is true (no servers, no analytics, camera/notifications only when used, Google Play + Android backup disclosed). | | Not Tested | | |
| SEC-16 | Play Data Safety form consistency | P1 | SEC | Play Console draft | Compare with SEC-01/02 | "No data collected/shared" is consistent with the code. | | Not Tested | | |

Not applicable (no accounts/server): authentication, sessions, cookies, CSRF, IDOR across users, rate limiting, password policy.

### 8.2 Accessibility (target WCAG 2.2 AA, adapted for Android)

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| A11Y-01 🔴 | TalkBack: core logging flow | P0 | A11Y | TalkBack ON | 1. Log a full match without looking | Every control announced with a meaningful name and role ("Log a match", "Win", "Date, <date>. Change date", "Start time, 6:00 PM. Change", "Clear start time", "Remove game 2", "Use this suggestion", "Save"). Errors ("Pick Win or Loss…") are announced. | | Not Tested | | |
| A11Y-02 | TalkBack: list row is one sentence | P1 | A11Y | RD-7 | 1. Focus D1 | e.g. "Doubles, today, won, against Dave R. and Maya, with Ana, at Rizal Park Courts, scores 11 to 7, 11 to 9" (+ "with photo" where applicable). | | Not Tested | | |
| A11Y-03 | TalkBack: dashboard header | P1 | A11Y | RD-7 | 1. Focus header | "7 matches, 4 won, 3 lost, 57 percent won, current streak 4 weeks, counting all matches"; filtered adds "figures filtered by …"; action "Open full stats"; last-5 described "last 5 matches, oldest first: …". | | Not Tested | | |
| A11Y-04 | TalkBack: dialogs and sheets | P1 | A11Y | — | 1. Delete dialog, erase dialogs, filter sheet, upgrade prompts, mascot sheets, settings drawer | Focus moves into the dialog/sheet; title read first; focus returns to the trigger on close; drawer announced as pane "Settings". | | Not Tested | | |
| A11Y-05 | Headings | P2 | A11Y | — | 1. TalkBack headings navigation on Stats, Paywall, Privacy, Support | Section titles are headings. | | Not Tested | | |
| A11Y-06 | Touch targets ≥ 48 dp | P1 | A11Y | Accessibility Scanner | 1. Scan every screen | No undersized targets (photo move/remove, chip ✕, paywall ✕, game remove, time clear). | | Not Tested | | |
| A11Y-07 | Contrast | P1 | A11Y | Accessibility Scanner, light + dark | 1. Scan | Text ≥ 4.5:1 (large ≥ 3:1); UI components ≥ 3:1, including the dark-green header, chips, W/L badges, share card themes. | | Not Tested | | |
| A11Y-08 | 200% font + largest display | P0 | A11Y | Font Largest + Display Largest | 1. Visit every screen and dialog | No clipped or overlapping text; every button reachable; header scrolls away; dialogs scroll. | | Not Tested | | |
| A11Y-09 | Colour not the only signal | P1 | A11Y | Monochromacy simulation | 1. List, detail, cap banner, filter indicator, heatmap, chemistry chart | Meaning conveyed by text/icons as well ("W"/"L", "+N"/"−N", legends). | | Not Tested | | |
| A11Y-10 | Keyboard / switch access | P2 | A11Y | Bluetooth keyboard or Switch Access | 1. Tab through Home, editor, dialogs | All controls focusable in a logical order; visible focus indicator; Enter activates. | | Not Tested | | |
| A11Y-11 | Charts have text alternatives | P2 | A11Y | Pro stats + TalkBack | 1. Focus Form, weekdays, months, heatmap | Form: "Rolling win rate over your last 10 matches. It started at X% and is now Y%." Months: "<Month>: N wins, M losses" / "no matches". Weekdays per-day summary. | | Not Tested | | |
| A11Y-12 | Reduced motion | P2 | A11Y | Developer options → animator duration scale **off**; Settings → Accessibility → Remove animations | 1. Onboarding, home, sheets, ducks | Record: **no code reads the reduced-motion setting** — looping duck animations may keep moving. Check WCAG 2.2.2 (pause/stop for auto-moving content > 5 s). See §13 R-12. | | Not Tested | | |
| A11Y-13 | Images described | P2 | A11Y | TalkBack | 1. Onboarding illustrations, privacy duck, about duck ("Pet the duck"), photo tiles | Meaningful descriptions; purely decorative ducks skipped. | | Not Tested | | |
| A11Y-14 | Form errors identified | P1 | A11Y | TalkBack | 1. ONB-07, LOG-04, LOG-12, AUT-06 | Each error is announced and tied to its field. | | Not Tested | | |
| A11Y-15 | Pettable duck & gestures have alternatives | P3 | A11Y | — | 1. Photo pinch-zoom, swipe pager | Alternatives exist (double-tap zoom, "Previous/Next photo" buttons). | | Not Tested | | |

Specialist AT (Voice Access, braille display) testing is outside this plan; note it as a gap if the app targets those users.

### 8.3 Responsive design and compatibility

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| CMP-01 🔴 | Launch on API 26 | P0 | COMP | ENV-A | 1. Install debug, launch, log a match | Works; CI `Launch on API 26` green. | | Not Tested | | |
| CMP-02 🔴 | Launch on API 37 | P0 | COMP | ENV-B | Same | Works; CI green. | | Not Tested | | |
| CMP-03 | API 29–33 spot checks | P2 | COMP | Emulators 29, 31, 33 | 1. Smoke suite | Pass. Photo picker, notification permission boundary (33). | | Not Tested | | |
| CMP-04 | Small phone | P1 | COMP | 4.7" 720×1280 | 1. Every screen | No clipping; editor scrolls; keyboard doesn't hide Save. | | Not Tested | | |
| CMP-05 | Landscape phone | P2 | COMP | — | 1. Every screen | Usable; header and editor don't trap content. | | Not Tested | | |
| CMP-06 | Tablet / foldable | P2 | COMP | Pixel Tablet, Pixel Fold (fold/unfold) | 1. Every screen; fold during editing | Content not stretched edge-to-edge unreadably; state kept across fold. | | Not Tested | | |
| CMP-07 | Edge-to-edge & cutouts | P2 | COMP | Device with cutout, gesture + 3-button nav | 1. Every screen | No content under status/nav bars; FAB above nav bar; drawer respects insets. | | Not Tested | | |
| CMP-08 | Dark theme everywhere | P1 | UI | System dark | 1. Every screen, dialog, sheet, notification | No light-only colours, unreadable text or white flashes. | | Not Tested | | |
| CMP-09 | OEM skins | P2 | COMP | Samsung One UI, Xiaomi (ENV-C variants if available) | 1. Smoke + reminders | Works; check aggressive battery killers for RMD-09. | | Not Tested | | |
| CMP-10 | WebView versions | P2 | COMP | Oldest supported WebView on API 26 image | 1. SHR-01…05 | Renders; fonts load from assets. | | Not Tested | | |
| CMP-11 | Touch + mouse | P3 | COMP | ChromeOS / emulator with mouse | 1. Click, scroll, right-click | Works; no hover-only affordances. | | Not Tested | | |

Browsers: N/A (native app). Supported OS range: Android 8.0 (API 26) → Android API 37.

### 8.4 Performance and reliability

No formal budgets are agreed in code. The values marked **(proposed)** are from the PRD/RFC-018/TEST-CHECKLIST and **must be confirmed** before they are used as pass/fail gates.

| ID | Scenario | Pri | Type | Pre / Data | Steps | Expected | Actual | Status | Evidence | Bug |
|---|---|---|---|---|---|---|---|---|---|---|
| PRF-01 | Cold start | P1 | PERF | Mid-range ENV-C, release, RD-7 and 1,000 matches | `adb shell am force-stop com.maxeydev.picklelog; adb shell am start -W -n com.maxeydev.picklelog/.MainActivity` ×10 | Median `TotalTime` < 2000 ms **(proposed)**. Record both data sizes. | | Not Tested | | |
| PRF-02 | Log-a-match speed | P1 | PERF | RD-7 | 1. Stopwatch: FAB → Win → Save | ≤ 20 s for a result-only match incl. thinking (product promise); sheet open < 500 ms **(proposed)**. | | Not Tested | | |
| PRF-03 | List scroll at 1,000 | P1 | PERF | 1,000 matches with thumbnails | `MatchListScrollBenchmark` (AUTO-12) + manual fling | P90 frame time ≤ 16.7 ms **(proposed)**; no visible jank. | | Not Tested | | |
| PRF-04 | Search/filter/sort at 1,000 | P2 | PERF | 1,000 | 1. Type search, change filter/sort | Results update < 1 s **(proposed)**. | | Not Tested | | |
| PRF-05 | Stats at 1,000 | P2 | PERF | Pro, 1,000 | 1. Open stats | Renders < 1.5 s **(proposed)**; scroll smooth. | | Not Tested | | |
| PRF-06 | First share-card render | P2 | PERF | Cold start → Share | 1. Measure | < 2 s **(proposed)**; `FirstShareLatencyTest`. | | Not Tested | | |
| PRF-07 | Memory over a long session | P2 | PERF | Android Studio Profiler | 1. 30 min: open 50 details, 20 shares, 10 photo viewers | No steady heap growth; no OOM; Bitmaps released. | | Not Tested | | |
| PRF-08 | Battery / background | P2 | PERF | 24 h idle with reminders on | `adb shell dumpsys batterystats` | No wakeups except the weekly reminder/daily export-prompt worker. | | Not Tested | | |
| PRF-09 | APK/AAB size | P3 | PERF | Release | Check bundle size | Record; R8 + resource shrinking on. Sound/duck assets reasonable. | | Not Tested | | |
| PRF-10 | Baseline profile | P3 | PERF | `app/src/main/baseline-prof.txt` | Inspect | **File currently has 1 line** — effectively no baseline profile. Record cold-start impact; see §13 R-13. | | Not Tested | | |
| PRF-11 | DB with 10,000 matches (stress) | P3 | PERF | Pro, seed 10,000 | 1. Launch, scroll, export | No ANR; export < 32 MB so it can be re-imported (check size). | | Not Tested | | |

Concurrent users/simultaneous load: N/A (single-user local app).

---

## 9. Smoke and Regression Test Suites

### 9.1 Smoke suite (≈ 25 min, every new build, ENV-B debug unless noted)

Stop and report if **any** smoke test fails.

- [ ] AUTO-04/05/06 green (JVM tests) — or CI green on the commit
- [ ] CMP-02 Launch on API 37 (CI) · CMP-01 Launch on API 26 (CI)
- [ ] NAV-01 Cold start lands on Home
- [ ] ONB-06 Onboarding with a valid name (fresh install)
- [ ] LOG-01 Result-only match saves
- [ ] LOG-04 Save blocked without result
- [ ] LOG-05 Full doubles match persists every field
- [ ] EDT-02 Edit persists, no duplicate
- [ ] DET-06 Delete with confirmation
- [ ] LST-02 Default order
- [ ] DSH-01 Header figures (RD-7)
- [ ] SCH-01 Search by opponent
- [ ] FLT-01 Format filter
- [ ] PHO-01 Add one gallery photo
- [ ] SHR-01 Card renders · SHR-03 Share intent
- [ ] CAP-04 Save blocked at 50 (`cap-50`)
- [ ] BAK-02 Export produces a valid file
- [ ] ERR-01 Offline core flow (log + share only, 2 min)
- [ ] **Release candidates only:** SEC-08 Debug tools absent · PAY-02 Price loads (ENV-C)

### 9.2 Regression suite (before every release / after any change to a shared module)

**All P0 and P1 tests** in §5–§8 (listed per module in the tracker §11), plus E2E-01, E2E-03, E2E-05, plus AUTO-01…09. Additionally, re-run every test linked to a bug fixed in the release, and every test in the module the fix touched.

Targeted regression by change area:

| If the change touches… | Re-run at least |
|---|---|
| `:domain` streak/insurance/stats | AUTO-04, STK-*, INS-*, DSH-01/04, STA-01, PST-02…09, MIL-03 |
| `:domain` entitlement / `:data` billing | AUTO-04/05, CAP-*, PAY-*, ERS-10, SEC-12 |
| Room schema / DAO | AUTO-07, NAV-12 (upgrade over existing data), DAT-*, LST-*, SRT-*, SCH-*, FLT-*, BAK-08…20 |
| Match editor / drafts | AUTO-06/08, LOG-*, EDT-*, AUT-*, LAN-*, NAV-06/07 |
| Photos | PHO-*, DET-05/08, SHR-10, DAT-08, ERR-07/08 |
| Share card (HTML/CSS/JS/renderer) | AUTO-08 (`CardGoldenTest`, `LockdownTest`), SHR-*, SEC-05 |
| Export/import codec | AUTO-05/07 (`ExportCodecTest`, `BackupImportTest`, `BackupRoundTripTest`), BAK-* |
| Settings / onboarding / erase | ONB-*, SET-*, ERS-*, RMD-01…05 |
| Theme / typography / mascot | UI-*, CMP-08, A11Y-07/08, SET-06…08 |
| Gradle / dependencies / AGP / Kotlin | AUTO-01…10, smoke on API 26 and 37, SEC-02/03/08, PRF-01 |

### 9.3 Full suite

Everything in §5–§8 and §6, on ENV-A, ENV-B and ENV-C, plus E2E-06 and BAK-29/30 on ENV-C/D.

### 9.4 Cadence

| When | Run |
|---|---|
| Every commit/PR (CI) | AUTO-01…04, AUTO-10 (already wired in `.github/workflows/ci.yml`) |
| Every build you install | Smoke (§9.1) |
| Before each Play upload (internal/closed/production) | Regression (§9.2) + E2E-01, 03, 05 + SEC-01/02/03/08 + PAY-04/07/09/12 on ENV-C |
| Before production launch | Full suite (§9.3) |
| Monthly / after a major Android, WebView or Play Billing release | CMP-*, SHR-01…05, PAY-02/04/09, RMD-09/15, PRF-01/03 |
| After any data-loss report | DAT-*, BAK-08…20, ERS-*, NAV-06/12 |

---

## 10. Bug Reporting Template

Copy into a GitHub issue (`maxey05/picklelog`) or a `verification/bugs/BUG-NNN.md` file.

```markdown
### BUG-NNN — <short, specific title: what fails, where>

| Field | Value |
|---|---|
| Related feature / test | F-xx · <TEST-ID> |
| Severity | Critical / Major / Minor / Trivial  (impact on the user) |
| Priority | P0 / P1 / P2 / P3  (how soon it must be fixed) |
| Build | versionName / versionCode / commit SHA / debug or release |
| Device & OS | e.g. Pixel 7, Android 15 (API 35), One UI/OEM skin |
| Environment | ENV-A/B/C/D/E · WebView version · Play Store version · online/offline |
| Entitlement state | Free (N matches) / Pro / Pending |
| Reproduction frequency | Always / N of M attempts / Once |

**Preconditions**
- e.g. RD-7 loaded, filter "Singles" active, Don't keep activities ON

**Steps to reproduce**
1.
2.
3.

**Expected result** (quote the test's Expected column)

**Actual result** (exact text on screen, what happened to data)

**Evidence**
- Screenshot / screen recording:
- `adb logcat -d > bug-NNN.log` (attach; check for user data before sharing)
- `adb logcat -b crash -d`
- `Get-PickleStatus` output (debug builds)
- Export JSON (synthetic data only)

**Suspected cause** (optional, e.g. file/function)

**Workaround** (if any)

**Verification**
| Fixed in build | Retested by | Date | Result (Pass/Fail) | Regression tests re-run |
|---|---|---|---|---|
| | | | | |
```

**Severity vs priority guide:** Severity = impact (Critical: data loss, crash in core flow, privacy leak, payment wrong · Major: feature broken, no workaround · Minor: workaround exists · Trivial: cosmetic). Priority = order of fixing, which also weighs how many users hit it and launch timing. A Trivial typo on the paywall can be P1; a Major bug in an unreachable path can be P3.

---

## 11. Test Execution Tracker

**Definitions**

- *Executed* = Pass + Fail.
- *Completion %* = (Pass + Fail) ÷ (Total − N/A) × 100. Blocked and Not Tested count as **not** executed.
- *Pass rate* = Pass ÷ (Total − N/A) × 100. A Blocked or unexecuted test is never counted as passed.
- Update counts from the Status columns in §5–§8, §12 and §6 after each session.

| Module | IDs | Total | P0 | P1 | Not Tested | Pass | Fail | Blocked | N/A | Open bugs | Completion % | Notes / next actions |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Onboarding | ONB-* | 16 | 3 | 6 | 16 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Log a match | LOG-* | 30 | 4 | 12 | 30 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Edit a match | EDT-* | 8 | 2 | 5 | 8 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Players & autocomplete | AUT-* | 12 | 1 | 3 | 12 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Log another | LAN-* | 6 | 1 | 3 | 6 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Match list | LST-* | 12 | 2 | 5 | 12 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Sort | SRT-* | 10 | 0 | 8 | 10 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Search | SCH-* | 13 | 1 | 4 | 13 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Filters | FLT-* | 17 | 0 | 7 | 17 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Dashboard header | DSH-* | 12 | 1 | 2 | 12 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Stats (free) | STA-* | 8 | 0 | 4 | 8 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Pro stats | PST-* | 14 | 0 | 3 | 14 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Weekly streak | STK-* | 10 | 0 | 6 | 10 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Streak insurance | INS-* | 8 | 0 | 3 | 8 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Celebration sheets | MIL-* | 7 | 0 | 2 | 7 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Detail & delete | DET-* | 11 | 2 | 3 | 11 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Photos | PHO-* | 22 | 2 | 11 | 22 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Share card | SHR-* | 20 | 3 | 10 | 20 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Free cap & prompts | CAP-* | 12 | 3 | 5 | 12 | 0 | 0 | 0 | 0 | 0 | 0% | Resolve R-00 (50 vs 100) first. |
| Paywall & billing | PAY-* | 24 | 4 | 13 | 24 | 0 | 0 | 0 | 0 | 0 | 0% | Needs ENV-C (Play internal track + license tester). |
| Export / import / backup | BAK-* | 34 | 4 | 14 | 34 | 0 | 0 | 0 | 0 | 0 | 0% | BAK-29/30 need two physical phones. |
| Settings & info screens | SET-* | 22 | 0 | 7 | 22 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Streak reminders | RMD-* | 17 | 0 | 11 | 17 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Erase all data | ERS-* | 10 | 3 | 2 | 10 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Navigation & state | NAV-* | 12 | 3 | 3 | 12 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| General UI | UI-* | 6 | 0 | 1 | 6 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| End-to-end journeys | E2E-* | 9 | 4 | 5 | 9 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Input validation | NEG-* | 10 | 0 | 3 | 10 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Error handling | ERR-* | 10 | 1 | 4 | 10 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Data integrity | DAT-* | 10 | 2 | 4 | 9 | 0 | 0 | 0 | 1 | 0 | 0% | DAT-10 N/A (single user). |
| Security & privacy | SEC-* | 16 | 6 | 8 | 16 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Accessibility | A11Y-* | 15 | 2 | 7 | 15 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Compatibility | CMP-* | 11 | 2 | 2 | 11 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Performance | PRF-* | 11 | 0 | 3 | 11 | 0 | 0 | 0 | 0 | 0 | 0% |  |
| Automated suites | AUTO-* | 12 | 7 | 5 | 11 | 1 | 0 | 0 | 0 | 0 | 8% | AUTO-04 passed 2026-10-10. Run AUTO-01…03, 05…12 with Gradle; AUTO-07/08/09 have never been recorded green. |
| **Total** | | **477** | **63** | **194** | **475** | **1** | **0** | **0** | **1** | **0** | **0.2%** | |

**Session log**

| Date | Tester | Build | Env | Suites/IDs run | Pass | Fail | Blocked | New bugs | Notes / next actions |
|---|---|---|---|---|---|---|---|---|---|
| | | | | | | | | | |

---

## 12. Automation Recommendations

### 12.1 Existing automated suites (run these first)

| ID | Suite | Command (PowerShell, repo root) | Layer | Last recorded result | Actual | Status |
|---|---|---|---|---|---|---|
| AUTO-01 | Mechanical rule checks (versions, prerelease, keystore, destructive migration, READ_MEDIA_IMAGES, schema JSON, backup rules, domain date-time indirection) | `.\gradlew.bat checkRules` | Build | Not found | | Not Tested |
| AUTO-02 | ktlint | `.\gradlew.bat ktlintCheck` | Static | Not found | | Not Tested |
| AUTO-03 | Full build incl. JVM tests | `.\gradlew.bat build` | Build | Not found | | Not Tested |
| AUTO-04 | Domain unit tests (29 classes) | `.\gradlew.bat :domain:test` | Unit | 309/309 pass (your Gradle run, 2026-10-09) | 309/309 pass on 2026-10-10, compiled with the repo's Kotlin 2.3.21 + JUnit 4.13.2 outside Gradle | Pass |
| AUTO-05 | Data JVM tests (9 classes) | `.\gradlew.bat :data:testDebugUnitTest` | Unit | 77/77 pass (2026-10-09, stale) | | Not Tested |
| AUTO-06 | UI ViewModel tests (21 classes) | `.\gradlew.bat :ui:testDebugUnitTest` | Unit | 257/257 pass (2026-10-09, stale) | | Not Tested |
| AUTO-07 | Data instrumented (Room, DataStore, photos, backup, erase, reminders) | `.\gradlew.bat :data:connectedDebugAndroidTest` | Integration | **No record** | | Not Tested |
| AUTO-08 | UI instrumented (flows, list, filters, dashboard, onboarding, settings, backup, support, about, card goldens, lockdown, share intent) | `.\gradlew.bat :ui:connectedDebugAndroidTest` | Component / E2E-lite | **No record** (last known run in Sept failed) | | Not Tested |
| AUTO-09 | App instrumented (manifest permissions, backup rules, compression, billing flow, notification) | `.\gradlew.bat :app:connectedDebugAndroidTest` | Integration | **No record**; `BillingFlowTest` needs ENV-C | | Not Tested |
| AUTO-10 | CI (build, ktlint, rules, launch on API 26 & 37) | Push / PR | CI | Check GitHub Actions | | Not Tested |
| AUTO-11 | Release build sanity | `.\gradlew.bat :app:bundleRelease` then SEC-08 | Build | Not found | | Not Tested |
| AUTO-12 | List scroll macrobenchmark | `.\gradlew.bat :benchmark:connectedBenchmarkAndroidTest` (device, benchmark build) | Performance | Not found | | Not Tested |

### 12.2 What to automate next

Prefer the tools already in the repo: JUnit 4, kotlinx-coroutines-test, Turbine, Compose UI test (`createComposeRule`), the `ui/src/sharedTest` fakes (`FakeDependencies`, `FixedClock`, `FakeProStore`, …), Room testing, Macrobenchmark. Nothing below needs a new dependency unless marked **(new dep)**.

| # | Automate | Why | Manual tests replaced | Layer & tool | Setup & key assertions | Flakiness risks |
|---|---|---|---|---|---|---|
| 1 | **Nav-graph integration test**: onboarding → Home; cap-50 save → Paywall → `FakeProStore` unlock → auto-save exactly once | Highest-value money + data path; currently only VM-level | ONB-06/13, CAP-04/05, E2E-03 | Compose UI test hosting `PicklelogNavHost` with `FakeDependencies` | Assert route changes via test tags; `FakeMatchRepository` count = 51; save called once | Sheet animations → `composeRule.mainClock.autoAdvance`; use `waitUntil` not sleeps |
| 2 | **Notification deep link** while onboarding required / not required | Regressions here are silent | ONB-15, RMD-07/08 | Compose test on `PicklelogNavHost(openLogging = true)` | Editor shown only when onboarding complete; `onOpenLoggingHandled` called once | None significant |
| 3 | **State restoration** of editor draft, filter sheet, delete dialog, share hidden details | Process death is a top data-loss source | NAV-06/07, EDT-08, PHO-19, SHR-09 | `StateRestorationTester` (in Compose UI test) + `SavedStateHandle` VM tests | Draft fields equal after `emulateSavedInstanceStateRestore()` | Photo import coroutines → use fake queue |
| 4 | **Paywall screen UI test** | Only VM tests exist | PAY-01, 03, 05, 06, 10, 14, 17 | Compose UI + `FakeProStore` | Price button text, disabled while working, message per `StoreMessage`, auto-close on unlock | None |
| 5 | **Match detail UI test** | No UI test for detail/delete | DET-01/02/06/07/10 | Compose UI + `FakeMatchRepository` | Fields rendered, delete dialog flow, `isGone` pops | None |
| 6 | **Share screen variant picker UI test** | Goldens cover rendering, not controls | SHR-05…13 | Compose UI + `FakeCardRenderer` | Pro theme → upgrade prompt for free; format saved to `FakeCardFormatStore`; hidden details not persisted | WebView not involved (fake renderer) |
| 7 | **Reminder toggle permission flow** | Permission paths are easy to break | RMD-01…05 | Compose UI on API 33+ emulator with `GrantPermissionRule` / UiAutomator for the system dialog | Rationale shown, enable called after grant, blocked banner when disabled | System dialogs vary by OEM — run on emulator only |
| 8 | **Import fixtures B-1…B-9 as androidTest assets** | All-or-nothing and path rules are P0 | BAK-13…20, BAK-32/33, SEC-06 | Extend `BackupImportTest` (already has a harness) | DB row counts unchanged on every rejection; exact `ImportProblem` | None |
| 9 | **Search/filter edge cases** | Documented inconsistencies (R-01, R-02, R-09) | SCH-08…10, FLT-13…15 | Extend `MatchFilterSortSearchQueryTest` | Encode the *decided* behaviour once you choose it | None |
| 10 | **Screen goldens in light/dark/200% font** for Home, editor, stats, paywall, settings | Catches clipping regressions (A11Y-08, CMP-08) | A11Y-08, CMP-04/08, UI-03 | Reuse the `captureToImage` + golden approach from `CardGoldenTest` | Pixel diff within tolerance | Font rendering differs across API levels — pin one emulator image |
| 11 | **Startup macrobenchmark + Baseline Profile** | `baseline-prof.txt` is effectively empty | PRF-01, PRF-10 | Add `StartupBenchmark` to `:benchmark`; generate profile with `BaselineProfileRule` (androidx.benchmark already present; the Gradle plugin `androidx.baselineprofile` would be **(new dep)**) | `StartupTimingMetric` median; profile file committed | Emulator timings are noisy — use a physical device |
| 12 | **Run instrumented suites in CI** | AUTO-07/08 have no recorded green run | AUTO-07/08 | Extend the existing `device-matrix` job to run `:data:connectedDebugAndroidTest :ui:connectedDebugAndroidTest` on API 37 | Job fails on any test failure | Emulator boot flakiness; WebView golden diffs on swiftshader — keep goldens on one image or mark them `@LargeTest` and run nightly |
| 13 | **Accessibility checks in Compose tests** | Catch missing labels/targets automatically | A11Y-01, 06 | `AccessibilityChecks.enable()` from Espresso **(new dep: espresso-accessibility)**, or keep manual with Accessibility Scanner | No errors on core screens | False positives on custom drawn charts |

Keep manual: real Play purchases/refunds (PAY-04…13, 20/21), cloud backup restore (BAK-29/30), real share targets (SHR-04), OEM battery behaviour (CMP-09), TalkBack listening quality (A11Y-01…04), visual polish (UI-03), exploratory charters.

---

## 13. Coverage Gaps and Known Risks

### 13.1 Discrepancies and potential defects found during inspection

These are **observations from reading the code**, not executed results. Each is linked to the test that will confirm it. Decide "bug" or "intended" for each.

| ID | Finding | Evidence in code | Impact | Confirm with | Suggested decision / fix |
|---|---|---|---|---|---|
| **R-00** | **Free cap is 50 matches in code**, warnings at 40/48. Your most recent stated decision was a **100-match** free cap. FEATURES F35, `export-format.md` and the paywall all say 50. | `domain/entitlement/FreeTier.kt` | P0 product decision — the paywall copy, import cap and banners all follow this constant | CAP-*, PAY-01, BAK-11 | Confirm 50 or 100 before launch; F35 says "advertise 50, mean 50" — changing later is a bait-and-switch risk |
| R-01 | Opponent filter lists **every person** (partners and people whose matches were deleted) | `MatchListViewModel.filterChoices` uses `personRepository.observeAll()` | Choosing a partner-only or orphaned person shows an empty list | FLT-13, FLT-14 | Source the list from people who appear as opponents in ≥1 match |
| R-02 | Location filter uses exact text; prior-location list groups by exact text | `MatchListQueries` (`m.location = :location`), `observePriorLocations` `GROUP BY location` | "Rizal Park" and "rizal park" are two filter options and filter separately, while Pro "Courts" merges them | FLT-15, PST-05 | Normalise on save or compare `COLLATE NOCASE` |
| R-03 | A Play refund never revokes Pro | `EntitlementSignal.RefundConfirmed` is only emitted by debug tools; `NoPurchaseFound` never revokes | Refunded users keep Pro forever | PAY-20 | Probably intended (R58 "no path may revoke Pro" except refund) — but then nothing handles refunds at all. Decide and document |
| R-04 | Closing the editor discards changes with no confirmation | `onClose = popBackStack`, no `BackHandler` in `MatchEditScreen` | Accidental Back loses a typed match | LOG-27, EDT-07 | Add "Discard this match?" when the draft differs from its initial state |
| R-05 | "Donate!" row has no click action | `SupportScreen.kt` `SettingsRow(... DONATE)` without `clickable` | Looks tappable, does nothing | SET-20 | Hide until a donation method is chosen |
| R-06 | "Last export" is recorded when the share chooser **opens**, even if the user cancels | `BackupSettingsScreen` → `onShareLaunched` → `recordExportShared` | Backup reminder silenced although nothing was saved | BAK-05 | Accept (Android gives no result) but change copy to "Last export prepared", or use `ACTION_CREATE_DOCUMENT` to get a real result |
| R-07 | Once the Dark-mode switch is flipped there is no way back to "follow system"; CHANGE-REQUEST-001 §7.1 says Erase all data restores it, but erase **keeps** the theme (test `erasing_sends_the_user_back_through_onboarding_but_keeps_their_theme`) | `RoomLocalDataEraser`, `DataStoreAppSettingsStore` | Doc is wrong; users can't return to system theme short of clearing app data | SET-07, ERS-03 | Fix the doc, or add a "System" option |
| R-08 | Profile file (with `isPro` and purchase token) is included in cloud backup and device transfer | `data_extraction_rules.xml` excludes only `photos/` | Pro can arrive on a new device from the backup rather than Play; device transfer to a different account carries it | BAK-31, SEC-13 | Exclude the profile from backup and rely on Play restore, or accept and document |
| R-09 | Search is accent-sensitive and ASCII-only case-insensitive for location/notes/paddle, while autocomplete is accent-insensitive | `SearchTerm` + SQLite `LIKE`; `suggestionKey` strips marks | "jose" finds José in autocomplete but not in search | SCH-09, SCH-10 | Store a folded search column, or accept and document |
| R-10 | No maximum length on opponent/partner names, location, paddle, notes | `MatchDraft`, `PersonSlotField`, `AutocompleteField` | Layout breakage on list/detail/card/filter sheet; large exports | NEG-01, SHR-17 | Cap names (e.g. 40) and notes (e.g. 2,000) |
| R-11 | Save path has no error handling; a DB write failure (disk full) propagates from `viewModelScope.launch` | `MatchEditViewModel.save()` | Crash and possibly lost draft on low storage | ERR-02 | Catch `SQLiteException`/`IOException`, keep the draft, show a message |
| R-12 | No reduced-motion handling; ducks animate continuously | No reads of animator scale / `Settings.Global.ANIMATOR_DURATION_SCALE` in `ui` | WCAG 2.2.2 risk; motion-sensitive users | A11Y-12 | Respect "Remove animations"; stop looping after a few seconds |
| R-13 | Baseline profile file has 1 line | `app/src/main/baseline-prof.txt` | Slower cold start / first scroll than possible | PRF-01, PRF-10 | Generate a real profile (automation #11) |
| R-14 | Import validates structure but not UI rules (future dates, scores > 999, whitespace-only text, sparse game numbers, untrimmed strings) | `ExportCodec.parseMatch` | Data the UI can't produce can enter via a crafted/edited file | BAK-32 | Apply the same rules as the editor, or reject |
| R-15 | Debug `DebugToolsProvider` is exported with no permission in debug builds | `app/src/debug/AndroidManifest.xml` | Any app on a tester's phone could seed/clear data or grant Pro in a debug build | SEC-08 | Never distribute debug builds; add `android:permission` signature-level for safety |
| R-16 | Instrumented suites have no recorded green run, and CI doesn't run them | No `androidTest-results`; `ci.yml` | P0 tests like `LockdownTest`, `MigrationTest`, `MatchDeletionTest` unverified | AUTO-07/08/09 | Run them now; add to CI (automation #12) |
| R-17 | Celebration sheets depend on in-session transitions (`rememberSaveable`) | `StreakMilestoneHost` | Milestone can re-trigger after delete + re-log; missed if crossed while the UI wasn't observing | MIL-02, MIL-05 | Persist "last celebrated milestone" if that matters |
| R-18 | Editing a match that disappears before Save throws (`requireNotNull`) | `MatchEditViewModel.buildMatch` | Low reachability (debug tools, split-screen with another instance) | — | Guard and finish gracefully |
| R-19 | Zero-width/invisible characters can create invisible people | `normalizePersonName` uses `trim()` + `\s+` | Confusing autocomplete/filter entries | NEG-04 | Strip `\p{Cf}` before validation |
| R-20 | Entitlement is client-side only | `DataStoreEntitlementRepository` | Rooted users can unlock Pro | SEC-12 | Accept for a local-only app; document |
| R-21 | Documentation drift: `TEST-CHECKLIST.md` (Sept) predates most features; CR-001 erase/theme statement; `FEATURES.md` F64 `targetSdk 36` vs code 37 | docs vs code | Testers follow stale instructions | — | Mark old checklist superseded |

### 13.2 Known gaps and untestable areas

| Area | Why it can't be fully tested here | Mitigation |
|---|---|---|
| Real-money purchase behaviour | Only test cards allowed | License-tester purchases on internal track; watch first production orders |
| Google cloud backup service | Out of app control; timing opaque | BAK-29/30 on two real devices; disclose in onboarding (already done) |
| OEM background-work killers | Vendor-specific | CMP-09 on available devices; dontkillmyapp.com guidance |
| Share targets' own processing | Third-party apps | SHR-04 spot checks |
| WebView variants | Updated by Play independently of the app | CMP-10 + monthly SHR re-run |
| Specialist assistive tech | Needs specialist users/devices | Note as a gap |
| Performance thresholds | Not agreed in requirements | §8.4 values are **proposed**; agree them before using as gates |

### 13.3 Exploratory charters (30 min each)

1. *Explore logging at the court:* one hand, sun glare (max brightness, dark theme), interruptions; look for slow taps and mis-taps.
2. *Explore people management:* typos, nicknames, renames by editing; look for duplicate/invisible people and filter confusion.
3. *Explore time:* matches near midnight, Sunday/Monday boundaries, DST change (use a DST zone like America/New_York), travel across zones.
4. *Explore the money path:* every route into the paywall, back out, re-enter, purchase pending, kill app mid-flow.
5. *Explore data safety:* export, edit the JSON by hand in creative ways, import; erase; reinstall.
6. *Explore the share card:* every combination of ratio × theme × hidden details × photo × very long text.

---

## 14. Release-Readiness Checklist

Release readiness is decided from **recorded** results in §11 and open bugs — not from this document's expectations.

### 14.1 Exit criteria

- [ ] R-00 (free cap 50 vs 100) decided and the code, paywall copy, FEATURES F35 and `export-format.md` agree
- [ ] AUTO-01…09 green on the release candidate commit; AUTO-07/08 green on **API 26 and API 37**
- [ ] 100% of P0 tests executed and **Pass** (Completion % for P0 = 100, no Blocked)
- [ ] ≥ 95% of P1 tests executed; every P1 Fail has a fix or a written, dated waiver below
- [ ] 0 open Critical defects; 0 open Major defects without a waiver
- [ ] E2E-01, E2E-03, E2E-05 Pass on ENV-C with the **release** build from the Play internal track
- [ ] PAY-04, PAY-07, PAY-09, PAY-12 Pass with Play test cards (no `DEVELOPER_ERROR`)
- [ ] SEC-01, SEC-02, SEC-03, SEC-05, SEC-08 Pass on the release build
- [ ] BAK-29 (cloud restore) executed and recorded in `documentation/backup-restore-verification.md`
- [ ] A11Y-01, A11Y-08 Pass; contrast (A11Y-07) has no failures on primary screens
- [ ] PRF-01 within the agreed budget (agree the budget first)
- [ ] Every finding in §13.1 marked "fixed", "accepted" or "deferred" with a reason

### 14.2 Store / compliance

- [ ] `versionCode` incremented; `versionName` correct; About and drawer show it (SET-16)
- [ ] Signed with the upload key (not debug); `keystore/` not committed (`checkRules`)
- [ ] Release AAB has R8 + resource shrinking; no `DebugToolsProvider` (SEC-08)
- [ ] `targetSdk` meets Play's current requirement (37 in code)
- [ ] In-app product `picklelog_pro` active with correct price per country
- [ ] Privacy policy in-app (SET-14) matches the hosted URL in the listing and the Data Safety form (SEC-15/16)
- [ ] Store listing screenshots match the current UI (dark green header, duck)
- [ ] Closed test with ≥ 12 testers for 14 days completed if the developer account requires it

### 14.3 Waivers

| Defect / test | Severity | Reason accepted | Owner | Date | Revisit by |
|---|---|---|---|---|---|
| | | | | | |

**Go / No-go:** ☐ Go ☐ No-go — signed: __________ date: __________

---

## 15. Final Testing Summary

### 15.1 Test inventory by priority and category

| Type \ Priority | P0 | P1 | P2 | P3 | N/A | Total |
|---|---|---|---|---|---|---|
| A11Y | 2 | 8 | 8 | 1 | 0 | 19 |
| AUTO | 7 | 5 | 0 | 0 | 0 | 12 |
| BVA | 0 | 9 | 10 | 4 | 0 | 23 |
| COMP | 2 | 5 | 8 | 3 | 0 | 18 |
| DATA | 10 | 11 | 5 | 0 | 0 | 26 |
| E2E | 6 | 6 | 0 | 0 | 0 | 12 |
| EDGE | 1 | 6 | 14 | 15 | 0 | 36 |
| FUN | 17 | 85 | 73 | 14 | 0 | 189 |
| INT | 4 | 14 | 4 | 0 | 0 | 22 |
| NEG | 3 | 22 | 13 | 4 | 0 | 42 |
| PERF | 0 | 3 | 7 | 3 | 0 | 13 |
| REG | 1 | 1 | 0 | 0 | 0 | 2 |
| SEC | 10 | 10 | 2 | 0 | 0 | 22 |
| UI | 0 | 9 | 13 | 18 | 0 | 40 |
| — | 0 | 0 | 0 | 0 | 1 | 1 |
| **Total** | **63** | **194** | **157** | **62** | **1** | **477** |

Tests marked 🔴 (release blockers): **43** — ONB-06, LOG-01, LOG-04, LOG-28, EDT-01, EDT-02, AUT-07, LST-02, DSH-01, DET-06, PHO-01, PHO-08, SHR-01, SHR-03, SHR-13, SHR-18, CAP-04, CAP-05, PAY-04, PAY-07, PAY-09, PAY-12, BAK-02, BAK-08, BAK-16, BAK-17, RMD-07, ERS-03, NAV-01, NAV-06, E2E-01, E2E-03, E2E-05, ERR-01, DAT-01, SEC-01, SEC-02, SEC-05, SEC-06, SEC-08, A11Y-01, CMP-01, CMP-02.

`AUTO` rows are the automated suites in §12.1; their priority reflects the risk they cover. Status today: **{tot['ps']} Pass** (AUTO-04), **{tot['nt']} Not Tested**, **{tot['na']} N/A**, overall completion **{comp}%**.

### 15.2 Critical user journeys and release-blocking checks

E2E-01 first-time logging · E2E-03 cap → purchase → auto-save · E2E-05 export → erase → import · plus every 🔴 test in §5–§8.

### 15.3 Current state (2026-10-10)

- **Executed by me:** `:domain` unit tests — 309/309 pass. Nothing else.
- **Recorded earlier (stale):** `:data` 77/77 and `:ui` 257/257 JVM tests passed on 2026-10-09.
- **Unverified:** all instrumented suites, all manual tests, billing, backup restore, performance.
- **Biggest open questions:** R-00 free-cap number; R-03 refund policy; R-08 entitlement in backups; R-11 crash risk on full storage; R-04 no discard confirmation.
- **Readiness:** cannot be assessed until the P0 tests are executed and recorded. Current status: **Not ready — untested.**

---

## Appendix A — Feature ↔ test coverage matrix

| Feature | Test cases | # | P0 | P1 | Automated coverage already in repo |
|---|---|---|---|---|---|
| F-01 App launch & bootstrap | NAV-01…03, PRF-01, CMP-01, CMP-02 | 6 | 3 | 2 | — |
| F-02 Onboarding + required name | ONB-01…16 | 16 | 3 | 6 | OnboardingTest, OnboardingViewModelTest, OnboardingScreenTest |
| F-03 Log a match (create) | LOG-01…30 | 30 | 4 | 12 | MatchEditViewModelTest, MatchDraftTest, MatchLoggingFlowTest, MatchPersistenceTest |
| F-04 Edit a match | EDT-01…08 | 8 | 2 | 5 | MatchEditViewModelTest, MatchPersistenceTest |
| F-05 Players & autocomplete | AUT-01…12 | 12 | 1 | 3 | SuggestPeopleTest, NameNormalizerTest, MatchEditSuggestionsTest, AutocompleteFieldTest, PersonRepositoryTest, SuggestionQueryTest |
| F-06 Game scores & result advisory | LOG-10…18 | 9 | 0 | 3 | SuggestResultTest, MatchDraftTest |
| F-07 Time range & duration | LOG-19…23, SRT-07…08 | 7 | 0 | 6 | DeriveDurationTest |
| F-08 Log another | LAN-01…06 | 6 | 1 | 3 | LogAnotherFlowTest |
| F-09 Match list | LST-01…12 | 12 | 2 | 5 | MatchListViewModelTest, MatchListScreenTest, MatchRowTest, MatchListQueryTest |
| F-10 Sort | SRT-01…10 | 10 | 0 | 8 | MatchSortTest, MatchFilterSortSearchQueryTest, DataStoreMatchSortStoreTest |
| F-11 Search | SCH-01…13 | 13 | 1 | 4 | SearchTermTest, MatchFilterSortSearchQueryTest |
| F-12 Filters | FLT-01…17 | 17 | 0 | 7 | FilterStateTest, DatePresetTest, FilterUiTest |
| F-13 Dashboard header | DSH-01…12 | 12 | 1 | 2 | DashboardViewModelTest, DashboardUiTest, BasicStatsTest, RollPlanTest |
| F-14 Stats screen (free) | STA-01…08 | 8 | 0 | 4 | DashboardUiTest |
| F-15 Pro stats | PST-01…14 | 14 | 0 | 3 | AdvancedStatsTest, ProInsightsTest |
| F-16 Weekly streak | STK-01…10 | 10 | 0 | 6 | StreakEngineTest, WeekKeyTest |
| F-17 Streak insurance (Pro) | INS-01…08 | 8 | 0 | 3 | StreakInsuranceTest, SkipNoticesTest, SkipUsedNoticeTest |
| F-18 Celebration sheets | MIL-01…07 | 7 | 0 | 2 | — |
| F-19 Match detail | DET-01…05, DET-11 | 6 | 0 | 1 | MatchDetailViewModelTest, MatchNumberQueryTest |
| F-20 Delete match | DET-06…10, DAT-03 | 6 | 3 | 2 | MatchDeletionTest |
| F-21 Photos | PHO-01…22 | 22 | 2 | 11 | PhotoOrderTest, MatchEditPhotosTest, PhotoPickerSectionTest, ImageCompressorTest, QueuedPhotoImporterTest, MatchPhotoPersistenceTest, PhotoStoreTest, CompressionSizeTest |
| F-22 Share card | SHR-01…07, SHR-14…20 | 14 | 3 | 5 | SharePreviewViewModelTest, CardDataTest, CardGoldenTest, LockdownTest, FirstShareLatencyTest, ShareIntentLauncherTest |
| F-23 Card variants | SHR-08…13 | 6 | 0 | 5 | CardFormatTest, DataStoreCardFormatStoreTest, CardGoldenCoverageTest |
| F-24 Free-tier cap & warnings | CAP-01…12 | 12 | 3 | 5 | FreeTierRulesTest, MatchEditEntitlementTest |
| F-25 Paywall & Play Billing | PAY-01…24 | 24 | 4 | 13 | EntitlementInvariantTest, PlayEntitlementRepositoryTest, PlayProStoreTest, PurchaseAcknowledgerTest, BillingStartupCheckTest, BackoffTest, PaywallViewModelTest, BillingFlowTest |
| F-26 Upgrade prompts | PHO-03…05, SHR-11, INS-06, STA-02, STA-03 | 7 | 0 | 7 | — |
| F-27 Export (JSON) | BAK-01…07 | 7 | 1 | 2 | ExportCodecTest, BackupExportTest, BackupViewModelTest, BackupSettingsScreenTest |
| F-28 Import (JSON) | BAK-08…22, BAK-32…34 | 18 | 3 | 8 | ImportCapTest, BackupImportTest, BackupRoundTripTest |
| F-29 Export reminders | BAK-23…27 | 5 | 0 | 1 | ExportPromptPolicyTest, ExportPromptViewModelTest, DataStoreExportPromptStoreTest |
| F-30 Android Auto Backup / device transfer | BAK-28…31 | 4 | 0 | 3 | BackupRulesTest, checkBackupRules |
| F-31 Settings drawer | SET-01…22 | 22 | 0 | 7 | SettingsViewModelTest, SettingsDrawerTest, DataStoreAppSettingsStoreTest, DirectoryAppCacheTest, DisplayNameTest |
| F-32 Streak reminders | RMD-01…17 | 17 | 0 | 11 | ReminderScheduleTest, StreakReminderTest, DataStoreReminderStoreTest, ReminderSchedulerTest, StreakNotificationTest, StreakAtRiskTest |
| F-33 Erase all data | ERS-01…10 | 10 | 3 | 2 | EraseAllDataTest, RoomLocalDataEraserTest |
| F-34 Privacy / About / Support | SET-14…22 | 9 | 0 | 2 | AboutScreenTest, SupportScreenTest |
| F-35 Dark theme | SET-06…08, CMP-08 | 4 | 0 | 3 | — |
| F-36 Sound effects | SET-09…10 | 2 | 0 | 0 | CuePolicyTest |
| F-37 Mascot (duck) | UI-01…06, A11Y-12 | 7 | 0 | 1 | — |
| F-38 Process-death & state restoration | NAV-04…12 | 9 | 2 | 2 | — |
| F-39 Debug tools provider | SEC-08 | 1 | 1 | 0 | — |
| *End-to-end journeys* (cross-cutting) | all E2E-* | 9 | 4 | 5 | — |
| *Input validation (all forms)* (cross-cutting) | all NEG-* | 10 | 0 | 3 | — |
| *Error handling & resilience* (cross-cutting) | all ERR-* | 10 | 1 | 4 | — |
| *Data integrity & persistence* (cross-cutting) | all DAT-* | 10 | 2 | 4 | MigrationTest, MatchDeletionTest, MatchPersistenceTest |
| *Security & privacy* (cross-cutting) | all SEC-* | 16 | 6 | 8 | ManifestPermissionsTest, LockdownTest, EntitlementInvariantTest |
| *Accessibility* (cross-cutting) | all A11Y-* | 15 | 2 | 7 | — |
| *Compatibility & responsive* (cross-cutting) | all CMP-* | 11 | 2 | 2 | — |
| *Performance* (cross-cutting) | all PRF-* | 11 | 0 | 3 | MatchListScrollBenchmark, FirstShareLatencyTest |
| *Automated suites* (cross-cutting) | all AUTO-* | 12 | 7 | 5 | — |

Every test ID in this document maps to at least one feature (F-01…F-39) or cross-cutting row; every feature has at least one test.

## Appendix B — Synthetic import fixtures

All data below is invented. Save files with UTF-8 encoding (no BOM unless stated) and copy them to the device (`adb push <file> /sdcard/Download/`), then pick them via "Import from a file".

**B-0 · Valid base file (2 people, 2 matches)** — `b0-valid.json`

```json
{
  "format": "picklelog-export",
  "schemaVersion": 1,
  "exportedAt": "2026-10-01T10:00:00Z",
  "people": [
    { "id": "11111111-1111-4111-8111-111111111111", "displayName": "Fixture Fred", "createdAt": 1790000000000 },
    { "id": "22222222-2222-4222-8222-222222222222", "displayName": "Fixture Gia", "createdAt": 1790000000000 }
  ],
  "matches": [
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
      "format": "DOUBLES", "date": "2026-09-30", "result": "WIN",
      "startTime": "18:00", "endTime": "19:15",
      "location": "Fixture Courts", "paddle": null, "notes": "fixture one",
      "createdAt": 1790000000000, "updatedAt": 1790000000000,
      "opponentIds": ["11111111-1111-4111-8111-111111111111"],
      "partnerId": "22222222-2222-4222-8222-222222222222",
      "games": [ { "gameNumber": 1, "myScore": 11, "opponentScore": 6 } ],
      "photos": []
    },
    {
      "id": "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
      "format": "SINGLES", "date": "2026-09-23", "result": "LOSS",
      "startTime": null, "endTime": null,
      "location": null, "paddle": null, "notes": null,
      "createdAt": 1789400000000, "updatedAt": 1789400000000,
      "opponentIds": ["11111111-1111-4111-8111-111111111111"],
      "partnerId": null,
      "games": [],
      "photos": []
    }
  ]
}
```

| Fixture | How to make it from B-0 | Expected (test) |
|---|---|---|
| B-1 | A plain text file `not json at all` (or any `.jpg`) | NOT_READABLE (BAK-13) |
| B-2 | `{ "hello": "world" }` | NOT_A_PICKLELOG_EXPORT (BAK-14) |
| B-3 | B-0 with `"schemaVersion": 2` | NEWER_VERSION (BAK-15) |
| B-4 | B-0 plus a third match whose `"result": "DRAW"` | CORRUPT_CONTENT, nothing imported (BAK-16) |
| B-5 | B-0 with match 1 `"photos": [ { "id": "cccccccc-cccc-4ccc-8ccc-cccccccccccc", "relativePath": "photos/../databases/picklelog.db", "width": 10, "height": 10, "byteSize": 1, "sortIndex": 0 } ]`; variants `photos/a/b.jpg`, `photos\\x.jpg`, `../x.jpg` | CORRUPT_CONTENT each (BAK-17) |
| B-6 | Generated with N matches (script below) | BAK-11/12 |
| B-7 | B-0 saved **with** a UTF-8 BOM (Windows PowerShell 5.1: `Set-Content -Encoding UTF8`; PowerShell 7: `-Encoding utf8BOM`) | Imports (BAK-19) |
| B-8 | Any file > 32 MB (e.g. B-0 with a 33 MB `notes` string) | TOO_LARGE (BAK-18) |
| B-9 | B-0 with match 2 `"date"` = tomorrow, games `[{"gameNumber":3,"myScore":5000,"opponentScore":0},{"gameNumber":7,"myScore":0,"opponentScore":11}]`, `"location": "   "`, `"notes"` = 20,000 chars | Accepted; observe UI (BAK-32) |
| B-10 | B-0 with a third person `{ "id": "33333333-3333-4333-8333-333333333333", "displayName": "fixture fred", "createdAt": 1 }` | CORRUPT_CONTENT, duplicate name (BAK-33) |

**Generator for B-6 (Windows PowerShell 5.1 or 7)** — run from any folder; writes UTF-8 without BOM:

```powershell
function New-PickleFixture {
    param([int]$Count = 10, [string]$Path = "$env:TEMP\picklelog-fixture-$Count.json")
    $personId = [guid]::NewGuid().ToString()
    $now = [DateTimeOffset]::UtcNow
    $matchList = foreach ($i in 1..$Count) {
        $created = $now.AddDays(-$i).ToUnixTimeMilliseconds()
        [ordered]@{
            id = [guid]::NewGuid().ToString()
            format = "SINGLES"
            date = $now.AddDays(-$i).ToString("yyyy-MM-dd")
            result = $(if ($i % 2) { "WIN" } else { "LOSS" })
            startTime = $null; endTime = $null
            location = "Fixture Courts"; paddle = $null; notes = "fixture $i"
            createdAt = $created; updatedAt = $created
            opponentIds = @($personId); partnerId = $null
            games = @([ordered]@{ gameNumber = 1; myScore = 11; opponentScore = 7 })
            photos = @()
        }
    }
    $file = [ordered]@{
        format = "picklelog-export"; schemaVersion = 1
        exportedAt = $now.ToString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        people = @([ordered]@{ id = $personId; displayName = "Fixture Fred"; createdAt = $now.ToUnixTimeMilliseconds() })
        matches = @($matchList)
    }
    [IO.File]::WriteAllText($Path, ($file | ConvertTo-Json -Depth 6))
    $Path
}
New-PickleFixture -Count 10
```

> Verify the generated file once with BAK-12 (Pro) before relying on it for BAK-11. If the import is rejected, open the JSON and check that single-element arrays (`opponentIds`, `games`) are arrays, not bare values.
