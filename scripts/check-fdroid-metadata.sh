#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
META="$ROOT/fastlane/metadata/android/en-US"
GRADLE="$ROOT/app/build.gradle.kts"

fail() { echo "fdroid-metadata: $*" >&2; exit 1; }

[[ -f "$META/short_description.txt" ]] || fail "missing short_description.txt"
[[ -f "$META/full_description.txt" ]] || fail "missing full_description.txt"
[[ -f "$META/title.txt" ]] || fail "missing title.txt"
[[ -f "$META/images/icon.png" ]] || fail "missing images/icon.png"
[[ -d "$META/images/phoneScreenshots" ]] || fail "missing phoneScreenshots/"

short_len="$(tr -d '\n' < "$META/short_description.txt" | wc -c)"
if (( short_len > 80 )); then
  fail "short_description.txt is ${short_len} chars (max 80)"
fi
if grep -q '\.$' "$META/short_description.txt"; then
  fail "short_description.txt must not end with a period"
fi

title_len="$(tr -d '\n' < "$META/title.txt" | wc -c)"
if (( title_len > 50 )); then
  fail "title.txt is ${title_len} chars (max 50)"
fi

full_len="$(wc -c < "$META/full_description.txt")"
if (( full_len > 4000 )); then
  fail "full_description.txt is ${full_len} chars (max 4000)"
fi

mapfile -t shots < <(find "$META/images/phoneScreenshots" -type f \( -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' \) | sort)
(( ${#shots[@]} >= 1 )) || fail "need at least one phone screenshot"

version_code="$(sed -n 's/.*versionCode = \([0-9][0-9]*\).*/\1/p' "$GRADLE" | head -n1)"
[[ -n "$version_code" ]] || fail "could not read versionCode from app/build.gradle.kts"
changelog="$META/changelogs/${version_code}.txt"
[[ -f "$changelog" ]] || fail "missing changelog for versionCode ${version_code}"
clog_len="$(wc -c < "$changelog")"
if (( clog_len > 500 )); then
  fail "${changelog} is ${clog_len} chars (max 500)"
fi

echo "F-Droid Fastlane metadata OK (versionCode=${version_code}, screenshots=${#shots[@]})"
