# CLAUDE.md — Odisej

Android aplikacija (Java), ki z `AccessibilityService` zazna Instagram Reels in
uporabnika vrne nazaj. Zasebnost po zasnovi: brez interneta, samo Instagram, brez
branja besedil. Celotna zasnova: **CONTEXT.md** (vir resnice).

## Trenutna faza
**v3 dokončan (F0–F4), ročno preverjen na napravi.** Naslednje: posnetki zaslona v README, push, Release. Stanje sproti v `docs/napredek.md`.

## Zlata pravila (CONTEXT.md §4) — ne krši brez izrecnega dovoljenja
1. Nikoli ne dodaj `INTERNET`, `ACCESS_NETWORK_STATE` ali mrežne/analitične knjižnice.
   V manifestu ostane `<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />`.
2. Servis posluša samo `com.instagram.android` (XML `packageNames` + preverjanje v kodi).
3. Nikoli ne kliči `AccessibilityNodeInfo.getText()`. Zaznava = ID-ji, razredi,
   kvečjemu `contentDescription` navigacijskih elementov.
4. Nič o zaslonu se ne shranjuje in ne logira v release. Debug izpis drevesa samo
   pod `BuildConfig.DEBUG`, samo ID-ji in razredi.
5. Vsebine `dumps/` nikoli ne beri v commit, ne kopiraj v teste neočiščene
   (besedila, imena, sporočila odstrani).
6. Vsa pravila zaznave (ID-ji, vzorci) živijo samo v `detect/DetectionRules.java`.
7. adb ukazi, ki berejo stanje drugih aplikacij ali zaslona (`dumpsys activity top`,
   `dumpsys window`, `uiautomator`, `screencap`, `screenrecord`), samo z Jakobovim
   izrecnim dovoljenjem v tistem trenutku in samo, ko je spredaj Odisej. Brez vprašanja
   sta dovoljena `adb install -r` in `adb logcat` z našimi tagi (`OdisejDump`,
   `OdisejService`). `adb pull` samo za datoteke, ki jih Jakob poimenuje.

## Kako delava (Jakob se uči)
- Pred kodo na kratko razloži **zakaj** (alternativa, kompromis). Nato koda.
- Majhni koraki: en korak → gradnja + testi → commit → zapis v `docs/napredek.md`.
- Ne preskakuj testov, ne delaj bližnjic. Če nekaj ni preverjeno na napravi, to povej.
- Ko ugotoviš kaj novega o Instagramu (ID, obnašanje), dopiši v CONTEXT.md §11/§12.
- Commit sporočila: Conventional Commits v angleščini (`feat:`, `fix:`, `chore:`, `test:`, `docs:`).
- Brez `sudo`. Pred `git push`, `git reset --hard` in brisanjem datotek vprašaj.

## Ukazi
```bash
./gradlew assembleDebug            # gradnja
./gradlew testDebugUnitTest        # enotski testi (detektor)
./gradlew lintDebug                # lint
./gradlew installDebug             # namesti na povezano napravo
./gradlew checkNoInternet          # pade, če je INTERNET v združenem manifestu (del check)

adb devices
adb logcat -s OdisejDump OdisejService           # debug izpis drevesa / servisa
scripts/collect-dumps.sh                       # vodeni dumpi (samo ID-ji) → dumps/*.tree.txt
adb shell settings get secure enabled_accessibility_services   # je servis vklopljen?
```

## Struktura
```
app/src/main/java/si/odisej/
  MainActivity.java                       stanje, števec, premor, nastavitve
  service/{ReelsAccessibilityService, ServiceStatus}.java
  detect/{UiNode, AccessibilityUiNode, DetectionRules, ReelsDetector, ScreenState}.java
  action/{BlockPolicy, BlockAction, BackAction, BlockOverlay, Debouncer}.java
  settings/{Prefs, BlockStats, PauseCountdown}.java
app/src/debug/.../service/TreeDumper.java     debug dumper (OdisejDump); src/release = prazen
app/src/main/res/xml/accessibility_service_config.xml
app/src/test/java/si/odisej/              testi (detektor na drevesih, logika)
app/src/test/resources/trees/             očiščena drevesa (samo ID/razred/meje/zastavice)
docs/      napredek.md, zaznava.md
scripts/   collect-dumps.sh (F1 dumpi), dump-ui.sh (uiautomator, zasebno)
dumps/     zasebni dumpi (gitignore)
```

## Android opombe
- minSdk 26. Build skripte v Kotlin DSL, koda v Javi.
- `TYPE_WINDOW_CONTENT_CHANGED` je zelo pogost → throttle; iskanje po drevesu
  prekini ob prvem zadetku; `AccessibilityNodeInfo` ne hrani dlje kot en klic.
- Overlay (v2) prek `TYPE_ACCESSIBILITY_OVERLAY`, ne `SYSTEM_ALERT_WINDOW`.
- Sideload na Android 13+: "Allow restricted settings" v App info (dokumentirano v README).
