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

Zapiši:
| Signal (ID) | Reels tab | Viewer (feed) | Viewer (DM) | DM | Story | Feed | Opomba |
|---|---|---|---|---|---|---|---|
| | | | | | | | |

Instagram verzija: `…` · Datum: `…` · Naprava / Android: `…`

## 4. Testna drevesa
Za enotske teste iz dumpa naredi **očiščeno** drevo: samo `resource-id`, `class` in
gnezdenje. Brez `text`, brez `content-desc` (razen navigacijskih). Shrani v
`app/src/test/resources/trees/<ime>.xml`. Preden commitaš, preglej datoteko z očmi.
