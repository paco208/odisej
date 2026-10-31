# Odisej

Block Instagram Reels on Android — keep DMs, feed, stories and posting.

> Status: v3 feature-complete and tested on a device (Instagram 453.0.0.57.79, Android 16).

*Odisej* is Slovenian for Odysseus. He had himself tied to the mast so he could hear
the Sirens without steering toward them — psychologists call this a *Ulysses contract*.
Odisej does the same for Instagram: you keep the app for messages, but the Reels feed
can't pull you in.

<!-- TODO: screenshots (main screen, moment of pause). No personal data on them! -->

## Why another blocker?

Most Reels/Shorts blockers use Android's Accessibility Service, which can read
**everything on your screen in every app** — including your private messages.
Most of them are closed source and have network access, so you have to trust
their privacy policy.

Odisej is built so you don't have to trust it:

| Guarantee | How you can verify it |
|---|---|
| **Cannot send data off the device** | No `INTERNET` permission. It is explicitly removed with `tools:node="remove"`, and the build fails if any library adds it back (`./gradlew checkNoInternet`, part of `check` and CI). |
| **Only watches Instagram** | `android:packageNames="com.instagram.android"` in the service config, plus a package check in code. |
| **Doesn't read your messages** | Detection uses only view IDs, class names, visibility and the "selected" state of tabs. `getText()` and `getContentDescription()` are never called. |
| **Stores nothing about your screen** | On disk: your settings, the end time of a pause, **daily totals for the last 7 days** — blocks, reels you watched while not blocked, and seconds spent in unblocked Reels (dates only, no times; older days are deleted on every write) — and the all-time block total. No timestamps of individual reels or sessions, no screen history, no screen content. Backups and device-to-device transfer are disabled. |
| **No hidden debug logging** | The tree dumper used during development lives in the `debug` source set only. The release APK doesn't contain it. |
| **The APK matches the source** | R8 shrinks the release build but does not obfuscate names, so a decompiled APK shows the same class names as this repo. |

## How it works

1. Instagram emits accessibility events when its screen changes.
2. At most every 300 ms, Odisej looks at Instagram's view tree:
   - a **visible** `clips_viewer_view_pager` → you're in Reels
     (Instagram keeps a *hidden* copy of the Reels tab in almost every screen, so visibility is
     what matters),
   - a selected `clips_tab` → the Reels tab,
   - a reply bar next to the reel → a reel someone sent you in a DM.
3. It then either shows a **moment of pause** (default) — a black screen with one breath
   (in 1.5 s, out 1.5 s), after which a single sentence and the buttons *Go to messages* /
   *Go to feed* appear — or immediately **goes back** (Reels tab → Home tab, viewer → Back).
   The buttons are hidden during the breath on purpose, so you can't tap them by reflex. With
   system animations turned off, the circle stays still but the 3-second pause remains.
4. **Reels sent to you in DMs** can be watched (optional, on by default). Swiping to the next,
   suggested reel is blocked.
5. When Reels are **not** blocked (during a pause, or a reel from a DM), Odisej counts the reels
   you watched and the time spent, shown as one line under today's number.

   The main screen shows two different counts:
   - **stopped today** (the big number) — times Odisej stepped in: the moment of pause was shown
     or it went back for you;
   - **watched** (the grey line) — reels you actually watched because they weren't blocked:
     during a pause, or a reel someone sent you in a DM. Together with the minutes spent. It counts a new reel
   when the Reels pager reports a new page index (scroll event; only the view ID and the index
   are read). Time stops when you leave Reels, turn the screen off or leave Instagram. No usage
   access permission is needed.

All detection rules live in one file
([`DetectionRules.java`](app/src/main/java/si/odisej/detect/DetectionRules.java)), because
Instagram changes its view IDs with updates. The detector is plain Java and is unit-tested
against 15 recorded (text-free) screen trees. See [`docs/zaznava.md`](docs/zaznava.md) for how
the IDs were found.

## Strict mode

Pausing the blocker takes a **60-second wait on Odisej's screen**; leaving the screen restarts
the wait. Then blocking pauses for 15 minutes. Resuming is instant.

To be honest about it: **you can always turn the service off in Android's accessibility
settings**, instantly. Odisej doesn't try to prevent that — doing so would require watching the
Settings app (breaking the "only Instagram" guarantee), and hiding from or locking the user out
are malware patterns. Odisej adds friction for your weak moments; it isn't a cage.

## Quick Settings tile

Add the **Odisej** tile to Quick Settings to see the state at a glance (active / paused until
14:32 / off). Tapping it opens the app; it can't pause blocking — that still takes the 60-second
wait in the app.

## Install

1. Download the APK from Releases and install it.
2. **Android 13+:** sideloaded apps can't enable accessibility services by default
   ("restricted settings"). Try enabling the service once, dismiss the dialog, then go
   to *Settings → Apps → Odisej → ⋮ → Allow restricted settings*.
   (When installed from Android Studio / `adb install`, this restriction didn't appear on
   Android 16.)
3. *Settings → Accessibility → Odisej → On.*
4. Some manufacturers kill background services to save battery. If blocking stops after a
   while, exclude Odisej from battery optimisation (*Settings → Apps → Odisej → Battery →
   Unrestricted*).

## Limitations

- **Instagram updates can break detection** until the rules are updated. Detection was
  measured on Instagram 453.0.0.57.79 (Android 16, Slovenian UI).
- Reels playing **inline in the feed** are not blocked; only the full-screen viewer and the
  Reels tab are.
- The DM exception allows the reel that was sent to you. If you swipe to a suggested reel and
  back, the original one is allowed again.
- While the overlay is visible, Odisej checks once a second **which app** is in front (package
  name only), so the overlay disappears if you leave Instagram.
- Odisej asks Android for audio focus while the overlay is shown so the reel behind it goes
  quiet (Instagram 453 pauses; future versions might not).
- You can always turn the service off in system settings (see *Strict mode*).

## Build

```bash
./gradlew assembleDebug            # debug APK (includes the tree dumper)
./gradlew testDebugUnitTest        # detector + logic tests
./gradlew lintDebug
./gradlew checkNoInternet          # fails if INTERNET/ACCESS_NETWORK_STATE is in the merged manifest
./gradlew assembleRelease          # R8; signed if the properties below exist, otherwise unsigned
```

Release signing reads these from `~/.gradle/gradle.properties` (never from the repo):

```properties
ODISEJ_STORE_FILE=/path/to/odisej-release.jks
ODISEJ_STORE_PASSWORD=...
ODISEJ_KEY_ALIAS=odisej
ODISEJ_KEY_PASSWORD=...
```

Create the keystore with:

```bash
keytool -genkeypair -v -keystore ~/keystores/odisej-release.jks -alias odisej \
  -keyalg RSA -keysize 4096 -validity 10000
```

### When Instagram changes its IDs

With a debug build installed and the service on, run `scripts/collect-dumps.sh`. It walks you
through the screens and saves text-free trees (IDs, classes, bounds, flags) to `dumps/`. Compare
them, update `DetectionRules.java`, copy the new trees to `app/src/test/resources/trees/` and run
the tests. `TreeFilesPrivacyTest` fails if a tree line contains anything but the allowed format.

## Third-party

The app bundles the **Adwaita Sans** font (a fork of Inter), licensed under the
[SIL Open Font License 1.1](third_party/adwaita-sans/OFL.txt). It is subset and shipped inside
the APK — no downloadable fonts, no network. Details in
[`third_party/adwaita-sans/`](third_party/adwaita-sans/README.md).

## Not on Play Store

Google Play restricts Accessibility API use to accessibility tools, so Odisej is
distributed only via GitHub.

## License

Copyright (c) 2026 Jakob Ferjančič. All rights reserved.

The source is published for transparency (so the privacy claims can be verified). You may
download and install the released APK for personal use.
