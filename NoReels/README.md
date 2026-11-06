# NoReels

Personal Android app that limits YouTube Shorts, Instagram Reels, Facebook Reels,
Snapchat Spotlight and TikTok: 5 min per session, 30 min per day, 15 min cooldown.

## Run on your phone (development)
1. Enable Developer Options + USB debugging, connect via USB, check `adb devices`.
2. `npm install`
3. `npx react-native run-android`

## Build a standalone APK (no Metro needed)
```
cd android
./gradlew assembleRelease
```
Copy `android/app/build/outputs/apk/release/app-release.apk` to `apk/NoReels-v<version>.apk`.
Install over USB: `adb install -r apk/NoReels-v1.1.apk` (installing by tapping the file is
blocked by Play Protect because the app uses Accessibility).
Without an upload key configured, the release build is signed with the debug key (fine for
sideloading, not for the Play Store).

## First launch
Grant: Accessibility (required), Notifications (required), ignore battery optimization
(recommended), Usage access (optional). On Android 13+, if Accessibility says
"Restricted setting": App info → ⋮ → Allow restricted settings.

## Testing checklist
- YouTube → Shorts: notification countdown appears; at 1:00 a heads-up warning; at 0:00 the block screen.
- Re-open Shorts during cooldown: block screen with countdown; "Blocked attempts" goes up.
- Reboot phone, unlock, open Dashboard: today's total is preserved.
- Settings → Detection log: check for "uncertain" entries after app updates.
