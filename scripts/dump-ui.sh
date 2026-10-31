#!/usr/bin/env bash
# Posname UI drevo trenutnega zaslona (uiautomator dump) v dumps/<ime>.xml
# in izpiše unikatne resource ID-je v dumps/<ime>.ids.txt.
#
# POZOR: <ime>.xml vsebuje besedila z zaslona (DM-ji, imena). Ostane v dumps/,
# ki je v .gitignore. Nikoli ga ne commitaj.
#
# Uporaba: scripts/dump-ui.sh <ime-zaslona> [--compressed]
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Uporaba: $0 <ime-zaslona> [--compressed]" >&2
  exit 1
fi

NAME="$1"
EXTRA="${2:-}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT/dumps"
REMOTE="/sdcard/odisej_dump.xml"
mkdir -p "$OUT_DIR"

if ! adb get-state >/dev/null 2>&1; then
  echo "Ni povezane naprave (adb devices)." >&2
  exit 1
fi

# uiautomator izpiše napako na stdout in vrne 0, zato preverimo besedilo.
RESULT="$(adb shell uiautomator dump $EXTRA "$REMOTE" 2>&1 || true)"
if grep -q "ERROR" <<<"$RESULT"; then
  echo "$RESULT" >&2
  echo "Namig: ustavi video (tap) in poskusi znova, ali uporabi --compressed," >&2
  echo "ali debug dumper v servisu: adb logcat -s OdisejDump" >&2
  exit 2
fi

adb pull "$REMOTE" "$OUT_DIR/$NAME.xml" >/dev/null
adb shell rm -f "$REMOTE"

grep -o 'resource-id="[^"]*"' "$OUT_DIR/$NAME.xml" \
  | sed 's/resource-id="//; s/"$//' \
  | grep -v '^$' \
  | sort -u > "$OUT_DIR/$NAME.ids.txt"

PKG="$(grep -o 'package="[^"]*"' "$OUT_DIR/$NAME.xml" | sort | uniq -c | sort -rn | head -1 || true)"
COUNT="$(wc -l < "$OUT_DIR/$NAME.ids.txt")"

echo "Shranjeno: dumps/$NAME.xml (zasebno) in dumps/$NAME.ids.txt ($COUNT ID-jev)"
echo "Prevladujoči paket: $PKG"
echo "Kandidati z 'clips':"
grep -i clips "$OUT_DIR/$NAME.ids.txt" || echo "  (ni)"
