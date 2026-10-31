# Napredek

Po vsakem koraku: datum, kaj je narejeno, kako preverjeno, kaj ostaja odprto.

## F0 — Postavitev
- [ ] Android Studio projekt (`si.odisej`, Java, Kotlin DSL, minSdk 26)
- [ ] Prvi commit + GitHub repozitorij
- [ ] INTERNET odstranjen (`tools:node="remove"`) + `checkNoInternet`
- [ ] Prazen `ReelsAccessibilityService` + XML konfiguracija + manifest
- [ ] `MainActivity`: stanje servisa + gumb v nastavitve dostopnosti
- [ ] Debug dumper (`OdisejDump`, samo ID-ji in razredi)
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
<!-- 2026-11-01 — F0: ... -->
- 2026-10-31 — F0: projekt preimenovan Reelstop → Odisej (dokumentacija, paket `si.odisej`,
  log tagi `OdisejDump`/`OdisejService`, odločitev D11). Preverjeno: `grep -ri reelstop`
  najde le D11. Android projekta še ni, zato gradnja ni bila mogoča.
