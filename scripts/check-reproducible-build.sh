#!/usr/bin/env bash
# Checks that the release APK is reproducible.
#
# F-Droid verifies a release by rebuilding the tagged commit and comparing the result against the
# developer's APK, so this builds the same commit twice under deliberately different conditions and
# fails if the two are not byte-identical:
#
#   - separate clones in directories of different lengths, because a build path that leaks into the
#     output would show up here
#   - a different timezone and locale, which catch timestamps and collation-order dependence
#   - no keystore.properties, which leaves the APK unsigned: signature bytes are not reproducible in
#     the way F-Droid compares, and the unsigned APK is the artifact it rebuilds against
#
# Requires OpenJDK 17 and an Android SDK, located the same way Gradle finds it (local.properties, or
# ANDROID_HOME/ANDROID_SDK_ROOT).
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
work="$(mktemp -d)"
kept=0

cleanup() {
  if [ "$kept" -eq 0 ]; then
    rm -rf "$work"
  else
    printf 'Builds kept for comparison in %s\n' "$work" >&2
  fi
}
trap cleanup EXIT

build() {
  local dir="$1" tz="$2" locale="$3"

  git clone --quiet --local "$root" "$dir"
  # The clones are clean checkouts, so local.properties is absent. Pass on a local SDK location
  # when there is one; otherwise Gradle falls back to ANDROID_HOME, as it does on CI.
  if [ -f "$root/local.properties" ]; then
    cp "$root/local.properties" "$dir/local.properties"
  fi

  printf 'Building in %s (TZ=%s, LANG=%s)\n' "$dir" "$tz" "$locale" >&2
  (
    cd "$dir"
    TZ="$tz" LANG="$locale" LC_ALL="$locale" ./gradlew :app:assembleRelease --no-daemon --quiet
  )
  printf '%s\n' "$dir/app/build/outputs/apk/release/app-release-unsigned.apk"
}

first="$(build "$work/a" UTC C.UTF-8)"
second="$(build "$work/reproducible-build-check" Pacific/Auckland en_GB.UTF-8)"

if cmp -s "$first" "$second"; then
  printf 'Reproducible: %s\n' "$(sha256sum "$first" | cut -d' ' -f1)"
  exit 0
fi

kept=1
printf 'Not reproducible: the two builds differ.\n' >&2
sha256sum "$first" "$second" >&2
printf 'Run "diffoscope %s %s" to see where they diverge.\n' "$first" "$second" >&2
exit 1
