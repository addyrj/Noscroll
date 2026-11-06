# Issues — NoReels

Open problems only. Fixed / answered items are removed.

---

## Fixed in v1.2

1. ~~Sound keeps playing when blocked~~ → **Fixed**: audio focus grabbed, app pauses playback.
2. ~~Blocks wrong apps (banking etc.)~~ → **Fixed**: installed-apps picker — you choose which apps to block.
3. ~~Auto-detect social media~~ → **Fixed**: known social media apps shown first, plus searchable installed-apps list.
4. ~~Competitor features (Forest, NoScroll, one sec, Opal)~~ → **Added**: pause before opening, curious mode, per-app limits, re-intervention, custom schedule, gamification.

---

## How to install (until it is on the Play Store)
Play Protect blocks installing the APK file by tapping it ("App blocked to protect your device"),
because the app uses Accessibility. This cannot be removed with code. Use one of:
- **A) USB:** `adb install C:\NoScroll\NoReels\apk\NoReels-v1.2.apk` (takes ~5–7 min while
  Play Protect scans it — wait, don't unplug)
- **B) No computer:** Play Store → profile picture → Play Protect → ⚙️ → turn off
  "Scan apps with Play Protect" → install the APK → turn it back on.

## Known Android limit (cannot be fixed by any app)
- **Force stop** (Settings → Apps → NoReels → Force stop) turns blocking OFF, and Android then
  blocks ALL of the app's code — so no notification can appear, not even after a restart.
  Open the app and switch Accessibility on again (the red "Blocking is OFF" card shows how).

---

## Future — Play Store (you will do later)
Already ready in the app: Accessibility disclosure popup, `isAccessibilityTool` declaration,
no restricted battery permission, targetSdk 36, privacy policy text in
`C:\NoScroll\NoReels\docs\privacy-policy.md`.

Answer these when you start publishing:
1. **Package name** (permanent): keep `com.noreels`, or `com.yourname.noreels`?
2. **Upload key**: I create it, you back it up.
3. **Google Play Developer account** ($25). New personal accounts need a closed test with
   12 testers for 14 days before going public.
4. **Privacy policy**: public web link (Google Sites / GitHub Pages) + contact email.

Play Console will also ask for: an Accessibility declaration + short screen-recording video,
the Data safety form ("no data collected or shared"), content rating, screenshots, and a
feature graphic. I can prepare the texts when you start.
