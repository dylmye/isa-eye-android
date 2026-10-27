#!/usr/bin/env bash
# Points fdroid/me.dylmye.isa.yml at a released tag.
#
# The recipe in this repository is a copy of the one submitted to fdroiddata. F-Droid's autoupdate
# adds new builds to its own copy from the tags, so this only keeps ours from lagging behind: it
# reads the version from app/build.gradle.kts, peels the tag to its commit, and rewrites the build
# entry plus the current-version fields.
#
# Usage: scripts/update-fdroid-metadata.sh <tag>
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

recipe="fdroid/me.dylmye.isa.yml"
build_file="app/build.gradle.kts"
tag="${1:-}"

if [ -z "$tag" ]; then
  printf 'usage: %s <tag>\n' "$(basename "$0")" >&2
  exit 2
fi

if [ ! -f "$recipe" ]; then
  printf 'No %s to update.\n' "$recipe" >&2
  exit 1
fi

version_name="$(sed -n 's/^[[:space:]]*versionName = "\(.*\)"$/\1/p' "$build_file" | head -n 1)"
version_code="$(sed -n 's/^[[:space:]]*versionCode = \([0-9][0-9]*\)$/\1/p' "$build_file" | head -n 1)"
if [ -z "$version_name" ] || [ -z "$version_code" ]; then
  printf 'Could not read versionName and versionCode from %s.\n' "$build_file" >&2
  exit 1
fi

# A full commit hash, not the tag name, as F-Droid's metadata reference asks. Peeling matters:
# `git rev-parse <tag>` on an annotated tag returns the tag object, not the commit.
commit="$(git rev-parse --verify --quiet "$tag^{commit}")" || {
  printf 'No such tag: %s\n' "$tag" >&2
  exit 1
}

# This copy describes one release. Refuse to guess if that ever stops being true.
entries="$(grep -c '^  - versionName:' "$recipe" || true)"
if [ "$entries" -ne 1 ]; then
  printf '%s has %s build entries; expected exactly one.\n' "$recipe" "$entries" >&2
  exit 1
fi

sed \
  -e "s|^  - versionName: .*|  - versionName: $version_name|" \
  -e "s|^    versionCode: .*|    versionCode: $version_code|" \
  -e "s|^    commit: .*|    commit: $commit|" \
  -e "s|^CurrentVersion: .*|CurrentVersion: $version_name|" \
  -e "s|^CurrentVersionCode: .*|CurrentVersionCode: $version_code|" \
  "$recipe" > "$recipe.tmp"
mv "$recipe.tmp" "$recipe"

# Fail rather than leave a half-written recipe behind.
for expected in \
  "  - versionName: $version_name" \
  "    versionCode: $version_code" \
  "    commit: $commit" \
  "CurrentVersion: $version_name" \
  "CurrentVersionCode: $version_code"
do
  grep -qxF "$expected" "$recipe" || {
    printf 'Failed to write "%s" into %s.\n' "$expected" "$recipe" >&2
    exit 1
  }
done

printf '%s now describes %s (%s) at %s.\n' "$recipe" "$version_name" "$version_code" "$commit"
