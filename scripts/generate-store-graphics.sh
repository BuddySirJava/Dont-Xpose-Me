#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/scripts/store-graphics"
DEST="$ROOT/fastlane/metadata/android/en-US/images"

magick "$SRC/icon.svg" PNG32:"$DEST/icon.png"
magick "$SRC/featureGraphic.svg" PNG32:"$DEST/featureGraphic.png"
magick "$SRC/phoneScreenshots/1.svg" PNG32:"$DEST/phoneScreenshots/1.png"
magick "$SRC/phoneScreenshots/2.svg" PNG32:"$DEST/phoneScreenshots/2.png"
for f in "$DEST/icon.png" "$DEST/featureGraphic.png" "$DEST/phoneScreenshots/"*.png; do
  magick "$f" -strip "$f"
done
echo "Wrote Fastlane images under $DEST"
