# Zaznava Reelsov — postopek

Cilj: najti resource ID-je, ki so **vedno** prisotni v Reelsih in **nikoli** v DM-jih,
storyjih ali feedu. Tega ne ugibamo — izmerimo na svoji verziji Instagrama.

## 0. Priprava
```bash
adb devices                      # telefon/emulator mora biti "device"
adb shell dumpsys package com.instagram.android | grep versionName
```
Verzijo Instagrama zapiši v tabelo spodaj — ID-ji veljajo za to verzijo.

## 1. Dump vsakega zaslona

**Priporočeno: `scripts/collect-dumps.sh`** (debug build, servis vklopljen). Skripta te vodi
po zaslonih, za vsakega pošlje servisu `am broadcast si.odisej.debug.DUMP` in shrani izpis
dumperja v `dumps/<ime>.tree.txt` — samo ID-ji, razredi, meje in zastavice (`sel` izbran,
`inv` neviden, `imp0` "not important", `clk` klikljiv). Besedil ni, `uiautomator` se ne
uporablja, zato ne nastane datoteka z DM-ji. Na koncu sama testira Q2 (BACK iz Reels zavihka).

Ročna alternativa z `uiautomator` (ustvari zasebni XML z besedili):
Odpri zaslon v Instagramu in poženi (ime brez presledkov):
```bash
scripts/dump-ui.sh reels-tab
scripts/dump-ui.sh reels-viewer-iz-feeda
scripts/dump-ui.sh reels-viewer-iz-explore
scripts/dump-ui.sh reel-iz-dm
scripts/dump-ui.sh dm-seznam
scripts/dump-ui.sh dm-pogovor
scripts/dump-ui.sh story
scripts/dump-ui.sh feed
scripts/dump-ui.sh profil
scripts/dump-ui.sh nova-objava
```
Skripta shrani `dumps/<ime>.xml` (cel dump, **zaseben**) in `dumps/<ime>.ids.txt`
(samo unikatni ID-ji — s tem delaš).

Če dobiš `ERROR: could not get idle state`: tapni video, da se ustavi, in poskusi
znova. Če še vedno ne gre, uporabi debug dumper iz servisa (CONTEXT.md §6.4):
```bash
adb logcat -c && adb logcat -s OdisejDump > dumps/reels-tab.log
```

## 2. Primerjava
```bash
# ID-ji, ki so v Reelsih, a ne v DM pogovoru:
comm -23 dumps/reels-viewer-iz-feeda.ids.txt dumps/dm-pogovor.ids.txt

# Kandidati, ki vsebujejo "clips" (hipoteza):
grep -i clips dumps/*.ids.txt

# Ali je kandidat prisoten v VSEH reels dumpih in v NOBENEM drugem:
grep -l 'clips_viewer' dumps/*.ids.txt      # primer — zamenjaj z dejanskim ID-jem
```

## 3. Izbira signalov
Dober signal je:
- prisoten v vseh Reels zaslonih (zavihek + viewer),
- odsoten v DM, story, feed, profil, nova objava,
- strukturen (vsebnik, view pager), ne dekorativen (ikona, ki jo lahko odstranijo).

### Rezultat (2026-10-31)

Instagram `453.0.0.57.79` · Android 16 (API 36), Motorola moto g86 power 5G · jezik sl-SI.
Vir: `scripts/collect-dumps.sh`, 15 zaslonov. ✓ = viden, (s) = v drevesu, a skrit (`inv`), – = ga ni.

| Signal (ID, `com.instagram.android:id/…`) | Reels tab | Viewer feed | Viewer Explore | Reel iz DM | Naslednji v DM | Feed | Reel v feedu | Story | DM seznam | DM pogovor | Explore | Profil | Reels mreža | Nova objava |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `clips_viewer_view_pager` | ✓ | ✓ | ✓ | ✓ | ✓ | (s) | (s) | (s) | (s) | – | (s) | (s) | (s) | – |
| `clips_expanded_touch_view` | ✓ | ✓ | ✓ | ✓ | ✓ | – | – | – | – | – | – | – | – | – |
| `clips_tab` izbran (`sel`) | ✓ | – | – | – | – | – | – | – | – | – | – | – | – | – |
| `reel_viewer_message_composer` / `reply_bar_container` | – | – | – | ✓ | – | – | – | – | – | – | – | – | – | – |

Ugotovitve:
- **Vidnost je ključna.** Instagram drži fragment Reels zavihka (`root_clips_layout` →
  `clips_viewer_view_pager`) v drevesu skoraj vseh zaslonov, skrit z ničelno ali negativno
  širino. Brez preverjanja `isVisibleToUser()` bi blokirali feed, DM-je in profil.
- Viewer iz feeda/Explore se odpre **nad** skritim zavihkom: v drevesu sta dva pagerja,
  eden viden. Detektor zato poddrevesa skritih vozlišč preskoči.
- Reel iz DM se odpre v **istem** viewerju (Q5), le z vrstico za odgovor pošiljatelju. Po
  potegu na naslednji (predlagan) reel vrstica izgine (pojavi se `suggested_title`).
- `tab_bar` in signali DM (`reply_bar_container`) so `imp0` → `flagIncludeNotImportantViews` je
  potreben (Q3). Pager in zavihki so "important".
- V tej verziji so DM-ji zavihek v spodnji vrstici (`direct_tab`), Home je `feed_tab`.
- Q2: BACK iz zavihka Reels (prišel iz feeda) ostane v Instagramu in pristane na feedu.
- Compose elementi imajo včasih ID brez paketa (`ig_text`, `search_row`) — to so `testTag`-i.

## 4. Testna drevesa
Izpis `collect-dumps.sh` je že v obliki testnega drevesa (samo ID, razred, meje, zastavice).
Odstrani vrstici `BEGIN`/`END` in shrani v `app/src/test/resources/trees/<ime>.txt`, nato dodaj
pričakovano stanje v `ReelsDetectorTreesTest`. `TreeFilesPrivacyTest` pade, če katerakoli
vrstica ni v strogi obliki (npr. če se vanjo pritihotapi besedilo). Preden commitaš, preglej
datoteko z očmi.
