# Odisej — projektni kontekst

> Ime: **Odisej**. Paket: `si.odisej`. Jezik: Java. Build: Gradle (Kotlin DSL).
> Ta dokument je vir resnice za zasnovo. Ko se odločitev spremeni, jo popravi tukaj
> in jo dopiši v poglavje 11 (Odločitve).

---

## 1. Povzetek

Android aplikacija z `AccessibilityService`, ki zazna, da je uporabnik v Instagram
Reelsih, in ga vrne nazaj (v1) oziroma pokaže blokado (v2). DM-ji, feed, storyji in
objavljanje delujejo normalno. Glavna zgodba projekta je **zasebnost po zasnovi**:
aplikacija tehnično ne more poslati podatkov z naprave, ker nima dovoljenja za internet.

Avtor: Jakob (dijak, 4. letnik tehnik računalništva). Osebni projekt + GitHub portfelj.
Časovni okvir MVP: ~2 vikenda. Ni objave na Play Store (pravilnik za Accessibility),
samo GitHub + APK.

## 2. Problem in motivacija

- Instagram je potreben za DM-je, Reelsi pa pojedo največ časa.
- Vgrajene časovne omejitve (npr. 30 min) se obidejo z enim klikom.
- Obstoječe aplikacije (AppBlock, one sec, ScrollGuard …) to znajo, a prek Accessibility
  Service vidijo **ves zaslon vseh aplikacij** (tudi DM-je) in so večinoma zaprtokodne,
  z dovoljenjem za internet. Zaupati moraš njihovi politiki zasebnosti.
- Odisej ne zahteva zaupanja: koda je javna, dovoljenja interneta ni, servis posluša
  samo Instagram.

## 3. Cilji in ne-cilji

**Cilji**
- C1: Zanesljivo zaznati Reels viewer in Reels zavihek v Instagramu.
- C2: Uporabnika odstraniti iz Reelsov (v1: nazaj, v2: overlay).
- C3: Ne motiti DM-jev, feeda, storyjev, objavljanja.
- C4: Zasebnost, ki jo lahko vsak preveri (manifest, koda, README).
- C5: Pravila za zaznavo na enem mestu, ker Instagram ob posodobitvah menja ID-je.

**Ne-cilji**
- Blokiranje drugih aplikacij (TikTok, YouTube Shorts) — morda kasneje, ne v MVP.
- Statistika uporabe, časovne kvote, oblak, računi, sinhronizacija.
- Objava na Play Store.
- Preprečevanje izklopa servisa v sistemskih nastavitvah (glej 8.3).

## 4. Zasebnost po zasnovi (pravila Z1–Z7)

Ta pravila so **nepogajljiva**. Če naloga zahteva kršitev, se ustavi in vprašaj.

- **Z1 — Brez interneta.** V manifestu ni `android.permission.INTERNET`. Ker lahko
  knjižnice dovoljenje dodajo prek *manifest merge*, ga eksplicitno odstranimo:
  ```xml
  <uses-permission android:name="android.permission.INTERNET" tools:node="remove" />
  ```
  in dodamo preverjanje (Gradle naloga ali test), ki pade, če se `INTERNET` pojavi v
  **združenem** manifestu (`build/intermediates/merged_manifests/...`). Prav tako
  ni `ACCESS_NETWORK_STATE`. Brez mrežnih knjižnic (OkHttp, Retrofit, Firebase,
  Crashlytics, analitika).
- **Z2 — Samo Instagram.** `android:packageNames="com.instagram.android"` v XML
  konfiguraciji servisa. Poleg tega servis v `onAccessibilityEvent` preveri
  `event.getPackageName()` in ignorira vse ostalo (obramba v globino).
  *Za preverit:* ali `getRootInActiveWindow()` lahko vrne okno druge aplikacije, ko
  pride dogodek iz Instagrama (npr. prekrivno okno). Servis drevo bere **samo**, če je
  paket korena `com.instagram.android`.
- **Z3 — Ne beremo besedila.** Zaznava uporablja samo `getViewIdResourceName()`,
  `getClassName()` in po potrebi `getContentDescription()` **navigacijskih** elementov
  (npr. zavihek). Nikoli `getText()`. Opisi vsebine (napisi pod reeli, sporočila) se ne
  logirajo in ne shranjujejo.
- **Z4 — Nič ne shranjujemo o zaslonu.** Na disku so samo: nastavitve, konec premora,
  **število blokad na dan za zadnjih 7 dni** (dan = datum, brez ure; starejši dnevi se ob vsakem
  zapisu pobrišejo) in skupno število blokad. Brez ur, brez zgodovine zaslonov, brez vsebine.
- **Z5 — Brez logiranja v release.** Diagnostični izpis drevesa (glej 6.4) obstaja
  samo v `debug` različici (`BuildConfig.DEBUG`) in izpiše **samo** ID-je in imena
  razredov, nikoli besedila.
- **Z6 — `android:allowBackup="false"`** in brez izvoznih komponent, ki jih ne
  potrebujemo (`exported="false"`, kjer gre).
- **Z7 — Dumpi so zasebni.** Datoteke iz `uiautomator dump` vsebujejo DM-je in imena.
  Shranjujejo se v `dumps/`, ki je v `.gitignore`. Nikoli v commit, nikoli v issue.

## 5. Arhitektura

```
si.odisej
├── MainActivity                 stanje servisa + navodila za vklop + gumb v nastavitve
├── service/
│   └── ReelsAccessibilityService   prejme dogodke, pokliče detektor, sproži ukrep
├── detect/
│   ├── UiNode                   vmesnik: id, className, contentDescription, otroci
│   ├── AccessibilityUiNode      ovoj okoli AccessibilityNodeInfo (Android)
│   ├── DetectionRules           VSA pravila (ID-ji, vzorci) na enem mestu
│   └── ReelsDetector            čista Java: UiNode → ScreenState (REELS_VIEWER, REELS_TAB, OTHER)
└── action/
    └── BackAction               GLOBAL_ACTION_BACK + debounce + toast
```

Zakaj vmesnik `UiNode`: `AccessibilityNodeInfo` se ne da enostavno ustvariti v JVM
enotskih testih. Če detektor dela nad lastnim vmesnikom, ga testiramo s ponarejenimi
drevesi (zgrajenimi iz posnetih dumpov, **očiščenih** besedil) brez telefona.

Tok:
```
Instagram dogodek → service (filter paketa, throttle) → root → AccessibilityUiNode
  → ReelsDetector.classify() → če REELS_* in ni v debounce → BackAction
```

### 5.1 Konfiguracija servisa (`res/xml/accessibility_service_config.xml`)

```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagIncludeNotImportantViews"
    android:canRetrieveWindowContent="true"
    android:packageNames="com.instagram.android"
    android:notificationTimeout="100"
    android:isAccessibilityTool="false"
    android:description="@string/accessibility_description" />
```
- `flagReportViewIds` — brez njega `getViewIdResourceName()` vrača `null`.
- `flagIncludeNotImportantViews` — *za preverit*, ali je potreben; brez njega so
  nekateri vsebniki skriti. Če zaznava dela brez njega, ga odstrani (manj podatkov).
- `typeWindowContentChanged` je zelo pogost (video, animacije) → throttle v servisu
  (npr. klasifikacija največ vsakih 300 ms).
- `android:description` naj pošteno pove, kaj servis vidi in česa ne pošilja.

## 6. Zaznava Reelsov

### 6.1 Hipoteza
Instagram interno Reelse imenuje **"clips"**. Resource ID-ji v Reels viewerju bodo
verjetno oblike `com.instagram.android:id/clips_...`. To je **hipoteza**, preveriti jo
moramo na Jakobovem telefonu z dejansko verzijo Instagrama. Instagram je lahko v
slovenščini, zato se na `contentDescription` ("Reels", "Posnetki Reels" …) ne
zanašamo kot na glavni signal.

### 6.2 Kateri zasloni nas zanimajo
| Zaslon | Blokiraj? | Opomba |
|---|---|---|
| Reels zavihek (spodnja vrstica) | da | glavni vir izgubljenega časa |
| Reels viewer iz feeda / Explore | da | pogosto isti viewer |
| Reel, poslan v DM | v1 da, v2 izjema | izjema samo, če jo zanesljivo zaznamo |
| Feed, storyji, DM-ji, profil, objavljanje | ne | ne smemo motiti |
| Reel kot objava v feedu (predvajanje v feedu) | ne v v1 | zaznamo le, če se odpre polni viewer |

### 6.3 Postopek (podrobno v `docs/zaznava.md`)
1. Na vsakem zaslonu iz tabele 6.2 posnemi dump (`scripts/dump-ui.sh <ime>`).
2. Skripta izpiše unikatne `resource-id`; primerjaj sezname med zasloni.
3. Poišči ID-je, ki so **prisotni v Reels** in **odsotni drugje** (zlasti DM, story).
4. Izberi 1–2 močna signala + rezervni signal. Zapiši v `DetectionRules` in v 11.

### 6.4 Če `uiautomator dump` ne deluje
Na zaslonih z videom pogosto vrne `ERROR: could not get idle state`, ker čaka, da se
zaslon umiri. Rešitve po vrsti:
1. Ustavi video (tap) in poskusi znova; poskusi `--compressed`.
2. **Debug dumper v servisu**: v debug različici servis na dogodek izpiše drevo
   (samo ID-je in razrede, zamik po globini) v Logcat s tagom `OdisejDump`.
   To je celo bolj natančno, ker vidiš točno to, kar vidi servis (z istimi zastavicami).
   `adb logcat -s OdisejDump > dumps/<ime>.txt`
3. Layout Inspector v Android Studiu ne deluje za tuje release aplikacije (ni
   debuggable) — ne izgubljaj časa z njim.

## 7. Ukrep

### 7.1 v1 — nazaj
- `performGlobalAction(GLOBAL_ACTION_BACK)` + `Toast` ("Reelsi so blokirani").
- **Debounce**: po ukrepu ignoriraj zaznave X ms (začetek 1500 ms), sicer zanka
  (nazaj → dogodek → še vedno Reels v drevesu med animacijo → nazaj → izhod iz app).
- *Za preverit:* kam pelje "nazaj" iz Reels **zavihka** (domov? zapre Instagram?).
  Če zapre Instagram, je to sprejemljivo za v1, a overlay v v2 postane pomembnejši.
  Alternativa: klik na zavihek Home (`performAction(ACTION_CLICK)` na node zavihka).

### 7.2 v2 — overlay
- `WindowManager` + `TYPE_ACCESSIBILITY_OVERLAY` (ne potrebuje `SYSTEM_ALERT_WINDOW`).
- Polni zaslon s sporočilom in gumbom "Nazaj na DM-je" / "Zapri".
- Overlay se odstrani, ko zaznava ni več REELS_*.

### 7.3 v2 — izjema za reele iz DM
Ideja: če je bil tik pred Reels viewerjem prikazan DM pogovor (ID-ji `direct_...`),
dovoli ta en reel; pri swipe-u na naslednjega blokiraj. Izvedljivost odvisna od dumpov.
Če ni zanesljivo → izpusti in zapiši v README kot znano omejitev.

## 8. Faze

### F0 — Postavitev (pol dneva)
- Android Studio projekt (Empty Views Activity, Java, Kotlin DSL, minSdk 26).
- Z1: `tools:node="remove"` za INTERNET + preverjanje združenega manifesta.
- Prazen `ReelsAccessibilityService` + XML konfiguracija + vnos v manifest
  (`android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"` → nanj se
  lahko veže samo sistem; vrednost `exported` uskladi z uradnim primerom v
  dokumentaciji za trenutni targetSdk in odločitev zapiši v 11).
- `MainActivity`: prikaže, ali je servis vklopljen; gumb odpre
  `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
- Debug dumper (6.4 točka 2).

### F1 — Zaznava (1 dan)
- Dumpi vseh zaslonov iz 6.2, analiza, izbira signalov.
- `UiNode`, `DetectionRules`, `ReelsDetector` + enotski testi na očiščenih drevesih.

### F2 — MVP (1 dan)  → **v1**
- Servis: throttle, klasifikacija, `BackAction` z debounce, toast.
- Ročni test: vsi zasloni iz 6.2 (zapis v `docs/napredek.md`).
- Release APK (podpisan z lastnim ključem, ključ **ni** v repozitoriju).

### F3 — Overlay + DM izjema → **v2**

### F4 — Števec, strogi način, README → **v3**
- Števec blokad v DataStore (Preferences) ali SharedPreferences — Room je za en
  števec preveč.
- **Strogi način**: v aplikaciji gumb "Začasno izklopi" zahteva 60 s čakanja.
  Pošteno v README: servis lahko vedno izklopiš v sistemskih nastavitvah; tega ne
  preprečujemo, ker bi to zahtevalo spremljanje aplikacije Nastavitve (krši Z2).
- README s posnetki zaslona (brez osebnih podatkov!).

### 8.3 Kar namerno ne delamo
Preprečevanje izklopa servisa, skrivanje ikone, device admin — to so vzorci
zlonamerne programske opreme in v nasprotju z zgodbo projekta.

## 9. Okolje in testiranje

- Fedora, Android Studio (`/opt/android-studio`), projekti v `~/Dokumenti/Projekti/`.
- Test: fizični telefon (USB debugging) ali emulator s **Google Play** sliko + Instagram.
- **Omejene nastavitve (Android 13+)**: za APK, nameščen iz brskalnika ali upravitelja
  datotek, sistem blokira vklop Accessibility. Postopek: poskusi vklopiti servis →
  zapri opozorilo → Nastavitve → Aplikacije → Odisej → ⋮ → *Allow restricted
  settings / Dovoli omejene nastavitve* → znova vklopi. Android 15 je seznam omejitev
  razširil, ročna izjema na strani aplikacije pa ostaja. Pri namestitvi iz Android
  Studia (adb) se omejitev običajno **ne** pojavi — *preveri* in zapiši v README.
- Po posodobitvi Instagrama: ponovi dump za Reels in DM; če se ID-ji spremenijo,
  popravi samo `DetectionRules` + teste.

## 10. Tveganja

| Tveganje | Verjetnost | Ukrep |
|---|---|---|
| Instagram spremeni ID-je | visoka | pravila na enem mestu, rezervni signal, postopek v docs |
| Zanka "nazaj" | srednja | debounce, test na vseh zaslonih |
| Lažno pozitivno v DM/storyjih | srednja | testi z drevesi DM in story, ki morajo vrniti OTHER |
| Poraba baterije (pogosti dogodki) | nizka–srednja | throttle, iskanje po drevesu s prekinitvijo ob prvem zadetku |
| uiautomator dump ne deluje | visoka | debug dumper v servisu (6.4) |
| Sistem ubije servis (OEM varčevanje) | srednja | navodila v README (izjeme za baterijo) |

## 11. Odločitve

- **D1** Java (konsistentno z Lumnom). Build skripte v Kotlin DSL (samo gradnja).
- **D2** Paket `si.odisej`, minSdk 26, target = privzeti zadnji v Android Studiu.
- **D3** Brez INTERNET, eksplicitno odstranjen prek `tools:node="remove"` + avtomatsko preverjanje.
- **D4** Servis omejen na `com.instagram.android` (XML + preverjanje v kodi).
- **D5** Zaznava samo prek ID-jev/razredov; `getText()` prepovedan.
- **D6** Detektor nad lastnim vmesnikom `UiNode` zaradi enotskih testov.
- **D7** v1 ukrep = GLOBAL_ACTION_BACK z debounce; overlay v v2.
- **D8** Števec v DataStore/SharedPreferences, ne Room.
- **D9** Ni preprečevanja izklopa v sistemu (8.3).
- **D10** Dumpi v `dumps/` (gitignore), testna drevesa v repozitoriju samo očiščena (brez besedil).
- **D11** Ime Odisej (prej delovno 'Reelstop'). Odisej se je dal privezati na jambor,
  da je slišal sirene, a ni mogel do njih (Ulysses contract — vnaprejšnja zaveza
  proti lastni šibkosti). Reelsi = sirene, servis = jambor, strogi način = vrvi.
  Paket `si.odisej`, log tagi `OdisejDump`, `OdisejService`.
- **D12** Servis `exported="false"` + `permission=BIND_ACCESSIBILITY_SERVICE`. Sistemski proces
  (uid system) se lahko veže tudi na neizvoženo komponento, druge aplikacije pa ne. Če se na
  napravi izkaže, da ga sistem ne najde, preklopi na `exported="true"` (dovoljenje še vedno
  omeji vezavo na sistem).
- **D13** Debug dumper v ločenem source setu (`src/debug` / prazen `src/release`), ne za
  `BuildConfig.DEBUG` — release APK kode za izpis drevesa sploh ne vsebuje (Z5).
- **D14** Brez `ProfileInstallReceiver` (androidx), vse izključeno v `data_extraction_rules` (Z6).
- **D15** Signali (Instagram 453.0.0.57.79, Android 16, tabela v `docs/zaznava.md`):
  Reels = **viden** `clips_viewer_view_pager` ali (rezerva) `clips_expanded_touch_view`;
  REELS_TAB = izbran `clips_tab` (zadošča sam, neodvisno od viewerja); reel iz DM =
  Reels + viden `reel_viewer_message_composer`/`reply_bar_container`. Detektor preskoči
  poddrevesa skritih vozlišč, ker Instagram skrit fragment Reels drži v drevesu povsod.
- **D16** Detektor ne bere niti `contentDescription` — ID-ji, vidnost in `isSelected()` zadoščajo
  (strožje od Z3).
- **D17** Testna drevesa so v obliki izpisa dumperja (`.txt`), ne XML: brez pretvorbe, oblika
  nima polja za besedilo, `TreeFilesPrivacyTest` jo preverja z regexom.
- **D18** `flagIncludeNotImportantViews` ostane (Q3): signal DM izjeme je `imp0`.
- **D19** Z zavihka Reels kliknemo `feed_tab` (rezerva: BACK); iz viewerja BACK. BACK z zavihka je
  odvisen od sklada (če IG odpreš naravnost v Reels, bi ga zaprl), klik na Home ni.
- **D20** Trailing throttle 300 ms (dogodki v oknu → ena klasifikacija na koncu okna, nad
  najnovejšim drevesom) + debounce 1500 ms po ukrepu. Logika v čisti Javi (`Debouncer`,
  `BlockPolicy`) zaradi testov.
- **D21** Overlay: `TYPE_ACCESSIBILITY_OVERLAY`, `FLAG_NOT_FOCUSABLE` (sistemski "nazaj" gre
  Instagramu). Ker servis vidi le dogodke Instagrama (Z2), med prikazom overlaya vsako sekundo
  preveri **samo ime paketa** aktivnega okna; če ni Instagram (Home, zaklep), se overlay skrije.
  Prehodni audio focus, da reel za overlayem utihne (preverjeno: Instagram 453 se ustavi).
- **D22** DM izjema izvedljiva strukturno (brez zgodovine zaslonov): reel z vrstico za odgovor =
  REELS_FROM_DM → dovoljen; poteg na naslednjega → REELS_VIEWER → blokiran. Privzeto vklopljena.
- **D23** Privzeti način = overlay (pove, kaj se dogaja, in ne posega v sklad "nazaj").
- **D24** Števec v SharedPreferences: tri števila (dan zadnje blokade, danes, skupaj). DataStore je
  za Kotlin korutine; iz Jave bi rabil še RxJava adapter — dve knjižnici za tri vrednosti.
  Dan zadnje blokade se prepisuje (ni zgodovina), rabimo ga za ponastavitev "danes".
- **D25** Strogi način: premor (15 min) zahteva 60 s odštevanja na zaslonu aplikacije; ob odhodu
  z zaslona se odštevanje prekine. Nadaljevanje blokiranja je takojšnje. Konec premora je v
  stenski uri (preživi ponovni zagon procesa).
- **D26** Release podpis iz `~/.gradle/gradle.properties` (`ODISEJ_STORE_FILE`, `_STORE_PASSWORD`,
  `_KEY_ALIAS`, `_KEY_PASSWORD`); če kaj manjka, nepodpisan APK (CI). Shema v2 (minSdk 26).
- **D27** R8 za vso kodo (ne samo androidx), `-dontobfuscate`: imena ostanejo berljiva, da lahko
  kdorkoli dekompilira APK in ga primerja z izvorno kodo. APK 2,8 MB → 1,4 MB. Keep pravil ne
  rabimo — komponente iz manifesta in XML ohrani AAPT2 (preverjeno v dex).
- **D28** UI "trenutek premora": samo temna tema, ozadje #000000 (OLED), besedilo #EDEDEA,
  sekundarno #A3A3A3 (8,3:1), en poudarek #9CCFC7 (bleda zelenomodra) samo za krog dihanja,
  aktivni status, vklopljeno stikalo, današnji stolpec in ikono. Gumbi: pilula z 1dp obrobo #3A3A3A. Brez kartic, senc in ikon.
- **D29** Pisava Adwaita Sans (fork Intera, OFL) — Inter ni bil nameščen, Adwaita Sans je, brez
  prenosa. Tri statične instance, okrnjene na latinico (164 KB skupaj), ker so variabilne osi
  prek XML na API 26–27 nezanesljive. Priložena, ne downloadable (Z1). Vir: `third_party/`.
- **D30** Overlay: 3 s dihanja (krivulja (1−cos 2πt)/2), gumbi šele po 3 s. Časovnico vodi
  `Handler`, ne konec animatorja — brez animacij (`ANIMATOR_DURATION_SCALE=0`) se animator konča
  takoj, pavza pa mora ostati. Haptika prek `performHapticFeedback` (brez dovoljenja VIBRATE).
  Fade-out se prekine, če se uporabnik vrne v Reelse (ista blokada se ne šteje dvakrat).
- **D31** Ohlajanje 1,5 s po odhodu z overlaya (gumb). Med animacijo prehoda je viewer še ~300 ms
  v drevesu; brez ohlajanja se je overlay pokazal (in štel) še enkrat — overlay 29, glavni
  zaslon 30. Test: `OverlayCountTest`.
- **D32** V overlayu eksplicitno `MaterialButton`: servis nima AppCompat inflaterja, zato `<Button>`
  postane gumb platforme (siv, velike črke) in ne upošteva `materialButtonStyle`.
- **D33** 7-dnevni stolpci: en niz `"dan:število,…"` v SharedPreferences, obrezan ob vsakem zapisu
  na dneve ≥ danes − 6. "Prihodnjih" dni (ura premaknjena nazaj) ne brišemo, da se štetje ne
  izgubi — izginejo, ko se postarajo. Prenos iz stare oblike (dan zadnje blokade + število).
- **D34** Quick Settings ploščica: samo prikaz stanja, tap odpre aplikacijo; premora ne sproži
  (strogi način ostane v aplikaciji). `exported="true"` je nujen — veže se SystemUI, ki ni
  sistemski uid; varuje jo `BIND_QUICK_SETTINGS_TILE`.
- **D35** Ohlajanje ima prednost pred oživljanjem izginjajočega overlaya (`OverlayStep`). Prej je
  servis najprej oživil overlay med 150 ms fade-outom in šele nato preveril ohlajanje — po
  "Zapri Instagram" se je overlay vrnil (dvojni tap). Velja za vse tri izhode.
- **D36** Overlay na 8dp mreži: središče kroga na 40 % višine okna (koda, `translationY` ob vsakem
  layoutu — z utežmi bi bilo le približno), stavek 32dp pod okvirjem kroga (224dp = rob
  razširjenega kroga), akcije pritrjene na dno. Razmiki pri akcijah so vidni (do črtnega okvirja
  16sp besedila, ki je 14,3dp od roba 48dp gumba): 2dp → 16 vidno, 34dp + inset → 48 vidno.
  Primarna akcija je kompaktna pilula (širina = vsebina, 48dp, padding 32dp), ostali dve sta
  ena siva vrstica "Na feed · Zapri Instagram" z 48dp dotikalnimi območji.
- *(D37+)*

## 12. Odprta vprašanja
- Q1: ~~Kateri ID-ji so zanesljivi?~~ → D15 (za Instagram 453.0.0.57.79).
- Q2: ~~Kam vodi "nazaj" iz Reels zavihka?~~ → ostane v Instagramu, na feed (preverjeno za
  pot feed → Reels). Ker je sklad "nazaj" odvisen od poti, z zavihka Reels raje kliknemo
  `feed_tab` (D19).
- Q3: ~~Ali je `flagIncludeNotImportantViews` potreben?~~ → da (D18).
- Q4: Ali `getRootInActiveWindow()` kdaj vrne tuje okno ob Instagram dogodku? — med dumpi ni
  bilo opaženo (izpis zajema le zadnji blok); koda vseeno preverja paket korena.
- Q5: ~~Ali se reel iz DM odpre v istem viewerju?~~ → da, z dodano vrstico za odgovor.
