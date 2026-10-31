# Odisej

Block Instagram Reels on Android — keep your DMs, feed, stories and posting.
No internet permission, privacy by design.

> **v1.0.0** · Android 8.0+ · tested on Android 16 with Instagram 453.0.0.57.79

<!-- TODO: screenshots (main screen, moment of pause while breathing, with actions) -->

*Odisej* is Slovenian for Odysseus. He had himself tied to the mast so he could hear the Sirens
without steering toward them — psychologists call this a *Ulysses contract*. Odisej does the same
for Instagram: you keep the app for messages, but the Reels feed can't pull you in.

## What it does

- **Opening Reels shows a moment of pause**: a black screen and one breath (in 1.5 s, out 1.5 s).
  Only then do *Go to messages*, *Go to feed* and *Close Instagram* appear — no reflex taps.
  Alternatively, Odisej can simply go back immediately.
- **Reels sent to you in DMs** still play (optional). Swiping to the next, suggested reel is
  blocked.
- **Pausing** takes a 60-second wait on Odisej's screen, then blocking pauses for 15 minutes.
  Resuming is instant.
- **Counts**, shown on the main screen and as 7 thin bars for the last week:
  - **stopped today** — times Odisej stepped in (moment of pause or going back);
  - **watched · min in Reels** — reels you actually watched because they weren't blocked
    (during a pause, or a reel from a DM), and the time spent.
- A **Quick Settings tile** shows the state (active / paused / off). It can't pause blocking.

## Privacy

Most Reels blockers use the Accessibility API, which can read everything on your screen in every
app, and most of them are closed source with network access. Odisej is built so you don't have
to trust it:

| Guarantee | How you can verify it |
|---|---|
| **Cannot send data off the device** | No `INTERNET` permission — explicitly removed with `tools:node="remove"`; the build fails if any library adds it back (`./gradlew checkNoInternet`, part of `check` and CI). |
| **Only watches Instagram** | `android:packageNames="com.instagram.android"` in the service config, plus a package check in code. |
| **Doesn't read your messages** | Detection uses only view IDs, class names, visibility and the selected state of tabs. `getText()` and `getContentDescription()` are never called. |
| **Stores nothing about your screen** | On disk: settings, the end of a pause, **daily totals for the last 7 days** (stopped, watched, seconds in Reels — dates only, older days deleted on every write) and an all-time total. No timestamps, no screen history, no content. Backups and device transfer are disabled. |
| **No debug logging in release** | The development tree dumper lives in the `debug` source set only; the release APK doesn't contain it. |
| **The APK matches the source** | R8 shrinks the release build but doesn't obfuscate names, so a decompiled APK shows the same classes as this repo. |

While the moment of pause is shown, or while you watch unblocked Reels, Odisej checks once a
second **which app** is in front (package name only), so it notices when you leave Instagram.

## Install

1. Download `odisej-v1.0.0.apk` from [Releases](../../releases) and install it.
2. **Android 13+:** sideloaded apps can't enable accessibility services right away
   ("restricted settings"). Try to turn Odisej on once and dismiss the dialog, then open
   *Settings → Apps → Odisej → ⋮ → Allow restricted settings*.
3. *Settings → Accessibility → Odisej → On.*
4. If blocking stops after a while, your phone may be killing background services: set
   *Settings → Apps → Odisej → Battery → Unrestricted*.

## How it works

At most every 300 ms Odisej looks at Instagram's view tree. A **visible**
`clips_viewer_view_pager` means Reels (Instagram keeps a *hidden* copy of the Reels tab in almost
every screen, so visibility is what matters); a selected `clips_tab` means the Reels tab; a reply
bar next to the reel means it was sent to you in a DM. All rules live in
[`DetectionRules.java`](app/src/main/java/si/odisej/detect/DetectionRules.java); the detector is
plain Java, unit-tested against 15 recorded, text-free screen trees
([how the IDs were found](docs/zaznava.md)).

## Limitations

- **Instagram updates can break detection** until the rules are updated.
- Reels playing **inline in the feed** aren't blocked — only the full-screen viewer and the tab.
- DM exception: swiping to a suggested reel and back allows the original one again.
- Muting the reel behind the pause relies on Instagram respecting audio focus (it does in 453).
- **You can always turn the service off** in Android's accessibility settings. That's
  intentional: preventing it would mean watching the Settings app and locking you out — malware
  patterns. Odisej adds friction for weak moments; it isn't a cage.
- Not on Google Play (its policy limits Accessibility API use to accessibility tools).

## Build

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug checkNoInternet
./gradlew assembleRelease          # R8; signed if the properties below exist, else unsigned
```

Release signing reads `ODISEJ_STORE_FILE`, `ODISEJ_STORE_PASSWORD`, `ODISEJ_KEY_ALIAS` and
`ODISEJ_KEY_PASSWORD` from `~/.gradle/gradle.properties` — never from the repo. Create a key with
`keytool -genkeypair -v -keystore ~/keystores/odisej-release.jks -alias odisej -keyalg RSA -keysize 4096 -validity 10000`.

**When Instagram changes its IDs:** with a debug build and the service on, run
`scripts/collect-dumps.sh` (text-free trees to `dumps/`), update `DetectionRules.java`, add the
trees to `app/src/test/resources/trees/` and run the tests. `TreeFilesPrivacyTest` fails if a
tree contains anything but IDs, classes, bounds and flags.

## Third-party

Bundles the **Adwaita Sans** font (a fork of Inter), [SIL Open Font License 1.1](third_party/adwaita-sans/OFL.txt),
subset and shipped inside the APK — no downloadable fonts.

## License

Copyright (c) 2026 Jakob Ferjančič. All rights reserved.

The source is published for transparency (so the privacy claims can be verified). You may
download and install the released APK for personal use.
