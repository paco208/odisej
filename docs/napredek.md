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
- [x] Popravki po prvem ogledu (insets, N, pilule, stikala, ločila)
- [x] Zapri Instagram, 7-dnevni stolpci, QS ploščica, pulz na vrhu vdiha
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
- 2026-10-31 — UI popravki po prvem ogledu na telefonu:
  - N na overlayu (29) ≠ glavni zaslon (30): ni bil vrstni red posnetkov, ampak dvojno štetje — po
    tapu "Na feed" je bil viewer med animacijo prehoda še viden in overlay se je za hip pokazal
    znova. Ohlajanje 1,5 s po odhodu (D31) + `OverlayCountTest`, ki posname to zaporedje.
  - Sivi gumbi z velikimi črkami: v servisu `<Button>` ne postane `MaterialButton` (D32).
  - Spodnja vrstica pod navigacijo: `clipToPadding="false"` je risal vsebino pod vrstico; zdaj
    `true` + izrez kamere. Overlay ima svojo obravnavo insetov (okno sega pod vrstice).
  - Pilule, poudarno stikalo, ločilne črte, "Zapri Instagram", pulz na vrhu vdiha (haptika
    spoštuje sistemsko nastavitev vibracij, ker ne uporabljamo `FLAG_IGNORE_GLOBAL_SETTING`).
- 2026-10-31 — 7-dnevni stolpci. Hrani se samo število na dan za zadnjih 7 dni (Z4 posodobljen).
  Test je ujel past: obrezovanje "na okno [danes−6, danes]" bi ob premiku ure nazaj pobrisalo
  današnje štetje, zato brišemo samo starejše dni. Stari števec se prenese.
- 2026-10-31 — QS ploščica: samo stanje + odpre aplikacijo. Premor iz ploščice bi obšel strogi
  način, zato ga namenoma ni. `exported=true`, ker SystemUI ni sistemski uid (prim. D12).
- 2026-10-31 — "Zapri Instagram" na dva tapa. Ohlajanje se je sicer začelo, a servis je najprej
  poklical `keepIfShowing()`, ki je izginjajoči overlay (fade-out 150 ms) oživil, preden je prišlo
  ohlajanje na vrsto. Odločitev je zdaj čista funkcija `OverlayStep.decide`, ki jo uporabljata
  servis in test — test ne posnema servisa na pamet. Mutacija (stari vrstni red) podre 2 testa.
- 2026-10-31 — Overlay na novo postavljen (D36). Izračun za Jakobov zaslon (1220×2712 @450 =
  434×964dp, trije gumbi): krog središče 386dp od vrha (okvir 274–498), stavek od 530dp, pilula
  784–832dp, siva vrstica s spodnjim robom besedila 48dp nad navigacijo. Na 1080×2340 @420
  (411×891dp) ostane med stavkom in pilulo 166dp. *Na napravi še ni preverjeno.*
  Kompromis: mirujoči krog (Ø90dp) je ~99dp nad stavkom, ker je prostor rezerviran za razširjen krog.
- 2026-10-31 — Pogledani reeli in čas v Reelsih (D37). Signal za nov reel iz dumpov ni preverljiv
  (dumpi so stanje, ne dogodki), zato: indeks strani iz `TYPE_VIEW_SCROLLED` na pagerju + rezerva
  "poteg umirjen". Debug log `OdisejService` izpiše `toIndex`, da na napravi vidimo, katera pot
  dela. Če se ročni test ne ujema, števec v UI označim kot oceno (~). Shramba: ista `DailyCounts`
  (že testirana, isto 7-dnevno brisanje), dva nova niza. Vrstica se skrije, ko je oboje 0 —
  prazen prostor je tu bolj pomenljiv kot "danes še nič pogledano". Na napravi še ni preverjeno.
- 2026-10-31 — Overlay akcije tretjič (D38): vrstice namesto pilul, en blok namesto akcij na dnu
  (prej ~210dp praznine). Po dihanju se ne premakne samo blok, ampak se skrčita tudi okvir kroga
  (224 → 96dp) in prostor za besedilo — sicer bi med mirujočim krogom (Ø90) in stavkom ostalo
  ~100dp praznine. Položaj računa čista funkcija `OverlayLayout.settledTop` (testi tudi za majhen
  zaslon, kjer ima 48dp do navigacije prednost). Moj prvi testni primer za to ni nič preverjal
  (pri 45 % centriranje ni prekoračilo meje) — popravljen z izračunanim primerom.
- 2026-10-31 — Oznake števcev: velika številka je zdaj "ustavljenih danes" — kolikokrat je Odisej
  posredoval (trenutek premora ali nazaj). Siva vrstica "pogledanih N · M min v Reelsih" so reeli,
  ki si jih res gledal, ker niso bili blokirani (premor ali reel iz DM). 40 in 17 sta torej dve
  različni stvari: 40-krat ustavljen, 17 reelov pogledanih med premorom/iz DM.
- 2026-10-31 — Spodnja vrstica odrezana, drugič. Tokrat izmerjeno na napravi z `dumpsys activity
  top` (samo naša aktivnost): ScrollView 0–2712px, vsebina 125–2858px. Zgornji inset je torej
  deloval; vsebina (972dp) je bila 100dp višja od vidnega dela (872dp), odkar so prišli 7-dnevni
  stolpci, in vrstica "Brez interneta …" je bila na koncu drsnega seznama. Prvi popravek
  (`clipToPadding`) je zdravil napačno diagnozo ("riše se pod navigacijo"): spremenil je le to, da
  se vrstica ne riše več POD navigacijo, ampak se ODREŽE na njenem robu — videti enako. Lekcija:
  najprej izmeri, potem popravljaj. Rešitev: vrstica je pripeta pod drsno vsebino, inseti so na
  korenu; padding samo na drsni vsebini bi pomagal le pri drsenju do konca.
  Po popravku na napravi še ni izmerjeno (telefon je bil v uporabi).
