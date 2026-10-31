# Odisej

Block Instagram Reels on Android — keep DMs, feed, stories and posting.

> Status: work in progress (v1 MVP).

*Odisej* is Slovenian for Odysseus. He had himself tied to the mast so he could hear
the Sirens without steering toward them — psychologists call this a *Ulysses contract*.
Odisej does the same for Instagram: you keep the app for messages, but the Reels feed
can't pull you in.

## Why another blocker?

Most Reels/Shorts blockers use Android's Accessibility Service, which can read
**everything on your screen in every app** — including your private messages.
Most of them are closed source and have network access, so you have to trust
their privacy policy.

Odisej is built so you don't have to trust it:

| Guarantee | How you can verify it |
|---|---|
| **Cannot send data off the device** | No `INTERNET` permission. It is explicitly removed with `tools:node="remove"`, and the build fails if any library adds it back (`./gradlew checkNoInternet`). |
| **Only watches Instagram** | `android:packageNames="com.instagram.android"` in the service config, plus a package check in code. |
| **Doesn't read your messages** | Detection uses only view IDs and class names. `getText()` is never called. |
| **Stores nothing about your screen** | The only thing saved is a local block counter. |

## How it works

1. Instagram emits accessibility events when its screen changes.
2. Odisej looks for view IDs that only exist in the Reels viewer.
3. If found, it presses Back and shows a toast. (v2: a blocking overlay.)

All detection rules live in one file (`DetectionRules.java`), because Instagram
changes its view IDs with updates. See [`docs/zaznava.md`](docs/zaznava.md) for how
the IDs were found.

## Install

1. Download the APK from Releases and install it.
2. **Android 13+:** sideloaded apps can't enable accessibility services by default
   ("restricted settings"). Try enabling the service once, dismiss the dialog, then go
   to *Settings → Apps → Odisej → ⋮ → Allow restricted settings*.
3. *Settings → Accessibility → Odisej → On.*

## Limitations

- Instagram updates can break detection until the rules are updated.
- You can always turn the service off in system settings. This is intentional:
  preventing that would require monitoring the Settings app.

## Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Not on Play Store

Google Play restricts Accessibility API use to accessibility tools, so Odisej is
distributed only via GitHub.
