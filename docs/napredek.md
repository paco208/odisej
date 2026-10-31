# Napredek

Po vsakem koraku: datum, kaj je narejeno, kako preverjeno, kaj ostaja odprto.

## F0 — Postavitev
- [x] Android Studio projekt (`si.odisej`, Java, Kotlin DSL, minSdk 26)
- [ ] Prvi commit + GitHub repozitorij
- [x] INTERNET odstranjen (`tools:node="remove"`) + `checkNoInternet`
- [x] Prazen `ReelsAccessibilityService` + XML konfiguracija + manifest
- [x] `MainActivity`: stanje servisa + gumb v nastavitve dostopnosti
- [x] Debug dumper (`OdisejDump`, samo ID-ji in razredi)
- [x] Servis vklopljen na napravi, dogodki iz Instagrama vidni v Logcatu

## F1 — Zaznava
- [x] Dumpi vseh zaslonov (docs/zaznava.md)
- [x] Izbrani signali zapisani v CONTEXT.md §11
- [x] `UiNode`, `DetectionRules`, `ReelsDetector` + testi

## F2 — MVP (v1)
- [x] Throttle + klasifikacija v servisu
- [x] `BackAction` z debounce + toast
- [x] Ročni test vseh zaslonov iz CONTEXT.md §6.2
- [x] Podpisan release APK

## F3 — v2
- [x] Overlay (`TYPE_ACCESSIBILITY_OVERLAY`) z gumboma Sporočila / Feed
- [x] Izjema za reel iz DM (strukturni signal)
- [x] Nastavitev: overlay / nazaj
- [x] Ročni test overlaya na napravi

## F4 — v3
- [x] Števec blokad (danes / skupaj)
- [x] Strogi način (60 s odštevanja za premor)
- [x] Release podpisovanje iz ~/.gradle/gradle.properties
- [x] R8 (statično preverjeno v dex/manifestu)
- [x] Servis po minifikaciji preverjen na napravi (release build)
- [x] GitHub Actions (napisano; teče šele po pushu — ni preverjeno)
- [x] README (posnetki zaslona: TODO, naredi Jakob)


## UI prenova (trenutek premora)
- [x] Temna tema, pisava, slogi, ikona
- [x] Overlay z dihanjem in zakasnjenimi gumbi
- [x] Glavni zaslon (števec, status, stikali, krožno odštevanje)
- [ ] Ročni test na napravi

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
- 2026-10-31 — F0 na napravi (moto g86, Android 16): servis z `exported=false` se pojavi in
  vklopi, "Allow restricted settings" ni bil potreben (namestitev prek adb). D12 potrjen.
- 2026-10-31 — F1: 15 dumpov, detektor + 28 testov. Glavna lekcija: `clips_viewer_view_pager`
  je v drevesu skoraj povsod (skrit fragment zavihka), zato šteje samo **viden** — brez tega bi
  blokirali feed in DM-je. Mutacijski test: brez preverjanja vidnosti pade 11 testov; vrstica z
  besedilom v drevesu podre `TreeFilesPrivacyTest`. DM izjemo loči vrstica za odgovor.
- 2026-10-31 — F2: servis klasificira in ukrepa. Throttle je *trailing* (zadnji dogodek v oknu
  se vedno obdela), ker *leading* throttle lahko izpusti končno stanje zaslona. Z zavihka Reels
  klik na Home namesto BACK (D19). Na napravi še ni preverjeno.
- 2026-10-31 — F3: overlay + DM izjema + nastavitev načina. Past: servis zaradi Z2 ne izve, da
  je uporabnik zapustil Instagram, zato bi overlay ostal čez zaganjalnik → med prikazom preverjamo
  samo ime paketa aktivnega okna (D21). DM izjema brez zgodovine zaslonov, ker jo loči že drevo.
- 2026-10-31 — F4: števec + strogi premor. SharedPreferences namesto DataStore (D24). Odštevanje
  se prekine ob `onPause`: trenje mora trajati, dokler impulz traja; nadaljevanje je takojšnje,
  ker trenje velja samo v smeri šibkosti.
- 2026-10-31 — F4: podpis + R8. Skelet je optimiziral samo androidx (`packageScope`); zdaj vso
  kodo, a brez zakrivanja imen, ker je preverljivost zgodba projekta (D27). Preverjeno z
  `dexdump`/`aapt2`: servis, MainActivity in konfiguracija servisa ostanejo, nizi dumperja ne.
  `apksigner verify`: podpis v2 OK; brez lastnosti nastane `app-release-unsigned.apk`.
- 2026-10-31 — CI: isti ukazi kot lokalno. JDK 25, ker ga zahteva `gradle-daemon-jvm.properties`.
  Release se v CI ne gradi, ker ključa tam ni (in ga ne sme biti). Poročila se naložijo vedno,
  tudi ob napaki, da vidiš, kateri test/lint je padel.
- 2026-10-31 — README: kako deluje, zasebnost, strogi način s pošteno opombo o sistemskem
  izklopu, namestitev, omejitve, gradnja, postopek ob posodobitvi Instagrama.
- 2026-10-31 — Ročni test na napravi (moto g86, Android 16, Instagram 453.0.0.57.79, **release
  build z R8**): vseh 12 primerov uspešnih — overlay (oba gumba, umik ob Home), DM izjema in njen
  izklop, način "nazaj" brez zanke, števec, strogi premor s ponastavitvijo ob odhodu. Audio focus
  deluje: zvok reela za overlayem se ustavi. S tem je potrjeno tudi, da R8 servisa ne pokvari.
- 2026-10-31 — Licenca: vse pravice pridržane, razdelek v README, brez datoteke LICENSE.
- 2026-10-31 — UI prenova. Inter ni bil nameščen; Adwaita Sans je njegov fork (OFL), zato brez
  prenosa. Statične instance namesto variabilne pisave, ker je izbira osi prek XML na API 26–27
  nezanesljiva; okrnitev na latinico (164 KB). Gumbi na overlayu pridejo po uri (`Handler`), ne ob
  koncu animatorja, ker se brez animacij animator konča takoj — pavza mora ostati. Krog
  odštevanja riše po uri (Choreographer), ker je kazalnik napredka, ne okras. Slovenske dvojinske
  oblike sekund prek `<plurals>`. Na napravi še ni preverjeno.
