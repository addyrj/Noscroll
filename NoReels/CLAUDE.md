# NoReels — Project Instructions

## What this is
A personal Android app (React Native + Kotlin) that blocks YouTube Shorts,
Instagram Reels, Facebook Reels, TikTok, and Snapchat Spotlight.
Sideloaded only — no Play Store. No backend. All local.

## Rules (never change these without asking me)
- Session limit: 5 min max (user can set 1–5 min)
- Daily limit: 30 min shared across ALL apps (user can set 5–30)
- Cooldown: 15 min between sessions (user can set 5–30)
- Reset: local midnight (not rolling 24hr)
- Leaving the feed pauses a session; it is committed after a full cooldown away.

## Tech stack
- React Native 0.88, bare (no Expo), New Architecture, TypeScript
- Kotlin: AccessibilityService (`ShortBlockerService`) hosts the engine, overlay and notifications
- Ledger + settings: SQLite in Kotlin (`LedgerDatabase`, plain SQLiteOpenHelper). Kotlin owns
  settings too, so the UI cannot bypass limits. No AsyncStorage, no MMKV.
- Bridge: TurboModule. Spec in `src/specs/NativeNoReels.ts`, codegen config in package.json,
  Kotlin in `bridge/NoReelsModule.kt`. Payloads are JSON strings typed in
  `src/modules/NoReelsModule.ts`.
- No backend, no cloud sync, no auth

## Target Android versions
minSdk 26, targetSdk 36, compileSdk 37 (Android 14–17 primary)

## Style rules
- Kotlin: follow Android Kotlin style guide
- React Native: functional components + hooks only, no class components
- No third-party UI libraries — custom components in `src/components`
- All colors from `src/theme/colors.ts` (overlay mirrors the dark palette in `BlockingOverlay.kt`)

## What NOT to do
- Do not add PIN, login, or any security/lock features — the app only blocks short videos
- Do not add a backend unless I ask
- Do not use Expo — bare React Native only
- Do not use AsyncStorage for the ledger
- Do not publish to Play Store

## Key files
- android/app/src/main/java/com/noreels/blocking/ShortBlockerService.kt — main loop
- .../blocking/GuardEngine.kt — timing state machine (session, daily, cooldown, focus)
- .../blocking/DetectionRules.kt — per-app feed detection (update when apps change UI)
- .../blocking/BlockingOverlay.kt — block screen (TYPE_ACCESSIBILITY_OVERLAY)
- .../ledger/LedgerDatabase.kt — SQLite ledger
- src/screens/Dashboard.tsx — main home screen
- src/modules/NoReelsModule.ts — typed bridge
