#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION=8.13
SHA=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78
command -v java >/dev/null || { echo "Install JDK 17+ first." >&2; exit 1; }
command -v curl >/dev/null || { echo "curl is required." >&2; exit 1; }
command -v unzip >/dev/null || { echo "unzip is required." >&2; exit 1; }
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/aerodrive-bootstrap"
mkdir -p "$CACHE"
ZIP="$CACHE/gradle-$VERSION-bin.zip"
if [[ ! -f "$ZIP" ]]; then
    curl --fail --location --retry 3 "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ZIP.part"
    mv "$ZIP.part" "$ZIP"
fi
if command -v sha256sum >/dev/null; then
    ACTUAL="$(sha256sum "$ZIP" | awk '{print $1}')"
else
    ACTUAL="$(shasum -a 256 "$ZIP" | awk '{print $1}')"
fi
[[ "$ACTUAL" == "$SHA" ]] || { echo "Gradle checksum mismatch. Delete $ZIP and retry." >&2; exit 1; }
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
unzip -q "$ZIP" -d "$WORK"
mkdir -p "$WORK/wrapper-project"
printf 'rootProject.name = "wrapper-bootstrap"
' > "$WORK/wrapper-project/settings.gradle.kts"
"$WORK/gradle-$VERSION/bin/gradle" --no-daemon -p "$WORK/wrapper-project" wrapper     --gradle-version "$VERSION" --distribution-type bin --gradle-distribution-sha256-sum "$SHA"
cp "$WORK/wrapper-project/gradlew" "$ROOT/gradlew"
cp "$WORK/wrapper-project/gradlew.bat" "$ROOT/gradlew.bat"
mkdir -p "$ROOT/gradle/wrapper"
cp "$WORK/wrapper-project/gradle/wrapper/"* "$ROOT/gradle/wrapper/"
chmod +x "$ROOT/gradlew"
echo "Gradle wrapper installed. Open $ROOT in Android Studio."
