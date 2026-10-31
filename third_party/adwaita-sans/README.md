# Adwaita Sans (pisava aplikacije)

- Vir: Fedora paket `adwaita-sans-fonts` 50.0 (`/usr/share/fonts/adwaita-sans-fonts/AdwaitaSans-Regular.ttf`),
  upstream https://gitlab.gnome.org/GNOME/adwaita-fonts — fork pisave Inter (https://github.com/rsms/inter).
- Licenca: SIL Open Font License 1.1 (`OFL.txt`), brez rezerviranega imena pisave.
- Spremembe (fonttools 4.68): iz variabilne pisave statične instance in okrnitev na latinico
  (U+0020–017F, ločila, puščice, €):
  - `app/src/main/res/font/odisej_light.ttf`   — wght 300, opsz 32 (velike številke, naslovi)
  - `app/src/main/res/font/odisej_regular.ttf` — wght 400, opsz 14 (besedilo)
  - `app/src/main/res/font/odisej_medium.ttf`  — wght 500, opsz 14 (gumbi)
- Tabela `name` (avtorske pravice, licenca) je ohranjena v vsaki datoteki.

Ponovitev:
```bash
fonttools varLib.instancer AdwaitaSans-Regular.ttf wght=300 opsz=32 -o light.ttf
pyftsubset light.ttf --unicodes="U+0020-007E,U+00A0-00FF,U+0100-017F,U+2010-2027,U+2030-203A,U+2190-2193,U+20AC,U+2212" \
  --layout-features="kern,liga,calt,tnum,ccmp,locl,mark,mkmk" --name-IDs='*' --name-languages='*' \
  --output-file=odisej_light.ttf
```
