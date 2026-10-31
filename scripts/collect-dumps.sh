#!/usr/bin/env bash
# F1: vodi te po zaslonih Instagrama in za vsakega shrani izpis debug dumperja
# (SAMO ID-ji, razredi, meje, zastavice — brez besedil) v dumps/<ime>.tree.txt.
#
# Ne uporablja uiautomator dump, zato ne nastane nobena datoteka z besedili (DM-ji).
# Potrebuje: povezan telefon, nameščen DEBUG build, vklopljen servis Odisej.
#
# Uporaba: scripts/collect-dumps.sh            (vsi zasloni)
#          scripts/collect-dumps.sh feed story  (samo izbrani)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/dumps"
SERVICE="si.odisej/si.odisej.service.ReelsAccessibilityService"
mkdir -p "$OUT"

# ime|navodilo
SCREENS=(
  "feed|Zavihek Home (hišica), feed z objavami. Počakaj, da se naloži."
  "reel-v-feedu|V feedu se pomakni do reela, ki se predvaja v feedu (NE tapni nanj)."
  "story|Odpri nek story (krogec zgoraj). Če hitro mine, ga drži s prstom."
  "dm-seznam|Seznam sporočil (ikona DM / papirnato letalo)."
  "dm-pogovor|Odpri en pogovor."
  "reel-iz-dm|V pogovoru tapni reel, ki ti ga je nekdo poslal (odpre se cel zaslon)."
  "reel-iz-dm-naslednji|V tem istem pogledu podrsaj navzgor na naslednji reel (če gre; sicer 's')."
  "explore|Zavihek Iskanje / Explore (mreža slik in videov)."
  "reels-viewer-iz-explore|V Explore tapni video (reel), da se odpre čez cel zaslon."
  "reels-viewer-iz-feeda|Nazaj na Home; v feedu tapni reel, da se odpre čez cel zaslon."
  "profil|Tvoj profil (zavihek desno spodaj)."
  "profil-reels-mreza|Na profilu odpri mrežo reelov (ikona Reels pod biografijo), NE odpiraj reela."
  "nova-objava|Ustvari novo objavo (+). Ostani na prvem zaslonu izbire."
  "reels-tab|Zavihek Reels v spodnji vrstici. Naj se reel predvaja."
)

die() { echo "NAPAKA: $*" >&2; exit 1; }
pause() { read -r -p "$1" REPLY </dev/tty; }

adb get-state >/dev/null 2>&1 || die "ni povezane naprave (adb devices)."

if ! adb shell pm list packages si.odisej | grep -q '^package:si.odisej$'; then
  echo "Odisej ni nameščen — nameščam debug build ..."
  (cd "$ROOT" && ./gradlew installDebug -q)
fi

INFO="$OUT/device.txt"
{
  echo "datum: $(date -I)"
  echo "naprava: $(adb shell getprop ro.product.manufacturer | tr -d '\r') $(adb shell getprop ro.product.model | tr -d '\r')"
  echo "android: $(adb shell getprop ro.build.version.release | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"
  echo "instagram: $(adb shell dumpsys package com.instagram.android | grep -m1 versionName | tr -d ' \r')"
  echo "jezik: $(adb shell getprop persist.sys.locale | tr -d '\r')"
} > "$INFO"
cat "$INFO"

until adb shell settings get secure enabled_accessibility_services | grep -q "$SERVICE"; do
  echo
  echo "Servis Odisej ni vklopljen. Odpiram nastavitve dostopnosti na telefonu ..."
  adb shell am start -a android.settings.ACCESSIBILITY_SETTINGS >/dev/null
  echo "Vklopi: Nameščene aplikacije / Downloaded apps → Odisej → Vklopi."
  echo "Če je stikalo sivo/blokirano: glej navodila v aplikaciji Odisej (Allow restricted settings)."
  echo "Ali se Odisej sploh pojavi na seznamu? To zapiši (preverjamo D12)."
  pause "Ko je vklopljen, pritisni Enter ... "
done
echo "Servis je vklopljen."
adb shell dumpsys accessibility | grep -q "si.odisej" \
  && echo "dumpsys accessibility: servis je povezan." \
  || echo "OPOZORILO: dumpsys accessibility servisa ne omenja."

# Zajame zadnji celoten blok BEGIN..END iz OdisejDump.
capture() {
  local name="$1"
  adb logcat -c
  adb shell am broadcast -a si.odisej.debug.DUMP -p si.odisej >/dev/null
  sleep 2
  adb logcat -d -v raw -s OdisejDump:D \
    | awk '/^BEGIN #/{buf=""; on=1} on{buf=buf $0 "\n"} /^END #/{last=buf; on=0} END{printf "%s", last}' \
    > "$OUT/$name.tree.txt"
  if [[ ! -s "$OUT/$name.tree.txt" ]]; then
    echo "  ni izpisa. Zadnje vrstice dumperja:"
    adb logcat -d -v raw -s OdisejDump:D | tail -3 | sed 's/^/    /'
    return 1
  fi
  echo "  shranjeno dumps/$name.tree.txt ($(tail -1 "$OUT/$name.tree.txt"))"
}

want() {
  [[ ${#SELECTED[@]} -eq 0 ]] && return 0
  local s; for s in "${SELECTED[@]}"; do [[ "$s" == "$1" ]] && return 0; done; return 1
}
SELECTED=("$@")

echo
echo "Za vsak zaslon: odpri ga v Instagramu, nato tu pritisni Enter."
echo "  s + Enter = preskoči,  r + Enter = ponovi zadnjega."
for entry in "${SCREENS[@]}"; do
  name="${entry%%|*}"; hint="${entry#*|}"
  want "$name" || continue
  while true; do
    echo
    echo "[$name] $hint"
    pause "  Enter = zajemi, s = preskoči: "
    [[ "$REPLY" == "s" ]] && { echo "  preskočeno"; break; }
    if capture "$name"; then
      pause "  OK? Enter = naprej, r = ponovi: "
      [[ "$REPLY" == "r" ]] || break
    fi
  done
done

if want "q2-back"; then
  echo
  echo "[q2-back] Test Q2: kam pelje 'nazaj' iz zavihka Reels."
  pause "  Odpri zavihek Reels in pritisni Enter (s = preskoči): "
  if [[ "$REPLY" != "s" ]]; then
    adb shell input keyevent KEYCODE_BACK
    sleep 2
    {
      echo "fokus po BACK:"
      adb shell dumpsys window | grep -m1 -E 'mCurrentFocus' | sed 's/^ *//'
    } > "$OUT/q2-back.txt"
    cat "$OUT/q2-back.txt"
    capture "q2-po-back" || true
  fi
fi

echo
echo "Končano. Datoteke so v dumps/ (gitignore). Sporoči Claudu, da je zbiranje končano."
