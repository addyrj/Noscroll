# NoReels — Features & Test Report

App: **NoReels** (`com.noreels`) · version 1.2 · Android 8 – 17 (minSdk 26, targetSdk 36, compileSdk 37)
Tested on: **Realme RMX3231, Android 11 (API 30), 32-bit** — 17 Oct 2026
v1.2 features built: 20 Oct 2026

Legend: ✅ passed on phone · 🟡 built, not fully tested live · ⏳ to test on another phone · ➖ removed / not built (your decision)

---

## 1. How to install on another phone

1. Copy `C:\NoScroll\NoReels\apk\NoReels-v1.2.apk` to the phone (USB, Drive, WhatsApp to yourself…).
2. Tap the file → allow **"Install unknown apps"** for that app → Install.
   - If you see **"App blocked to protect your device"** (Play Protect): install over USB instead
     (see below), or turn off Play Protect scanning only while installing — details in issue.md.
3. Open NoReels → grant on the setup screen:
   - **Accessibility** (required) — Settings → Accessibility → Installed apps → NoReels → ON
   - **Notifications** (required)
   - **Ignore battery optimization** (recommended)
   - **Usage access** (optional)
4. Android 13+: if Accessibility says **"Restricted setting"** → App info → ⋮ (top right) →
   **Allow restricted settings** → try again.

Or with the phone on USB: `adb install -r C:\NoScroll\NoReels\apk\NoReels-v1.2.apk`

---

## 2. Mode 1 — Shorts Block (normal, always on)

| Feature | How it works | Result |
|---|---|---|
| YouTube Shorts detection | Reads the Shorts player screen | ✅ Detected (Shorts player + Shorts tab) |
| Long YouTube videos allowed | Only Shorts are counted | ✅ Home + normal videos not blocked |
| Session limit 5 min | Counts every second in the feed | ✅ Session saved as exactly 300 s |
| 1-minute warning | Heads-up notification at 1:00 left | ✅ Fired on time |
| Session countdown notification | "Short-video session · x:xx left" | ✅ |
| Block screen at 0:00 | Full-screen "Time's Up" over the app | ✅ |
| **Audio muted on block** | Audio focus grabbed when block screen appears | 🟡 Built v1.2 |
| Cooldown 15 min | Shorts blocked until cooldown ends, live countdown | ✅ "Cooldown: 11:55" shown, steady (no flicker) |
| Go Back button | Leaves Shorts, back to YouTube Home | ✅ |
| Daily limit 30 min (shared by all apps) | Daily total across all apps | 🟡 Total is tracked; the "Daily allowance used" screen at 30:00 was not reached live |
| Leaving Shorts pauses the session | Return continues the same session (no fresh 5 min) | ✅ Dashboard showed "Session paused — 4:51 left" |
| Reset at local midnight | Daily total goes to 0 at 00:00 | 🟡 Logic built; cannot test without waiting for midnight |
| Instagram Reels | Reels screen detection | ⏳ Instagram not logged in on test phone |
| Facebook Reels | Reels tab detection | ⏳ Facebook not logged in on test phone |
| Snapchat Spotlight | Spotlight tab detection | ⏳ Not installed on test phone |
| TikTok (whole app) | Whole app counted | ⏳ Not installed on test phone |
| Custom block list | Add any app from installed apps picker | 🟡 Screen works; not tested with a real app |
| Turn an app on/off (Apps screen) | Disabled app is not blocked | 🟡 Toggles shown; effect not tested live |
| Uncertain detection log | Unsure screens logged, **not counted** | ✅ One entry logged ("'Shorts' visible without tab bar") |

## 3. Mode 2 — Hard Focus

| Feature | How it works | Result |
|---|---|---|
| Length options | 30 min, 30 + 10 min, 30 + 20 min | ✅ Screen shows all 3 |
| Blocks all social media fully | YouTube (long + Shorts), Facebook, Instagram, TikTok, Snapchat + custom apps — even if switched off on Apps screen | 🟡 Built; old version of the block screen was ✅, new full-block not run live (would lock your apps 30 min) |
| Other apps keep working | Calls, messages, WhatsApp, Chrome, study apps | 🟡 Not run live |
| "End focus" / "Add 25 minutes" | Visible but **disabled** until timer ends | 🟡 Built, not seen live |
| Survives restart / reinstall | Focus timer kept | ✅ Timer kept after reinstall (earlier version) |

## 4. NEW — Pause Before Opening (one sec style) — v1.2

| Feature | How it works | Result |
|---|---|---|
| Pause overlay | "Take a moment" screen when opening a social app | 🟡 Built v1.2 |
| Countdown timer | "Continue" button appears after the set delay (default 5 sec) | 🟡 Built v1.2 |
| "Yes, continue" | Dismisses the pause, user enters the app | 🟡 Built v1.2 |
| "No, go back" | Sends user to home screen | 🟡 Built v1.2 |
| Pause cooldown | Won't show again for the same app for 5 minutes | 🟡 Built v1.2 |
| Configurable delay | 3–30 seconds in Settings | 🟡 Built v1.2 |

## 5. NEW — Curious Mode — v1.2

| Feature | How it works | Result |
|---|---|---|
| Short peek time | Overrides session limit with a short peek (default 30 sec) | 🟡 Built v1.2 |
| Configurable duration | 10–60 seconds in Settings | 🟡 Built v1.2 |
| Block after peek | Block screen appears after the peek time | 🟡 Built v1.2 |

## 6. NEW — Re-intervention — v1.2

| Feature | How it works | Result |
|---|---|---|
| Re-prompt | After X minutes in a social app, the pause prompt appears again | 🟡 Built v1.2 |
| Configurable interval | 5–60 minutes in Settings | 🟡 Built v1.2 |

## 7. NEW — Per-App Daily Time Limits — v1.2

| Feature | How it works | Result |
|---|---|---|
| Per-app limit chips | Tap Off / 5m / 10m / 15m / 20m / 30m per app on the Apps screen | 🟡 Built v1.2 |
| APP_LIMIT block reason | "App limit reached" block screen when a single app's limit is used | 🟡 Built v1.2 |
| Independent of global daily limit | Per-app limit can be lower than the shared 30 min limit | 🟡 Built v1.2 |

## 8. NEW — Custom Scheduled Blocking — v1.2

| Feature | How it works | Result |
|---|---|---|
| Custom night hours | Start and end hour adjustable in Settings (default 10pm–7am) | 🟡 Built v1.2 |
| Works across midnight | e.g. 11pm–6am blocks correctly | 🟡 Built v1.2 |

## 9. NEW — Gamification & Achievements — v1.2

| Feature | How it works | Result |
|---|---|---|
| Focus Points | Earned for: days under limit (10 pts), streaks (5 pts/day), sessions (2 pts), blocks (1 pt) | 🟡 Built v1.2 |
| Level system | Levels 1–5: Beginner → Aware → Focused → Disciplined → Master | 🟡 Built v1.2 |
| Achievements | 10 unlockable achievements (First Day, 3-Day Streak, Weekly Warrior, etc.) | 🟡 Built v1.2 |
| Focus Progress section | Shows on Reports screen with level, points, stats, achievements | 🟡 Built v1.2 |

## 10. NEW — Installed Apps Picker — v1.2

| Feature | How it works | Result |
|---|---|---|
| Browse installed apps | Shows all non-system installed apps with names | 🟡 Built v1.2 |
| Search / filter | Search by name or package name | 🟡 Built v1.2 |
| Social apps first | Known social media packages highlighted at top | 🟡 Built v1.2 |
| Toggle to block | Switch any app on/off for whole-app blocking | 🟡 Built v1.2 |
| Replaces manual package entry | No need to type com.example.app any more | 🟡 Built v1.2 |

## 11. Block screen options (Settings)

| Feature | Result |
|---|---|
| Motivational quote on block screen (custom text) | ✅ Quote shown on block screen |
| Strict mode (5-sec hold + reason to leave) | 🟡 Built, not tested live |
| Night schedule (custom hours, default 10pm – 7am) | 🟡 Built; needs testing at night |

## 12. Screens

| Screen | Result |
|---|---|
| Setup / permissions (✅/❌ per permission, re-checks on return) | ✅ |
| Dashboard (used today, budget bar, session timer, sessions, blocked attempts, streak) | ✅ |
| Apps (known social toggles + per-app limits + installed apps picker + search) | 🟡 v1.2 |
| Reports (weekly bar chart, best/worst day, totals, streak + Focus Progress + Achievements) | 🟡 v1.2 |
| Settings (time limits + pause + curious mode + re-intervention + night schedule + block screen) | 🟡 v1.2 |
| Hard Focus | ✅ |
| Detection log | ✅ |
| Light mode / Dark mode | ✅ both |
| Accessibility disclosure popup (Play rule) → Agree opens Accessibility settings | ✅ |
| Landscape | ✅ Works (phone was rotated during testing) |
| App name **NoReels** + new **Reels Block** icon in launcher | ✅ |

## 13. Reliability

| Case | Result |
|---|---|
| Clean install over USB (`adb install`), v1.2 release | ⏳ Test on phone |
| Install by tapping the APK file (Files/WhatsApp/Drive) | ❌ Blocked by Play Protect ("App blocked to protect your device") — see issue.md |
| Saved data survives app reinstall | ✅ |
| App killed in background (battery saver / low memory / crash) | ✅ Restarts by itself, blocking continues |
| Realme "CLEAR ALL" recent apps | ✅ Blocker keeps running |
| Accessibility switched off | ✅ "Blocking is OFF — tap to turn it back on" notification; removed when back on |
| Phone restart | 🟡 Not done on test phone (notification-after-restart built) |
| Force stop | ⚠️ Android limit: blocking turns off, no notification possible; red card on Dashboard when app is opened |

---

## 14. Checklist for the other phone (Android 13 – 17 recommended)

- [ ] Install v1.2 + all permissions granted (note if "Restricted setting" appeared)
- [ ] Notification permission popup appears (Android 13+)
- [ ] YouTube Shorts: countdown notification → 1-min warning → block at 0:00 → cooldown screen
- [ ] **Audio stops** when block screen appears (v1.2 fix)
- [ ] Long YouTube video plays without blocking
- [ ] Instagram Reels detected and counted
- [ ] Facebook Reels detected and counted
- [ ] Snapchat Spotlight / TikTok (if installed)
- [ ] Daily limit: after 30 min total → "Daily allowance used" screen
- [ ] Hard Focus 30 min: YouTube (long videos too), Facebook, Instagram fully blocked; WhatsApp/Chrome open fine; End/Add buttons greyed out; unblocks by itself after 30 min
- [ ] Strict mode: Go Back needs 5-sec hold + reason
- [ ] Night schedule after custom hours
- [ ] **Pause Before Opening**: enable in Settings → open Instagram/YouTube → "Take a moment" screen appears → wait → "Yes, continue" works
- [ ] **Curious Mode**: enable in Settings → open Shorts → blocks after ~30 sec
- [ ] **Re-intervention**: enable in Settings → stay in an app → prompt reappears after set time
- [ ] **Per-app limit**: set YouTube to 5 min on Apps screen → 5 min in Shorts → "App limit reached" block
- [ ] **Installed apps picker**: Apps screen → "Block Entire Apps" section → search → toggle
- [ ] **Gamification**: Reports screen → Focus Progress section shows level, points, achievements
- [ ] Restart phone → blocking still works after unlock
- [ ] Settings → Detection log: any "uncertain" entries? (send them to me)
- [ ] Phone brand / Android version: ____________

---

## 15. Requirements check (howTo.md + chat + competitor features)

| Requirement | Status |
|---|---|
| React Native (bare, no Expo) + Kotlin engine | ✅ |
| Session 1–5 min, daily 5–30 min, cooldown 5–30 min, midnight reset | ✅ |
| Local only, no backend, sideload (no Play Store) | ✅ |
| Black theme colors from howTo.md, dark + light | ✅ |
| Supports Android 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 | ✅ built · tested on 11 only |
| Works on all brands (standard Android only, no brand-specific code) | ✅ built · tested on Realme only |
| No PIN / login / security | ✅ removed |
| Two modes: Shorts Block + Hard Focus | ✅ |
| Hard Focus 30 / 40 / 50 min, End/Add disabled | ✅ built |
| Notification when blocking stops | ✅ |
| Name NoReels everywhere + new icon | ✅ |
| Audio muted when block screen appears (Issue 1 fix) | ✅ built v1.2 |
| Installed apps picker — block only selected apps (Issue 2) | ✅ built v1.2 |
| Pause before opening — one sec style (competitor feature) | ✅ built v1.2 |
| Curious Mode — 1 reel peek (competitor feature) | ✅ built v1.2 |
| Per-app daily time limits — Opal style (competitor feature) | ✅ built v1.2 |
| Custom scheduled blocking hours (competitor feature) | ✅ built v1.2 |
| Re-intervention after X min (competitor feature) | ✅ built v1.2 |
| Gamification — points, levels, achievements (Forest style) | ✅ built v1.2 |
| Play Store publishing | 🔜 Future — app is prepared (see issue.md → Future) |

**Differences from howTo.md (agreed / on purpose):**
- Ledger uses plain SQLite (not Room) · settings stored in Kotlin (not MMKV)
- Block screen drawn by the Accessibility service → no "Display over other apps" permission needed
- No separate foreground service (Android restarts the Accessibility service itself)
- PIN removed (your decision) · Goals screen not built (extra) · no Chrome/browser blocking (your decision)
