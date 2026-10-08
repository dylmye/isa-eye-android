#!/usr/bin/env bash
#
# Regenerates the launcher icons from the source foreground image in art/launcher.
#
# Requires GraphicsMagick ("gm"). Run from anywhere:
#   scripts/generate-launcher-icons.sh
#
# Produces, for each density:
#   drawable-<density>/ic_launcher_foreground.png  (adaptive foreground, 108dp)
#   mipmap-<density>/ic_launcher.png               (legacy, square, 48dp)
#   mipmap-<density>/ic_launcher_round.png         (legacy, circular, 48dp)
# plus art/play-store-icon-512.png for the Play Console listing.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$ROOT/art/launcher/ic_launcher_foreground.png"
RES="$ROOT/app/src/main/res"
BG="#0f1f19"

if ! command -v gm >/dev/null 2>&1; then
  echo "GraphicsMagick ('gm') is required but was not found on PATH." >&2
  exit 1
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# density:legacy-size:foreground-size  (legacy = 48dp, foreground = 108dp, at each density)
for spec in "mdpi:48:108" "hdpi:72:162" "xhdpi:96:216" "xxhdpi:144:324" "xxxhdpi:192:432"; do
  IFS=: read -r density legacy fg <<<"$spec"
  mkdir -p "$RES/drawable-$density" "$RES/mipmap-$density"

  # Adaptive foreground.
  gm convert "$SRC" -resize "${fg}x${fg}" "$RES/drawable-$density/ic_launcher_foreground.png"

  # Scale the foreground to the legacy icon size and composite it over the background.
  gm convert "$SRC" -resize "${legacy}x${legacy}" "$TMP/fg-$density.png"
  gm convert -size "${legacy}x${legacy}" "xc:$BG" "$TMP/bg-$density.png"
  gm composite -geometry +0+0 "$TMP/fg-$density.png" "$TMP/bg-$density.png" \
    "$TMP/square-$density.png"

  # Legacy square: rounded corners (pre-API-26 launchers don't mask the icon themselves).
  corner=$((legacy / 5))
  gm convert -size "${legacy}x${legacy}" xc:none -fill white \
    -draw "roundrectangle 0,0 $((legacy - 1)),$((legacy - 1)) $corner,$corner" \
    "$TMP/roundrect-$density.png"
  gm composite -compose In "$TMP/square-$density.png" "$TMP/roundrect-$density.png" \
    "$RES/mipmap-$density/ic_launcher.png"

  # Legacy round: circular mask.
  radius=$((legacy / 2))
  gm convert -size "${legacy}x${legacy}" xc:none -fill white \
    -draw "circle $radius,$radius $radius,0" "$TMP/mask-$density.png"
  gm composite -compose In "$TMP/square-$density.png" "$TMP/mask-$density.png" \
    "$RES/mipmap-$density/ic_launcher_round.png"
done

# Play Console listing icon (not part of the APK).
gm convert "$SRC" -resize 512x512 "$TMP/fg-512.png"
gm convert -size 512x512 "xc:$BG" "$TMP/bg-512.png"
gm composite -geometry +0+0 "$TMP/fg-512.png" "$TMP/bg-512.png" "$ROOT/art/play-store-icon-512.png"

echo "Launcher icons generated from $SRC"
