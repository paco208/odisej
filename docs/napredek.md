# Napredek

Po vsakem koraku: datum, kaj je narejeno, kako preverjeno, kaj ostaja odprto.

## F0 — Postavitev
- [x] Android Studio projekt (`si.odisej`, Java, Kotlin DSL, minSdk 26)
- [ ] Prvi commit + GitHub repozitorij
- [x] INTERNET odstranjen (`tools:node="remove"`) + `checkNoInternet`
- [x] Prazen `ReelsAccessibilityService` + XML konfiguracija + manifest
- [x] `MainActivity`: stanje servisa + gumb v nastavitve dostopnosti
- [x] Debug dumper (`OdisejDump`, samo ID-ji in razredi)
- [ ] Servis vklopljen na napravi, dogodki iz Instagrama vidni v Logcatu

## F1 — Zaznava
- [ ] Dumpi vseh zaslonov (docs/zaznava.md)
- [ ] Izbrani signali zapisani v CONTEXT.md §11
- [ ] `UiNode`, `DetectionRules`, `ReelsDetector` + testi

## F2 — MVP (v1)
- [ ] Throttle + klasifikacija v servisu
- [ ] `BackAction` z debounce + toast
- [ ] Ročni test vseh zaslonov iz CONTEXT.md §6.2
- [ ] Podpisan release APK

---

## Dnevnik
- 2026-10-31 — F0: projekt preimenovan Reelstop → Odisej (dokumentacija, paket `si.odisej`,
  log tagi `OdisejDump`/`OdisejService`, odločitev D11). Preverjeno: `grep -ri reelstop`
  najde le D11. Android projekta še ni, zato gradnja ni bila mogoča.
- 2026-10-31 — F0: zasebnostni manifest, `checkNoInternet`, servis, debug dumper, MainActivity.
  - `checkNoInternet` bere *združeni* manifest prek variant API (`SingleArtifact.MERGED_MANIFEST`),
    ker knjižnice dovoljenja dodajo šele ob merge; v izvornem manifestu jih ne bi videl.
    Preverjeno: brez `tools:node="remove"` naloga pade. Povezana je s `check`.
  - Dumper je v `src/debug/`, v `src/release/` je prazen razred z istim imenom: koda za izpis
    v release APK fizično ne obstaja (močneje kot `if (BuildConfig.DEBUG)`, ki zaupa R8).
  - `allowBackup=false` ne ustavi prenosa med napravami na Android 12+, zato še
    `data_extraction_rules.xml`, ki izključi vse. Odstranjen androidx `ProfileInstallReceiver`
    (izvožen, ne rabimo ga). Servis `exported=false` — *na napravi še ni preverjeno* (D12).
- 2026-10-31 — F1 priprava: `scripts/collect-dumps.sh` + debug-only ukaz za ročni izpis
  (`am broadcast si.odisej.debug.DUMP`, pošlje ga lahko samo shell/sistem — dovoljenje `DUMP`).
  Zakaj: dumper piše ob dogodkih, statičen zaslon po `logcat -c` morda ne pošlje nobenega.
  Brez `uiautomator` ne nastane noben dump z besedili. Preverjeno: release dex nima `OdisejDump`.
